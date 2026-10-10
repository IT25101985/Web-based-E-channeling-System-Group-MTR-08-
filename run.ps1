param([ValidateSet('sqlserver','demo')][string]$Profile = 'sqlserver', [switch]$Build)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$artifact = Join-Path $PSScriptRoot 'target/echanneling-system-0.0.1-SNAPSHOT.jar'
if ($Build -or !(Test-Path -LiteralPath $artifact)) {
    $env:MAVEN_OPTS = '-Xmx512m -Xms32m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dnet.bytebuddy.experimental=true'
    & "$PSScriptRoot/mvnw.cmd" package -DforkCount=0 "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository"
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed. Review the output before running.' }
}
Write-Host "Starting CareLink ($Profile). Open http://localhost:8080. Press Ctrl+C to stop."
& java -Xmx256m -Xms32m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=32m -jar $artifact "--spring.profiles.active=$Profile"
exit $LASTEXITCODE
