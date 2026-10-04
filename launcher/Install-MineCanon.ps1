$ErrorActionPreference = 'Stop'
$launcher = $PSScriptRoot
$root = Split-Path $launcher -Parent
if (-not (Test-Path (Join-Path $launcher 'assets\canon-landscape.png'))) {
    throw 'MineCanon artwork is missing: launcher\assets\canon-landscape.png'
}
$jdk = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
    $jdk = $env:JAVA_HOME
} else {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $jdk = Split-Path (Split-Path $compiler.Source -Parent) -Parent }
}
if (-not $jdk) { throw 'Install a JDK 17 or newer with jpackage, or set JAVA_HOME to it.' }
foreach ($tool in @('javac', 'java', 'jar', 'jlink', 'jpackage')) {
    if (-not (Test-Path (Join-Path $jdk "bin\$tool.exe"))) { throw "Missing JDK tool: $tool" }
}

$build = Join-Path $launcher '.build'
$classes = Join-Path $build 'classes'
$inputDir = Join-Path $build 'input'
$dist = Join-Path $launcher 'dist'
$app = Join-Path $dist 'MineCanon'
$exe = Join-Path $app 'MineCanon.exe'
$icon = Join-Path $build 'MineCanon.ico'
if (Test-Path $app) {
    $running = @(Get-CimInstance Win32_Process | Where-Object {
        $_.ExecutablePath -and $_.ExecutablePath.StartsWith("$app\", [StringComparison]::OrdinalIgnoreCase)
    })
    if ($running.Count -gt 0) { throw 'Close MineCanon and its Forge installer before updating the bundled runtime.' }
}
New-Item -ItemType Directory -Force -Path $classes, $inputDir | Out-Null

Write-Host 'Compiling MineCanon...'
& (Join-Path $jdk 'bin\javac.exe') --release 17 -d $classes `
    (Join-Path $launcher 'MineCanonLauncher.java') `
    (Join-Path $launcher 'MineCanonLauncherTest.java') `
    (Join-Path $launcher 'MineCanonIcon.java') `
    (Join-Path $launcher 'ForgeRuntimeCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'MineCanon compilation failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp $classes MineCanonLauncherTest
if ($LASTEXITCODE -ne 0) { throw 'MineCanon checks failed.' }
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp $classes MineCanonIcon $icon
if ($LASTEXITCODE -ne 0) { throw 'Icon creation failed.' }
$jar = Join-Path $inputDir 'MineCanon.jar'
& (Join-Path $jdk 'bin\jar.exe') --create --file $jar --main-class MineCanonLauncher -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }

# Forge uses Gson's Unsafe allocator and HTTPS providers absent from java.desktop alone.
$modules = @('java.se', 'jdk.unsupported', 'jdk.zipfs', 'jdk.charsets', 'jdk.localedata', 'jdk.crypto.cryptoki', 'jdk.accessibility')
if (Test-Path (Join-Path $jdk 'jmods\jdk.crypto.ec.jmod')) { $modules += 'jdk.crypto.ec' }
$runtime = Join-Path $build 'runtime-forge-v2'
if (-not (Test-Path $runtime)) {
    & (Join-Path $jdk 'bin\jlink.exe') --add-modules ($modules -join ',') --strip-debug `
        --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) { throw 'Runtime creation failed.' }
}
$forge = Join-Path $root 'mods-1.20.1-base\forge-1.20.1-47.4.26-installer.jar'
& (Join-Path $runtime 'bin\java.exe') '-Djava.awt.headless=true' -cp "$classes;$forge" ForgeRuntimeCheck
if ($LASTEXITCODE -ne 0) { throw 'Bundled runtime cannot load Forge. The installed app was not updated.' }

if (Test-Path $exe) {
    if (-not (Test-Path (Join-Path $app '.minecanon-generated'))) {
        throw 'The destination already exists but is not a MineCanon-generated app. It was not changed.'
    }
    Write-Host 'Updating the installed MineCanon app (close it first)...'
    Copy-Item -LiteralPath $jar -Destination (Join-Path $app 'app\MineCanon.jar') -Force
    $installedRuntime = Join-Path $app 'runtime'
    $backupRuntime = Join-Path $app ('runtime-backup-' + [Guid]::NewGuid().ToString('N'))
    Move-Item -LiteralPath $installedRuntime -Destination $backupRuntime
    try {
        Copy-Item -LiteralPath $runtime -Destination $installedRuntime -Recurse
    } catch {
        # Keep the backup intact for recovery if copying the replacement fails.
        throw "Runtime update failed. Original runtime preserved at $backupRuntime. $($_.Exception.Message)"
    }
    Remove-Item -LiteralPath $backupRuntime -Recurse -Force
} else {
    if (Test-Path $app) { throw 'An incomplete dist\MineCanon directory exists. Move it aside before retrying.' }
    Write-Host 'Bundling the Java runtime and Windows GUI executable...'
    & (Join-Path $jdk 'bin\jpackage.exe') --type app-image --name MineCanon `
        --dest $dist --input $inputDir --main-jar MineCanon.jar --main-class MineCanonLauncher `
        --runtime-image $runtime --icon $icon --vendor 'John Matukutire' `
        --description 'Mine Canon World desktop launcher' --app-version '1.0.0'
    if ($LASTEXITCODE -ne 0) { throw 'Native packaging failed.' }
    New-Item -ItemType File -Path (Join-Path $app '.minecanon-generated') | Out-Null
}

& (Join-Path $app 'runtime\bin\java.exe') '-Djava.awt.headless=true' -cp "$classes;$forge" ForgeRuntimeCheck
if ($LASTEXITCODE -ne 0) { throw 'Installed Forge runtime check failed.' }

$desktop = [Environment]::GetFolderPath('Desktop')
$shortcutPath = Join-Path $desktop 'MineCanon.lnk'
$shell = New-Object -ComObject WScript.Shell
if (Test-Path $shortcutPath) {
    $existing = $shell.CreateShortcut($shortcutPath)
    if ($existing.TargetPath -ne $exe) {
        throw 'A different MineCanon desktop shortcut already exists. It was not overwritten.'
    }
}
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $exe
$shortcut.WorkingDirectory = $root
$shortcut.IconLocation = "$exe,0"
$shortcut.Description = 'MineCanon - John Matukutire | Mine Canon World'
$shortcut.Save()
Write-Host "Desktop shortcut: $shortcutPath"
Write-Host "Native GUI: $exe"
