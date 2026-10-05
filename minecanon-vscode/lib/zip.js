'use strict';
/** Minimal ZIP reader used to pull metadata out of mod JARs without loading any
 *  mod code. Node's zlib covers DEFLATE (method 8) and STORED (method 0); the
 *  central directory is parsed by hand. No external packages, so the extension
 *  ships with zero dependencies and the self-test runs under plain node. */

const zlib = require('zlib');

const EOCD_SIG = 0x06054b50; // end of central directory
const CDE_SIG = 0x02014b50;   // central directory entry
const LFH_SIG = 0x04034b50;  // local file header

function findEOCD(buf) {
  const start = Math.max(0, buf.length - 0x10000 - 22);
  for (let i = buf.length - 22; i >= start; i--) {
    if (buf.readUInt32LE(i) === EOCD_SIG) {
      return { count: buf.readUInt16LE(i + 10), offset: buf.readUInt32LE(i + 16) };
    }
  }
  throw new Error('not a ZIP file (no end-of-central-directory record)');
}

/** Central directory entries: { name, method, compSize, uncompSize, localOffset }. */
function entries(buf) {
  const eocd = findEOCD(buf);
  if (eocd.count === 0xFFFF || eocd.offset === 0xFFFFFFFF) {
    throw new Error('ZIP64 archives are not supported');
  }
  const list = [];
  let p = eocd.offset;
  for (let n = 0; n < eocd.count; n++) {
    if (buf.readUInt32LE(p) !== CDE_SIG) throw new Error('corrupt central directory at offset ' + p);
    const nameLen = buf.readUInt16LE(p + 28);
    const extraLen = buf.readUInt16LE(p + 30);
    const commentLen = buf.readUInt16LE(p + 32);
    list.push({
      method: buf.readUInt16LE(p + 10),
      compSize: buf.readUInt32LE(p + 20),
      uncompSize: buf.readUInt32LE(p + 24),
      localOffset: buf.readUInt32LE(p + 42),
      name: buf.toString('utf8', p + 46, p + 46 + nameLen)
    });
    p += 46 + nameLen + extraLen + commentLen;
  }
  return list;
}

/** Extracts one entry's UTF-8 text from a JAR buffer, or null when absent. */
function readTextEntry(buf, wanted) {
  for (const e of entries(buf)) {
    if (e.name !== wanted) continue;
    if (buf.readUInt32LE(e.localOffset) !== LFH_SIG) throw new Error('corrupt local header for ' + wanted);
    const nameLen = buf.readUInt16LE(e.localOffset + 26);
    const extraLen = buf.readUInt16LE(e.localOffset + 28);
    const dataStart = e.localOffset + 30 + nameLen + extraLen;
    const data = buf.subarray(dataStart, dataStart + e.compSize);
    if (e.method === 0) return data.toString('utf8');
    if (e.method === 8) return zlib.inflateRawSync(data).toString('utf8');
    throw new Error('unsupported ZIP compression method ' + e.method);
  }
  return null;
}

module.exports = { entries, readTextEntry };