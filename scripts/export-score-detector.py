#!/usr/bin/env python3
"""Export the six-field detector to the exact tensor contract consumed by both apps.

Run on macOS with ultralytics==8.4.19, torch==2.8.0, onnx==1.19.1,
coremltools==9.0. Python dependencies are only needed for export, not --check.
"""
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import shutil
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ONNX = ROOT / "model-assets/android/ScoreDetector.onnx"
COREML = ROOT / "model-assets/ios/ScoreDetector.mlpackage"
MANIFEST = ROOT / "scripts/score-detector.json"
FIELDS = ["title", "difficulty", "level", "score", "clear", "combo"]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def asset_hashes():
    paths = [ONNX] + sorted(p for p in COREML.rglob("*") if p.is_file())
    return {str(p.relative_to(ROOT)): digest(p) for p in paths}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("weights", nargs="?", type=Path, default=ROOT / "model-assets/exp.pt")
    parser.add_argument("--check", action="store_true", help="Verify published assets against their recorded hashes")
    args = parser.parse_args()
    if args.check:
        manifest = json.loads(MANIFEST.read_text())
        if manifest["assets_sha256"] != asset_hashes():
            raise SystemExit("Model assets do not match score-detector.json; re-export both platforms together.")
        print("Published ONNX and Core ML assets verified.")
        return

    import coremltools as ct
    import onnx
    from ultralytics import YOLO

    # Keep intermediates beside a temporary checkpoint, never overwrite the user's weights.
    with tempfile.TemporaryDirectory(prefix="chunithmd-export-") as directory:
        checkpoint = Path(directory) / "ScoreDetector.pt"
        shutil.copy2(args.weights, checkpoint)
        detector = YOLO(str(checkpoint))
        if [detector.names[i] for i in range(6)] != FIELDS or len(detector.names) != 6:
            raise SystemExit("Expected classes in order: " + ", ".join(FIELDS))
        common = dict(imgsz=1024, batch=1, nms=False, end2end=False, device="cpu")
        onnx_path = Path(detector.export(format="onnx", dynamic=False, simplify=False, opset=17, **common))
        coreml_path = Path(YOLO(str(checkpoint)).export(format="coreml", half=True, **common))

        graph = onnx.load(str(onnx_path))
        onnx.checker.check_model(graph)
        shape = lambda item: [d.dim_value for d in item.type.tensor_type.shape.dim]
        if len(graph.graph.input) != 1 or shape(graph.graph.input[0]) != [1, 3, 1024, 1024]:
            raise SystemExit("Unsupported ONNX input contract")
        if len(graph.graph.output) != 1 or shape(graph.graph.output[0]) != [1, 10, 21504]:
            raise SystemExit("Unsupported ONNX output contract")
        spec = ct.models.MLModel(str(coreml_path), skip_model_load=True).get_spec()
        image = spec.description.input[0]
        if image.name != "image" or image.type.imageType.width != 1024 or image.type.imageType.height != 1024:
            raise SystemExit("Unsupported Core ML input contract")
        if len(spec.description.output) != 1 or list(spec.description.output[0].type.multiArrayType.shape) != [1, 10, 21504]:
            raise SystemExit("Unsupported Core ML output contract")

        # Only install exports after both contracts validate.
        ONNX.parent.mkdir(parents=True, exist_ok=True)
        COREML.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(onnx_path, ONNX)
        if COREML.exists():
            shutil.rmtree(COREML)
        shutil.copytree(coreml_path, COREML)
        manifest = {
            "source": args.weights.name,
            "source_sha256": digest(args.weights),
            "export_versions": {p: importlib.metadata.version(p) for p in ("ultralytics", "torch", "onnx", "coremltools")},
            "input_size": 1024,
            "classes": FIELDS,
            "output_shape": [1, 10, 21504],
            "layout": "NCHW RGB 0..1; raw cx,cy,w,h pixels + six class probabilities; no objectness or NMS",
            "letterbox": "round resized dimensions; centered integer padding; RGB 114",
            "assets_sha256": asset_hashes(),
        }
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
        print("Exported and validated Android ONNX / iOS Core ML models.")


if __name__ == "__main__":
    main()
