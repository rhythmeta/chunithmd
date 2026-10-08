# Scanner model sources

These files are published separately and are not bundled into either app.

- `exp.pt`: six-field score detector checkpoint.
- `exp-2.pt`: selected-song title detector checkpoint; portrait live scanning uses this model.
- `android/`: ONNX detector, PP-OCRv6 small recognizer, Unicode vocabulary and license notices.
- `ios/`: Core ML detector package. Text recognition uses system Vision.

From the repository root:

```sh
python3 scripts/export-score-detector.py --check
python3 scripts/export-score-detector.py --kind song --check
python3 scripts/fetch-paddleocr.py --check
node scripts/build-model-assets.mjs
```

The build creates disposable files in `models-worker/public/`. The model workflow
publishes them to `https://chunithmd-models.rhythmeta.org` and verifies remote hashes.
The training checkpoint is not included in the published downloads.

To re-export the detector, run `python3 scripts/export-score-detector.py`; its
default input is `model-assets/exp.pt`. Export dependencies are documented in the
script. `scripts/score-detector.json` and `scripts/paddleocr-v6.json` record model
contracts, provenance and SHA-256 hashes. Keep both platform exports in sync.

Export the song detector with `python3 scripts/export-score-detector.py --kind song`. Both detectors use 1024 RGB letterboxing; SongDetector outputs `[1,5,21504]` (title only). `scripts/song-detector.json` records its contract and hashes.

New apps request `android-v2.json` / `ios-v2.json` (both detectors plus the platform OCR assets). Legacy manifests remain unchanged so installed v1 clients continue to work. Updating reuses verified OCR/score objects already in the local model cache. Models remain outside the app bundle.
