# Scanner model sources

These files are published separately and are not bundled into either app.

- `exp.pt`: original six-field detector checkpoint, retained for reproducible exports.
- `android/`: ONNX detector, PP-OCRv6 small recognizer, Unicode vocabulary and license notices.
- `ios/`: Core ML detector package. Text recognition uses system Vision.

From the repository root:

```sh
python3 scripts/export-score-detector.py --check
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
