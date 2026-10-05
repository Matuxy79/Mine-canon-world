'use strict';
/** MineCanon shared data and filesystem helpers. Pure Node (no VS Code API) so
 *  the self-test can run it outside the editor. Mirrors
 *  launcher/src/LauncherService.java and the in-game CanonState.java. */

const fs = require('fs');
const os = require('os');
const path = require('path');

const VERSION = '1.20.1-forge-47.4.26';
const TARGET_MC = '1.20.1';
const IDS = ['first-breath', 'nocturne-west', 'chakra-nations', 'nen-new-world', 'curse-modernity', 'dark-continent'];
const NAMES = ['First Breath', 'Nocturne West', 'Chakra Nations', 'Nen New World', 'Curse Modernity', 'Dark Continent'];
const LORE = [
  'Breath, bending and the first disciplines of the Canon Field.',
  'Bloodlines, sacred relics and the moonlit western kingdoms.',
  'Seals, summons and the rise of the clan-state civilizations.',
  'Aura, contracts and the age of licensed hunters.',
  'Domains, binding vows and the hidden urban world.',
  'Forbidden archaeology beyond the boundaries of known canon.'
];

function defaultLauncherHome() { return path.join(os.homedir(), 'MineCanon'); }

function defaultGameDir() {
  if (process.platform === 'win32' && process.env.APPDATA) return path.join(process.env.APPDATA, '.minecraft');
  if (process.platform === 'darwin') return path.join(os.homedir(), 'Library', 'Application Support', 'minecraft');
  return path.join(os.homedir(), '.minecraft');
}

/** Mod library: explicit setting wins, else mods-1.20.1-base found walking up from `from`. */
function defaultLibrary(from) {
  for (let probe = from || process.cwd(); probe; probe = path.dirname(probe)) {
    const candidate = path.join(probe, 'mods-1.20.1-base');
    try { if (fs.statSync(candidate).isDirectory()) return candidate; } catch { /* keep walking */ }
  }
  return path.join(from || process.cwd(), 'mods-1.20.1-base');
}

function propertiesFile(home) { return path.join(home, 'launcher.properties'); }

/** Parses a Java .properties file, preserving key order. Ignores the comment
 *  lines Java's store() writes; separator is the first '=' or ':'. */
function readProperties(file) {
  const out = [];
  if (!fs.existsSync(file)) return out;
  const text = fs.readFileSync(file, 'utf8');
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#') || line.startsWith('!')) continue;
    let eq = -1;
    for (let i = 0; i < line.length; i++) { const c = line[i]; if (c === '=' || c === ':') { eq = i; break; } }
    if (eq < 0) continue;
    out.push([line.slice(0, eq).trim(), line.slice(eq + 1).trim()]);
  }
  return out;
}

/** Writes back in Java Properties store() format; load() ignores the header. */
function writeProperties(file, entries) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const lines = ['# Mine Canon - local launcher settings'];
  for (const [k, v] of entries) lines.push(k + '=' + v);
  const tmp = file + '.tmp';
  fs.writeFileSync(tmp, lines.join('\n') + '\n', 'utf8');
  fs.renameSync(tmp, file);
}

/** Clamps and returns the `selected` age index from launcher.properties entries. */
function selectedAge(entries) {
  for (const [k, v] of entries) {
    if (k === 'selected') {
      const n = parseInt(v, 10);
      if (Number.isFinite(n)) return Math.max(0, Math.min(IDS.length - 1, n));
    }
  }
  return 0;
}

/** Sets `selected` in the desktop launcher's launcher.properties, same as CanonState.setAge. */
function setSelectedAge(home, index) {
  const file = propertiesFile(home);
  const kept = readProperties(file).filter(([k]) => k !== 'selected');
  kept.push(['selected', String(index)]);
  writeProperties(file, kept);
}

function instanceDir(home, i) { return path.join(home, 'instances', IDS[i]); }

/** Creates the age instance's mods/saves/resourcepacks folders. */
function ensureInstance(home, i) {
  const base = instanceDir(home, i);
  for (const d of ['mods', 'saves', 'resourcepacks']) fs.mkdirSync(path.join(base, d), { recursive: true });
  return base;
}

/** Lists *.jar regular files in a folder, lowercase-name sorted; [] when missing. */
function listJars(dir) {
  try {
    return fs.readdirSync(dir, { withFileTypes: true })
      .filter(e => e.isFile() && e.name.toLowerCase().endsWith('.jar'))
      .map(e => path.join(dir, e.name))
      .sort((a, b) => { const x = a.toLowerCase(), y = b.toLowerCase(); return x < y ? -1 : x > y ? 1 : 0; });
  } catch { return []; }
}

/** Copies every compatible library mod into the age's instance mods folder.
 *  Returns { copied, skipped, present, failed } — mirrors LauncherService.sortInto.
 *  Library originals are never modified; dry runs never touch the disk. */
function sortInto(home, libraryDir, ageIndex, dryRun, scanner) {
  const counts = { copied: 0, skipped: 0, present: 0, failed: 0 };
  const modsDir = path.join(instanceDir(home, ageIndex), 'mods');
  if (!dryRun) fs.mkdirSync(modsDir, { recursive: true });
  for (const info of scanner.scanFolder(libraryDir)) {
    if (info.status !== 'COMPATIBLE') { counts.skipped++; continue; }
    const target = path.join(modsDir, path.basename(info.file));
    if (fs.existsSync(target)) { counts.present++; continue; }
    if (dryRun) { counts.copied++; continue; }
    try { fs.copyFileSync(info.file, target); counts.copied++; } catch { counts.failed++; }
  }
  return counts;
}

/** Reads the minecanon-state.json bridge written by "Prepare profile" and the in-game UI. */
function readCanonState(gameDir) {
  const file = path.join(gameDir, 'minecanon-state.json');
  const state = { file, exists: fs.existsSync(file), age: '', ageName: '', lore: '', library: '', lastSort: '' };
  if (!state.exists) return state;
  try {
    const o = JSON.parse(fs.readFileSync(file, 'utf8'));
    for (const k of ['age', 'ageName', 'lore', 'library', 'lastSort']) {
      if (typeof o[k] === 'string') state[k] = o[k];
    }
    for (const k of ['lastSortCopied', 'lastSortSkipped', 'lastSortPresent', 'lastSortFailed']) {
      if (o[k] !== undefined) state[k] = o[k];
    }
  } catch (e) { state.error = String((e && e.message) || e); }
  return state;
}

module.exports = {
  VERSION, TARGET_MC, IDS, NAMES, LORE,
  defaultLauncherHome, defaultGameDir, defaultLibrary, propertiesFile,
  readProperties, writeProperties, selectedAge, setSelectedAge,
  instanceDir, ensureInstance, listJars, sortInto, readCanonState
};