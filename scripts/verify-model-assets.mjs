import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

const base = process.env.CHUNITHMD_MODEL_BASE_URL ?? 'https://chunithmd-models.rhythmeta.org';
for (const platform of ['android', 'ios']) {
  const response = await fetch(`${base}/${platform}.json`, { cache: 'no-cache', signal: AbortSignal.timeout(60000) });
  if (!response.ok) throw new Error(`Manifest HTTP ${response.status}`);
  const manifest = await response.json();
  const local = JSON.parse(readFileSync(new URL(`../models-worker/public/${platform}.json`, import.meta.url)));
  if (JSON.stringify(manifest) !== JSON.stringify(local)) throw new Error('Published manifest differs');
  for (const entry of manifest.entries) {
    const file = await fetch(`${base}/files/${entry.sha256}`, { signal: AbortSignal.timeout(120000) });
    if (!file.ok) throw new Error(`Asset HTTP ${file.status}`);
    const bytes = Buffer.from(await file.arrayBuffer());
    if (bytes.length !== entry.size || createHash('sha256').update(bytes).digest('hex') !== entry.sha256)
      throw new Error(`Corrupt asset: ${entry.filename}`);
    console.log(`Verified ${platform}/${entry.filename}`);
  }
}
