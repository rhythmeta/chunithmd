import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync, rmSync, copyFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const output = resolve(root, 'models-worker/public');
const assets = {
  android: ['ScoreDetector.onnx', 'PaddleOCRv6Small.onnx', 'PaddleOCRv6SmallVocab.json'],
  ios: ['ScoreDetector.mlpackage/Manifest.json', 'ScoreDetector.mlpackage/Data/com.apple.CoreML/model.mlmodel',
    'ScoreDetector.mlpackage/Data/com.apple.CoreML/weights/weight.bin'],
};
rmSync(output, { recursive: true, force: true });
mkdirSync(output, { recursive: true });
for (const [platform, files] of Object.entries(assets)) {
  const entries = files.map(filename => {
    const source = resolve(root, 'model-assets', platform, filename);
    const bytes = readFileSync(source);
    const sha256 = createHash('sha256').update(bytes).digest('hex');
    const target = resolve(output, 'files', sha256);
    mkdirSync(dirname(target), { recursive: true });
    copyFileSync(source, target);
    return { filename, sha256, size: bytes.length };
  });
  // Keep v1 manifests unchanged for installed clients that validate an exact file whitelist.
  writeFileSync(resolve(output, `${platform}.json`), JSON.stringify({ schemaVersion: 1, entries }) + '\n');
  const songFiles = platform === 'android' ? ['SongDetector.onnx'] : [
    'SongDetector.mlpackage/Manifest.json', 'SongDetector.mlpackage/Data/com.apple.CoreML/model.mlmodel',
    'SongDetector.mlpackage/Data/com.apple.CoreML/weights/weight.bin'];
  const songEntries = songFiles.map(filename => {
    const source = resolve(root, 'model-assets', platform, filename);
    const bytes = readFileSync(source);
    const sha256 = createHash('sha256').update(bytes).digest('hex');
    copyFileSync(source, resolve(output, 'files', sha256));
    return { filename, sha256, size: bytes.length };
  });
  writeFileSync(resolve(output, `${platform}-v2.json`), JSON.stringify({ schemaVersion: 1, entries: [...entries, ...songEntries] }) + '\n');
  console.log(`${platform}: ${entries.length} files, ${entries.reduce((n, e) => n + e.size, 0)} bytes`);
}
for (const filename of ['PaddleOCR-LICENSE.txt', 'PaddleOCR-NOTICE.txt']) {
  copyFileSync(resolve(root, 'model-assets/android', filename), resolve(output, filename));
}
writeFileSync(resolve(output, '_headers'), '/*.json\n  Cache-Control: public, max-age=60, must-revalidate\n/files/*\n  Cache-Control: public, max-age=31536000, immutable\n');
