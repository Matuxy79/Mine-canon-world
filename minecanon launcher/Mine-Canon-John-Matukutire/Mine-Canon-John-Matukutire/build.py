"""Build with Python 3 and Java 17+ JDK. No downloads or package manager."""
import pathlib, subprocess, zipfile
root=pathlib.Path(__file__).resolve().parent
(root/'build').mkdir(exist_ok=True)
forge=root/'forge-1.20.1-47.4.26-installer.jar'
subprocess.run(['java','com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',str(forge),'-d',str(root/'build'),*[str(p) for p in sorted((root/'src').glob('*.java'))]],check=True)
manifest='Manifest-Version: 1.0\r\nMain-Class: MineCanonLauncher\r\nClass-Path: forge-1.20.1-47.4.26-installer.jar\r\n\r\n'
with zipfile.ZipFile(root/'MineCanonLauncher.jar','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('META-INF/MANIFEST.MF',manifest)
 for p in (root/'build').rglob('*.class'):
  if not p.name.startswith('SelfTest'):z.write(p,p.relative_to(root/'build'))
 for p in (root/'assets').glob('*'):z.write(p,p.relative_to(root))
print('Built',root/'MineCanonLauncher.jar')
