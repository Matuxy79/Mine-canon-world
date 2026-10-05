'use strict';
/** MineCanon VS Code extension — age switcher, mod-library audit + auto-sort,
 *  and build integration for the desktop launcher and the in-game mod. All
 *  heavy logic lives in ./lib (pure Node) so `npm test` can validate headless. */

const fs = require('fs');
const os = require('os');
const path = require('path');
const vscode = require('vscode');
const canon = require('./lib/canon');
const scanner = require('./lib/modscanner');

let statusItem;
let tree;
let buildTerminal;

function cfg(key) { return vscode.workspace.getConfiguration('minecanon').get(key, ''); }
function launcherHome() { const c = cfg('launcherHome'); return c.trim() ? c : canon.defaultLauncherHome(); }
function gameDir() { const c = cfg('gameDir'); return c.trim() ? c : canon.defaultGameDir(); }
function workspaceRoot() {
  return vscode.workspace.workspaceFolders ? vscode.workspace.workspaceFolders[0].uri.fsPath : path.dirname(__dirname);
}
function libraryDir() {
  const c = cfg('libraryDir');
  if (c.trim()) return c;
  return canon.defaultLibrary(workspaceRoot());
}
function selectedAge() { return canon.selectedAge(canon.readProperties(canon.propertiesFile(launcherHome()))); }

function refreshAll() {
  const i = selectedAge();
  statusItem.text = `$(swords) ${canon.NAMES[i]}`;
  statusItem.tooltip = new vscode.MarkdownString(
    `**${canon.NAMES[i]}** — ${canon.LORE[i]}\n\n` +
    `Profile: \`Mine Canon | ${canon.NAMES[i]}\` (MC ${canon.VERSION})\n\n` +
    'Click to switch the launcher\u2019s selected age.');
  if (tree) tree.refresh();
}

/** Quick-pick an age. Accepts a raw index, a TreeItem, or nothing (asks). */
async function pickAge(arg) {
  let i = typeof arg === 'number' ? arg : (arg && typeof arg.ageIndex === 'number') ? arg.ageIndex : undefined;
  if (i !== undefined && i >= 0 && i < canon.IDS.length) return i;
  const items = canon.NAMES.map((n, idx) => ({
    label: idx === selectedAge() ? `$(check) ${n}` : n,
    description: canon.IDS[idx],
    detail: canon.LORE[idx],
    idx
  }));
  const picked = await vscode.window.showQuickPick(items, { placeHolder: 'Select the canon age' });
  return picked ? picked.idx : undefined;
}

class CanonTree {
  constructor() {
    this._emitter = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._emitter.event;
  }
  refresh() { this._emitter.fire(); }
  getTreeItem(el) { return el; }
  getChildren(el) {
    if (!el) return this.roots();
    if (el.contextValue === 'age') return modItems(path.join(canon.instanceDir(launcherHome(), el.ageIndex), 'mods'));
    if (el.contextValue === 'library') return modItems(libraryDir());
    return [];
  }
  roots() {
    const sel = selectedAge();
    const items = canon.IDS.map((id, i) => {
      const t = new vscode.TreeItem(canon.NAMES[i], vscode.TreeItemCollapsibleState.Collapsed);
      t.description = i === sel ? 'selected \u00b7 ' + id : id;
      t.tooltip = `${canon.LORE[i]}\n\ninstance: ${canon.instanceDir(launcherHome(), i)}`;
      t.contextValue = 'age';
      t.ageIndex = i;
      t.iconPath = new vscode.ThemeIcon(i === sel ? 'target' : 'circle-outline');
      t.command = { command: 'minecanon.openAgeFolder', title: 'Reveal Instance Folder', arguments: [i] };
      return t;
    });
    const lib = new vscode.TreeItem('Mod Library', vscode.TreeItemCollapsibleState.Collapsed);
    lib.contextValue = 'library';
    lib.description = path.basename(libraryDir());
    lib.tooltip = libraryDir();
    lib.iconPath = new vscode.ThemeIcon('package');
    items.push(lib);
    return items;
  }
}

function iconFor(status) {
  if (status === 'COMPATIBLE') return 'check';
  if (status === 'WRONG_LOADER') return 'close';
  if (status === 'WRONG_VERSION') return 'warning';
  return 'question';
}

function modItems(dir) {
  return scanner.scanFolder(dir).map(info => {
    const t = new vscode.TreeItem(path.basename(info.file), vscode.TreeItemCollapsibleState.None);
    t.description = info.status + (info.mcRange ? ' \u00b7 ' + info.mcRange : '');
    t.tooltip = new vscode.MarkdownString(
      `**${info.name}**\n\n${info.detail}\n\n` +
      `- Mod ID: \`${info.modId || '?'}\`\n- Version: \`${info.version || '?'}\`\n` +
      `- Loader: \`${info.loader}\`\n- MC range: \`${info.mcRange || 'none'}\``);
    t.iconPath = new vscode.ThemeIcon(iconFor(info.status));
    t.command = { command: 'minecanon.revealFile', title: 'Reveal in Explorer', arguments: [info.file] };
    return t;
  });
}

async function selectAge(arg) {
  const i = await pickAge(arg);
  if (i === undefined) return;
  canon.setSelectedAge(launcherHome(), i);
  refreshAll();
  vscode.window.showInformationMessage(
    `Desktop launcher age set to ${canon.NAMES[i]}. "Prepare profile" now targets Mine Canon | ${canon.NAMES[i]}.`);
}

async function sortIntoAge(arg) {
  const i = await pickAge(arg);
  if (i === undefined) return;
  const dry = await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: `MineCanon: scanning library for ${canon.NAMES[i]}` },
    () => Promise.resolve(canon.sortInto(launcherHome(), libraryDir(), i, true, scanner)));
  if (dry.copied === 0) {
    vscode.window.showInformationMessage(
      `Nothing to copy for ${canon.NAMES[i]}: ${dry.present} already present, ${dry.skipped} incompatible.`);
    return;
  }
  const yes = `Copy ${dry.copied} mod(s)`;
  const answer = await vscode.window.showWarningMessage(
    `Copy ${dry.copied} compatible mod(s) into ${canon.NAMES[i]}? ` +
    `(${dry.skipped} incompatible will be skipped, ${dry.present} already present.)`, { modal: true }, yes);
  if (answer !== yes) return;
  const r = await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: `MineCanon: sorting mods into ${canon.NAMES[i]}` },
    () => Promise.resolve(canon.sortInto(launcherHome(), libraryDir(), i, false, scanner)));
  refreshAll();
  vscode.window.showInformationMessage(
    `Copied ${r.copied} mod(s) into ${canon.NAMES[i]}. ` +
    `Skipped: ${r.skipped} incompatible, ${r.present} already present, ${r.failed} failed. ` +
    'Start the game from the official launcher to load them.');
}

async function scanAndShow(dir, what) {
  const infos = await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: `MineCanon: scanning ${what}` },
    () => Promise.resolve(scanner.scanFolder(dir)));
  if (infos.length === 0) { vscode.window.showInformationMessage(`MineCanon: no JAR files found in ${what}.`); return; }
  const ok = infos.filter(i => i.status === 'COMPATIBLE').length;
  vscode.window.showInformationMessage(`${what}: ${ok} of ${infos.length} JAR(s) are compatible with MC 1.20.1 / Forge 47.4.26.`);
  await vscode.window.showQuickPick(infos.map(i => ({
    label: `$(${iconFor(i.status)}) ${i.name || path.basename(i.file)}`,
    description: i.loader + (i.mcRange ? ' \u00b7 ' + i.mcRange : ''),
    detail: i.detail
  })), { placeHolder: `${what} \u2014 ${infos.length} JAR(s), ${ok} compatible` });
}

function showState() {
  const s = canon.readCanonState(gameDir());
  if (!s.exists) {
    vscode.window.showInformationMessage(
      'No canon state yet. It is written by the launcher\u2019s "Prepare profile" action and by in-game sorts (press O).');
    return;
  }
  vscode.window.showTextDocument(vscode.Uri.file(s.file), { preview: true });
}

function revealFolder(dir) {
  fs.mkdirSync(dir, { recursive: true });
  vscode.commands.executeCommand('revealInExplorer', vscode.Uri.file(dir));
}

async function openAgeFolder(arg) {
  const i = await pickAge(arg);
  if (i !== undefined) revealFolder(canon.instanceDir(launcherHome(), i));
}

let contextRef;

function runInBuildTerminal(cmdline) {
  if (!buildTerminal) {
    buildTerminal = vscode.window.createTerminal({ name: 'MineCanon Build', shellPath: 'cmd.exe', shellArgs: ['/k'] });
    contextRef.subscriptions.push(buildTerminal);
  }
  buildTerminal.show();
  buildTerminal.sendText(cmdline);
}

function buildLauncher() {
  const script = path.join(workspaceRoot(), 'launcher', 'Build-MineCanon.ps1');
  if (!fs.existsSync(script)) {
    vscode.window.showErrorMessage(`Build-MineCanon.ps1 not found under ${workspaceRoot()}. Open the Mine-canon-world workspace first.`);
    return;
  }
  runInBuildTerminal(`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "${script}"`);
}

function findJdk17() {
  const c = cfg('jdk17');
  if (c.trim() && fs.existsSync(path.join(c, 'release'))) return c;
  const jh = process.env.JAVA_HOME;
  if (jh) { try { if (fs.readFileSync(path.join(jh, 'release'), 'utf8').includes('JAVA_VERSION="17')) return jh; } catch { /* not 17 */ } }
  const pack = path.join(process.env.APPDATA || '', 'Code', 'User', 'globalStorage',
    'pleiades.java-extension-pack-jdk', 'java', '17');
  if (fs.existsSync(path.join(pack, 'release'))) return pack;
  return null;
}

function buildUiMod() {
  const modDir = path.join(workspaceRoot(), 'minecanon-ui');
  if (!fs.existsSync(path.join(modDir, 'build.gradle'))) {
    vscode.window.showErrorMessage('minecanon-ui not found in this workspace. Open the Mine-canon-world folder first.');
    return;
  }
  const jdk = findJdk17();
  if (!jdk) {
    vscode.window.showErrorMessage('No JDK 17 found. Set minecanon.jdk17 in Settings, or install the VS Code Java extension pack.');
    return;
  }
  const staged = path.join(os.tmpdir(), 'gradle-8.8', 'bin', process.platform === 'win32' ? 'gradle.bat' : 'gradle');
  const gradle = fs.existsSync(staged) ? staged : 'gradle';
  if (gradle === 'gradle') {
    vscode.window.showWarningMessage('Using Gradle from PATH. This ForgeGradle 6 build needs Gradle 8.x \u2014 the extension pack\u2019s Gradle 9.8 will fail.');
  }
  // The build dir must live outside OneDrive so Gradle's file handling does not stall.
  const buildDir = path.join(os.tmpdir(), 'minecanon-ui-build');
  runInBuildTerminal(`cd /d "${modDir}" && set "JAVA_HOME=${jdk}" && set "MINECANON_BUILD_DIR=${buildDir}" && call "${gradle}" --no-daemon --console=plain build`);
}

function installUiMod() {
  const root = workspaceRoot();
  const jarName = 'minecanon-ui-1.0.0.jar';
  const target = path.join(root, 'launcher', 'bin', 'MineCanon', 'app', jarName);
  const candidates = [
    path.join(os.tmpdir(), 'minecanon-ui-build', 'libs', jarName),
    path.join(root, 'minecanon-ui', 'build', 'libs', jarName)
  ].filter(p => fs.existsSync(p) && path.resolve(p) !== path.resolve(target))
    .sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs);
  if (candidates.length === 0) {
    vscode.window.showWarningMessage(`No built ${jarName} found. Run "MineCanon: Build minecanon-ui Mod" first.`);
    return;
  }
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(candidates[0], target);
  vscode.window.showInformationMessage(
    `Installed ${jarName} into the launcher app folder. The next "Prepare profile" copies it into the age instance.`);
}

function activate(context) {
  contextRef = context;
  statusItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
  statusItem.command = 'minecanon.selectAge';
  statusItem.show();

  tree = new CanonTree();
  const treeView = vscode.window.createTreeView('minecanon.ages', { treeDataProvider: tree, canSelectMany: false });

  const subs = context.subscriptions;
  subs.push(statusItem, treeView);
  subs.push(vscode.commands.registerCommand('minecanon.selectAge', selectAge));
  subs.push(vscode.commands.registerCommand('minecanon.sortIntoAge', sortIntoAge));
  subs.push(vscode.commands.registerCommand('minecanon.scanLibrary', () => scanAndShow(libraryDir(), 'Mod library')));
  subs.push(vscode.commands.registerCommand('minecanon.scanGameMods', () => scanAndShow(path.join(gameDir(), 'mods'), 'Game mods folder')));
  subs.push(vscode.commands.registerCommand('minecanon.showState', showState));
  subs.push(vscode.commands.registerCommand('minecanon.openAgeFolder', openAgeFolder));
  subs.push(vscode.commands.registerCommand('minecanon.openLibraryFolder', () => revealFolder(libraryDir())));
  subs.push(vscode.commands.registerCommand('minecanon.openGameModsFolder', () => revealFolder(path.join(gameDir(), 'mods'))));
  subs.push(vscode.commands.registerCommand('minecanon.revealFile', f => revealFolder(path.dirname(f))));
  subs.push(vscode.commands.registerCommand('minecanon.buildLauncher', buildLauncher));
  subs.push(vscode.commands.registerCommand('minecanon.buildUiMod', buildUiMod));
  subs.push(vscode.commands.registerCommand('minecanon.installUiMod', installUiMod));
  subs.push(vscode.commands.registerCommand('minecanon.refresh', () => refreshAll()));
  subs.push(vscode.workspace.onDidChangeConfiguration(e => { if (e.affectsConfiguration('minecanon')) refreshAll(); }));
  refreshAll();
}

module.exports = { activate };