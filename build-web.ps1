# Builds the web version into docs/ (static files, servable by GitHub Pages or any static host).
# Usage:  powershell -ExecutionPolicy Bypass -File build-web.ps1 [-Serve]
param([switch]$Serve, [int]$Port = 8000)
# Native tools write progress to stderr, so failures are detected via $LASTEXITCODE instead of "Stop".
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot

$teavmVersion = "0.12.0"
$toolsDir = "tools\teavm"
$outDir = "docs"
$classesDir = "build\web-classes"

$deps = @(
  "org.teavm:teavm-cli:$teavmVersion",
  "org.teavm:teavm-tooling:$teavmVersion",
  "org.teavm:teavm-core:$teavmVersion",
  "org.teavm:teavm-classlib:$teavmVersion",
  "org.teavm:teavm-platform:$teavmVersion",
  "org.teavm:teavm-interop:$teavmVersion",
  "org.teavm:teavm-jso:$teavmVersion",
  "org.teavm:teavm-jso-apis:$teavmVersion",
  "org.teavm:teavm-jso-impl:$teavmVersion",
  "org.teavm:teavm-metaprogramming-api:$teavmVersion",
  "org.teavm:teavm-metaprogramming-impl:$teavmVersion",
  "org.teavm:teavm-relocated-libs-asm:$teavmVersion",
  "org.teavm:teavm-relocated-libs-asm-analysis:$teavmVersion",
  "org.teavm:teavm-relocated-libs-asm-commons:$teavmVersion",
  "org.teavm:teavm-relocated-libs-asm-tree:$teavmVersion",
  "org.teavm:teavm-relocated-libs-asm-util:$teavmVersion",
  "org.teavm:teavm-relocated-libs-commons-cli:$teavmVersion",
  "org.teavm:teavm-relocated-libs-commons-io:$teavmVersion",
  "org.teavm:teavm-relocated-libs-hppc:$teavmVersion",
  "org.teavm:teavm-relocated-libs-rhino:$teavmVersion",
  "org.ow2.asm:asm:9.7.1",
  "org.ow2.asm:asm-analysis:9.7.1",
  "org.ow2.asm:asm-commons:9.7.1",
  "org.ow2.asm:asm-tree:9.7.1",
  "org.ow2.asm:asm-util:9.7.1",
  "com.carrotsearch:hppc:0.10.0",
  "commons-cli:commons-cli:1.9.0",
  "commons-io:commons-io:2.18.0",
  "org.mozilla:rhino:1.7.15",
  "joda-time:joda-time:2.12.2",
  "com.jcraft:jzlib:1.1.3"
)

New-Item -ItemType Directory -Force $toolsDir | Out-Null
foreach ($dep in $deps) {
  $g, $a, $v = $dep.Split(":")
  $jar = Join-Path $toolsDir "$a.jar"
  if (-not (Test-Path $jar)) {
    Write-Host "Downloading $dep"
    Invoke-WebRequest "https://repo1.maven.org/maven2/$($g.Replace('.', '/'))/$a/$v/$a-$v.jar" -OutFile $jar
  }
}
$toolsCp = (Get-ChildItem "$toolsDir\*.jar" | ForEach-Object FullName) -join ";"
$jsoCp = (@("teavm-jso", "teavm-jso-apis", "teavm-interop") | ForEach-Object { Join-Path $toolsDir "$_.jar" }) -join ";"

Write-Host "Compiling Java..."
if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir }
$sources = Get-ChildItem -Recurse src, web\src -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -cp $jsoCp -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "Translating to JavaScript with TeaVM..."
if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
# The main class must come first: -p accepts multiple values and would swallow it.
java -cp $toolsCp org.teavm.cli.TeaVMRunner web.WebMain -t js -d $outDir -f classes.js --js-module-type none -m -O 2 -p $classesDir
if ($LASTEXITCODE -ne 0) { throw "TeaVM failed" }

Write-Host "Assembling site..."
Copy-Item -Recurse web\static\* $outDir
New-Item -ItemType Directory -Force "$outDir\res" | Out-Null
Copy-Item res\thunder.obj "$outDir\res\"
Copy-Item -Recurse res\gems "$outDir\res\"
New-Item -ItemType File -Force "$outDir\.nojekyll" | Out-Null
$buildId = Get-Date -Format "yyyyMMddHHmmss"
(Get-Content "$outDir\sw.js" -Raw).Replace("BUILD_ID", $buildId) | Set-Content "$outDir\sw.js" -NoNewline
Write-Host "Built $outDir (build $buildId)"

if ($Serve) {
  $ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notmatch "Loopback|vEthernet" -and $_.IPAddress -notmatch "^169\." } | Select-Object -First 1).IPAddress
  Write-Host "Serving on http://localhost:$Port  (phone on same Wi-Fi: http://${ip}:$Port)"
  jwebserver -d (Resolve-Path $outDir) -b 0.0.0.0 -p $Port
}
