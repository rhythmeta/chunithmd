#!/usr/bin/env python3
"""Prepare Android PP-OCRv6 small assets (requires PyYAML and onnx).

Downloads the pinned official ONNX model without converting its weights. iOS uses
Vision and has no PaddleOCR assets. --check needs only the Python standard library.
"""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "model-assets/android"
MANIFEST = ROOT / "scripts/paddleocr-v6.json"
SOURCE = "https://huggingface.co/PaddlePaddle/PP-OCRv6_small_rec_onnx"
REVISION = "b8f84f0b80c529de40b4fbb3544b84fa7233a513"
SOURCES = {
    "inference.onnx": "5435fd747c9e0efe15a96d0b378d5bd157e9492ed8fd80edf08f30d02fa24634",
    "inference.yml": "ab078671bb49f06228eadccd34f1bb501e157f7a047095ffb943ba81512c77d1",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def check_vocabulary(vocabulary):
    assert len(vocabulary) == 18710 and vocabulary[0] == "blank" and vocabulary[-1] == " "
    required = [chr(i) for i in range(32, 127)] + list("中文国國日あいうえおアイウエオ時狂止")
    assert all(token in vocabulary for token in required), "Missing required ASCII/Chinese/Japanese tokens"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--cache", type=Path, default=ROOT / "artifacts/paddleocr-v6")
    args = parser.parse_args()
    if args.check:
        manifest = json.loads(MANIFEST.read_text())
        for path, expected in manifest["assets_sha256"].items():
            assert digest((ROOT / path).read_bytes()) == expected, f"Changed asset: {path}"
        check_vocabulary(json.loads((ASSETS / "PaddleOCRv6SmallVocab.json").read_text()))
        print("Android PaddleOCR model, vocabulary and license verified.")
        return

    import onnx
    import yaml

    args.cache.mkdir(parents=True, exist_ok=True)
    for name, expected in SOURCES.items():
        path = args.cache / name
        if not path.exists():
            with urllib.request.urlopen(f"{SOURCE}/resolve/{REVISION}/{name}", timeout=120) as response:
                data = response.read()
            assert digest(data) == expected, f"Unexpected download: {name}"
            path.write_bytes(data)
        assert digest(path.read_bytes()) == expected, f"Unexpected cached file: {name}"
    graph = onnx.load(str(args.cache / "inference.onnx"))
    onnx.checker.check_model(graph)
    shape = lambda value: [dimension.dim_value for dimension in value.type.tensor_type.shape.dim]
    assert len(graph.graph.input) == 1 and shape(graph.graph.input[0])[1:3] == [3, 48]
    assert len(graph.graph.output) == 1 and len(shape(graph.graph.output[0])) == 3
    assert shape(graph.graph.output[0])[-1] == 18710
    config = yaml.safe_load((args.cache / "inference.yml").read_text())
    vocabulary = ["blank"] + config["PostProcess"]["character_dict"] + [" "]
    check_vocabulary(vocabulary)
    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / "PaddleOCRv6Small.onnx").write_bytes((args.cache / "inference.onnx").read_bytes())
    (ASSETS / "PaddleOCRv6SmallVocab.json").write_text(json.dumps(vocabulary, ensure_ascii=False) + "\n", encoding="utf-8")
    names = ["PaddleOCRv6Small.onnx", "PaddleOCRv6SmallVocab.json", "PaddleOCR-LICENSE.txt", "PaddleOCR-NOTICE.txt"]
    manifest = {
        "source": SOURCE, "revision": REVISION, "license": "Apache-2.0",
        "source_sha256": SOURCES,
        "input": "float32 NCHW BGR, height 48, pixel / 127.5 - 1; normalized zero right padding",
        "width_buckets": [320, 640, 1280, 2560],
        "output": "[1,T,18710] probabilities; CTC blank 0, collapse repeats then remove blanks",
        "assets_sha256": {str((ASSETS / name).relative_to(ROOT)): digest((ASSETS / name).read_bytes()) for name in names},
    }
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    print("Prepared pinned Android PP-OCRv6 small model and Unicode vocabulary.")


if __name__ == "__main__":
    main()
