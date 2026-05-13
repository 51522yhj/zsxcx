$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\admin"
if (!(Test-Path "node_modules")) {
  npm install
}
npm run dev

