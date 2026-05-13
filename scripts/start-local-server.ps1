$ErrorActionPreference = "Stop"

$jdk17 = "C:\Users\LENOVO\.jdks\ms-17.0.18"
if (Test-Path $jdk17) {
  $env:JAVA_HOME = $jdk17
  $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

Set-Location "$PSScriptRoot\..\server"
Write-Host "Starting local backend. Config file: application-local.yml"
mvn spring-boot:run
