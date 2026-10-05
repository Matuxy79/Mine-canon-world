'use strict';
/** JavaScript twin of launcher/src/ModScanner.java (and the in-game mod's copy):
 *  reads mod JAR metadata — META-INF/mods.toml, fabric.mod.json or mcmod.info —
 *  and gives the same COMPATIBLE / WRONG_LOADER / WRONG_VERSION / UNKNOWN
 *  verdicts for the pinned 1.20.1-forge-47.4.26 profile. Pure Node, no deps. */

const fs = require('fs');
const path = require('path');
const zip = require('./zip');

const TARGET_MC = '1.20.1';

function scanFolder(dir) {
  try {
    const names = fs.readdirSync(dir, { withFileTypes: true })
      .filter(e => e.isFile() && e.name.toLowerCase().endsWith('.jar'))
      .map(e => e.name)
      .sort((a, b) => { const x = a.toLowerCase(), y = b.toLowerCase(); return x < y ? -1 : x > y ? 1 : 0; });
    return names.map(n => scanJar(path.join(dir, n)));
  } catch { return []; }
}

function scanJar(file) {
  let modId = '', name = '', version = '', loader = 'unknown', mcRange = '';
  try {
    const buf = fs.readFileSync(file);
    const toml = zip.readTextEntry(buf, 'META-INF/mods.toml');
    if (toml !== null) {
      loader = 'forge';
      const p = parseModsToml(toml);
      modId = p[0]; version = p[1]; name = p[2]; mcRange = p[3];
    } else {
      const fabric = zip.readTextEntry(buf, 'fabric.mod.json');
      if (fabric !== null) {
        loader = 'fabric';
        const o = JSON.parse(fabric);
        modId = text(o, 'id'); version = text(o, 'version');
        name = text(o, 'name'); if (!name) name = modId;
        if (o.depends && o.depends.minecraft) {
          const mc = Array.isArray(o.depends.minecraft) ? o.depends.minecraft[0] : o.depends.minecraft;
          if (typeof mc === 'string') mcRange = mc;
        }
      } else {
        const legacy = zip.readTextEntry(buf, 'mcmod.info');
        if (legacy !== null) {
          loader = 'forge';
          const a = JSON.parse(legacy);
          if (Array.isArray(a) && a.length && typeof a[0] === 'object' && a[0] !== null) {
            modId = text(a[0], 'modid'); name = text(a[0], 'name');
            version = text(a[0], 'version'); mcRange = text(a[0], 'mcversion');
          }
        }
      }
    }
  } catch (e) {
    return { file, modId: '', name: '', version: '', loader: 'unknown', mcRange: '', status: 'UNKNOWN', detail: 'could not read JAR: ' + errText(e) };
  }
  if (!name) name = path.basename(file);
  let status, detail;
  if (loader === 'fabric') { status = 'WRONG_LOADER'; detail = 'Fabric mod; this profile runs Forge.'; }
  else if (loader !== 'forge') { status = 'UNKNOWN'; detail = 'No mod metadata found (server plugin or unknown format).'; }
  else if (!mcRange) { status = 'COMPATIBLE'; detail = 'Forge mod; no Minecraft version range declared.'; }
  else if (versionMatches(mcRange, TARGET_MC)) { status = 'COMPATIBLE'; detail = 'Forge mod for Minecraft ' + TARGET_MC + '.'; }
  else { status = 'WRONG_VERSION'; detail = 'Declares ' + mcRange + ', not ' + TARGET_MC + '.'; }
  return { file, modId, name, version, loader, mcRange, status, detail };
}

function errText(e) { return (e && e.message) ? e.message : String(e); }

/** modId, version, displayName, minecraft range from a Forge mods.toml subset —
 *  deliberately mirrors the Java parser, quirks included. */
function parseModsToml(toml) {
  let modId = '', version = '', name = '', range = '';
  let inMods = false, captureRange = false, depForMc = false;
  for (const raw of toml.split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    if (line.startsWith('[[')) {
      const sec = line.slice(2, line.lastIndexOf(']]')).trim();
      inMods = sec === 'mods';
      captureRange = sec.startsWith('dependencies.');
      if (captureRange) depForMc = false;
      continue;
    }
    if (line.startsWith('[')) { inMods = false; captureRange = false; continue; }
    const eq = line.indexOf('=');
    if (eq < 0) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if (value.length >= 2 && value[0] === '"') {
      const end = value.indexOf('"', 1);
      if (end > 0) value = value.slice(1, end);
    }
    if (captureRange) {
      if (key === 'modId') depForMc = value === 'minecraft';
      else if (key === 'versionRange' && depForMc && !range) range = value;
    } else if (inMods) {
      if (key === 'modId' && !modId) modId = value;
      else if (key === 'version' && !version) version = value;
      else if (key === 'displayName' && !name) name = value;
    }
  }
  return [modId, version, name, range];
}

function text(o, key) { const v = o ? o[key] : undefined; return typeof v === 'string' ? v : ''; }

/** Maven-style version range check: "1.20.1", "[1.20.1]", "[1.20,)", "(,1.21)", "[1.20.1,1.21)". */
function versionMatches(range, target) {
  range = range.trim();
  if (!range) return true;
  for (const clause of range.split(/\s+/)) if (matchesClause(clause, target)) return true;
  return false;
}

function matchesClause(clause, target) {
  if (!clause.includes(',')) {
    let v = clause;
    if ((v.startsWith('[') || v.startsWith('(')) && v.length > 2) v = v.slice(1, -1);
    return cmp(v, target) === 0;
  }
  const lowerInclusive = clause.startsWith('['), upperInclusive = clause.endsWith(']');
  const inner = clause.slice(1, -1);
  const parts = inner.split(',', 2);
  const lower = parts[0].trim(), upper = parts.length > 1 ? parts[1].trim() : '';
  if (lower && ((lowerInclusive && cmp(lower, target) > 0) || (!lowerInclusive && cmp(lower, target) >= 0))) return false;
  if (upper && ((upperInclusive && cmp(upper, target) < 0) || (!upperInclusive && cmp(upper, target) <= 0))) return false;
  return true;
}

/** Component-wise compare; numeric when both parts parse as integers, else plain
 *  string compare — identical semantics to ModScanner.cmp in Java. */
function cmp(a, b) {
  const NUM = /^[+-]?\d+$/;
  const x = a.split(/[-._]/), y = b.split(/[-._]/);
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const p = i < x.length ? x[i] : '0', q = i < y.length ? y[i] : '0';
    let c;
    if (NUM.test(p) && NUM.test(q)) c = Math.sign(parseInt(p, 10) - parseInt(q, 10));
    else c = p < q ? -1 : p > q ? 1 : 0;
    if (c !== 0) return c;
  }
  return 0;
}

module.exports = { TARGET_MC, scanFolder, scanJar, parseModsToml, versionMatches, matchesClause, cmp };