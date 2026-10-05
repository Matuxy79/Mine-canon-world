'use strict';
/** Headless self-test for the extension's pure-Node libraries. Run with:
 *      npm test        (or: node test/selftest.js)
 *  Verifies scanner parity with the Java ModScanner plus the sort and
 *  properties helpers, against the real mod library and game mods folder. */

const fs = require('fs');
const os = require('os');
const path = require('path');
const canon = require('../lib/canon');
const scanner = require('../lib/modscanner');
const zip = require('../lib/zip');

let pass = 0, fail = 0;
function check(cond, label) {
  if (cond) { pass++; console.log('  ok   ' + label); }
  else { fail++; console.log('  FAIL ' + label); }
}

const LIB = process.env.MINECANON_LIB || canon.defaultLibrary(path.resolve(__dirname, '..'));
const GAME = process.env.MINECANON_GAME || canon.defaultGameDir();

console.log('MineCanon VS Code extension self-test');
console.log('library:  ' + LIB);
console.log('game dir: ' + GAME);

console.log('\n[1] version comparison parity (ModScanner.cmp / versionMatches)');
check(scanner.cmp('1.20.1', '1.20.1') === 0, 'cmp equal versions');
check(scanner.cmp('1.20', '1.20.1') < 0, 'cmp shorter version is smaller (missing part = 0)');
check(scanner.cmp('1.20.1', '1.20.10') < 0, 'cmp numeric component compare');
check(scanner.cmp('1.20.1', '1.20') > 0, 'cmp longer version is greater');
const MATCH = {
  '1.20.1': true, '[1.20.1]': true, '[1.20,)': true, '(,1.21)': true,
  '[1.20.1,1.21)': true, '[1.20,1.20.2]': true, '1.20 1.20.1': true,
  '[1.20.2,)': false, '(,1.20]': false, '1.20.x': false
};
for (const [range, want] of Object.entries(MATCH)) {
  check(scanner.versionMatches(range, '1.20.1') === want, `versionMatches('${range}') === ${want}`);
}

console.log('\n[2] mods.toml parser');
const sample = [
  'modLoader="javafml"', 'loaderVersion="[47,)"', 'license="CC-BY-SA-4.0"', '',
  '[[mods]]', 'modId="testmod"', 'version="1.2.3"', 'displayName="Test Mod"', '',
  '[[dependencies.testmod]]', 'modId="forge"', 'versionRange="[47,)"', '',
  '[[dependencies.testmod]]', 'modId="minecraft"', 'versionRange="[1.20,1.21)"'
].join('\n');
const p = scanner.parseModsToml(sample);
check(p[0] === 'testmod', 'toml modId');
check(p[1] === '1.2.3', 'toml version');
check(p[2] === 'Test Mod', 'toml displayName');
check(p[3] === '[1.20,1.21)', 'toml minecraft versionRange (not the forge one)');

console.log('\n[3] zip reader on a real JAR');
const curios = path.join(LIB, 'curios-forge-5.14.1+1.20.1.jar');
if (fs.existsSync(curios)) {
  const entry = zip.readTextEntry(fs.readFileSync(curios), 'META-INF/mods.toml');
  check(entry !== null && entry.includes('modLoader'), 'reads META-INF/mods.toml from a real jar');
} else {
  check(false, 'curios jar present in library');
}

console.log('\n[4] library scan verdicts');
const lib = scanner.scanFolder(LIB);
console.log('    ' + lib.length + ' jar(s):');
for (const m of lib) console.log(`    ${m.status.padEnd(13)} ${path.basename(m.file)}  [${m.loader} ${m.mcRange}] ${m.detail}`);
function verdictOf(name) { const m = lib.find(x => path.basename(x.file) === name); return m ? m.status : 'MISSING'; }
check(lib.length >= 17, `library has at least 17 jars (found ${lib.length})`);
check(verdictOf('curios-forge-5.14.1+1.20.1.jar') === 'COMPATIBLE', 'curios verdict COMPATIBLE');
check(verdictOf('geckolib-forge-1.20.1-4.8.4.jar') === 'COMPATIBLE', 'geckolib verdict COMPATIBLE');
check(verdictOf('Cobblemon-fabric-1.5.2+1.20.1.jar') === 'WRONG_LOADER', 'cobblemon-fabric verdict WRONG_LOADER');
check(verdictOf('Configured-mc1.20.4-v1.4.1.jar') === 'WRONG_LOADER',
  'Configured jar is a Fabric build, verdict WRONG_LOADER (loader checked before version, same as Java)');
check(verdictOf('forge-1.20.1-47.4.26-installer.jar') === 'UNKNOWN', 'forge installer verdict UNKNOWN');

console.log('\n[5] game mods scan (expect all COMPATIBLE)');
const game = scanner.scanFolder(path.join(GAME, 'mods'));
for (const m of game) console.log(`    ${m.status.padEnd(13)} ${path.basename(m.file)}`);
check(game.length >= 14, `game mods folder has the cleaned jars (found ${game.length})`);
check(game.every(m => m.status === 'COMPATIBLE'), 'every game mod is COMPATIBLE');

console.log('\n[6] launcher.properties round-trip');
const tmpHome = fs.mkdtempSync(path.join(os.tmpdir(), 'minecanon-selftest-'));
canon.setSelectedAge(tmpHome, 3);
check(canon.selectedAge(canon.readProperties(canon.propertiesFile(tmpHome))) === 3, 'setSelectedAge/readProperties round-trip');
canon.setSelectedAge(tmpHome, 5);
check(canon.selectedAge(canon.readProperties(canon.propertiesFile(tmpHome))) === 5, 'selected age updates in place');

console.log('\n[7] auto-sort into a temp age instance');
const compatibleCount = lib.filter(m => m.status === 'COMPATIBLE').length;
const dry = canon.sortInto(tmpHome, LIB, 0, true, scanner);
check(dry.copied === compatibleCount && dry.present === 0 && dry.failed === 0,
  `dry-run plans ${compatibleCount} copies, ${dry.skipped} skips`);
const real = canon.sortInto(tmpHome, LIB, 0, false, scanner);
check(real.copied === compatibleCount, `real sort copies ${compatibleCount} mods`);
check(canon.listJars(path.join(canon.instanceDir(tmpHome, 0), 'mods')).length === compatibleCount,
  'instance mods folder holds the copies');
const again = canon.sortInto(tmpHome, LIB, 0, false, scanner);
check(again.copied === 0 && again.present === compatibleCount, 'second run reports everything already present');

console.log('\n[8] canon state bridge');
const state = canon.readCanonState(GAME);
console.log('    exists=' + state.exists + (state.exists ? ` age=${state.age} lastSort=${state.lastSort}` : ' (written by Prepare profile / in-game sort)'));

fs.rmSync(tmpHome, { recursive: true, force: true });
console.log(`\n${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);