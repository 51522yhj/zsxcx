$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupDir = "$PSScriptRoot\..\db-backups"
if (!(Test-Path $backupDir)) {
  New-Item -ItemType Directory -Path $backupDir | Out-Null
}

$outFile = "$backupDir\xiaoyu_yinran-$timestamp.sql"
$container = "xiaoyu-yinran-mysql"
$password = $env:LOCAL_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) {
  throw "Set LOCAL_DB_PASSWORD before running this script."
}

Write-Host "Backing up xiaoyu_yinran from container $container ..."
docker exec -e MYSQL_PWD=$password $container mysqldump -u root xiaoyu_yinran --routines --triggers --single-transaction > $outFile

Write-Host "Done: $outFile"
