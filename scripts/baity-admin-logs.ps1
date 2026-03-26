param(
  [string]$BaseUrl = "https://baity-presence-sync.1427637445.workers.dev"
)

$ErrorActionPreference = "Stop"

$outDir = $PSScriptRoot

function Invoke-AdminVerify($AdminToken) {
  $headers = @{ "x-baity-admin-token" = $AdminToken }
  $resp = Invoke-WebRequest -UseBasicParsing -Method GET -Uri ($BaseUrl.TrimEnd("/") + "/admin/verify") -Headers $headers -TimeoutSec 20
  return ($resp.Content | ConvertFrom-Json)
}

function Invoke-PopLog($AdminToken, $kind) {
  $headers = @{ "x-baity-admin-token" = $AdminToken }
  $endpoint = switch ($kind) {
    "reads" { "/admin/reads-log" }
    "writes" { "/admin/writes-log" }
    default { throw "unknown kind: $kind" }
  }

  $resp = Invoke-WebRequest -UseBasicParsing -Method POST -Uri ($BaseUrl.TrimEnd("/") + $endpoint) -Headers $headers -TimeoutSec 20
  return ($resp.Content | ConvertFrom-Json)
}

function Save-Logs($logs, $kind) {
  $ts = (Get-Date).ToString("yyyyMMdd-HHmmss")
  $path = Join-Path $outDir ("baity-" + $kind + "-log-" + $ts + ".json")
  $logsJson = $logs | ConvertTo-Json -Depth 10
  Set-Content -Path $path -Value $logsJson -Encoding UTF8
  return $path
}

while ($true) {
  Write-Host ""
  Write-Host "Baity Remote Sync Admin Log Tool"
  Write-Host "1) Print & clear remote READ logs"
  Write-Host "2) Print & clear remote WRITE logs"
  Write-Host "3) Exit"
  Write-Host "4) Clear ALL remote sync data (tokens + logs + throttles)"
  $choice = Read-Host "Select (1/2/3/4)"

  if ($choice -eq "3") {
    break
  }

  if ($choice -eq "4") {
    $confirm = Read-Host "This will DELETE all remote sync data. Continue? (y/n)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
      Write-Host "Cancelled."
      continue
    }

    $adminToken = Read-Host "Admin token (x-baity-admin-token)"
    if ([string]::IsNullOrWhiteSpace($adminToken)) {
      Write-Host "Empty token, abort."
      continue
    }

    try {
      $verify = Invoke-AdminVerify $adminToken
      if (-not $verify.ok) {
        Write-Host "Invalid admin token."
        continue
      }

      $headers = @{ "x-baity-admin-token" = $adminToken }
      $resp = Invoke-WebRequest -UseBasicParsing -Method POST -Uri ($BaseUrl.TrimEnd("/") + "/admin/clear-all") -Headers $headers -TimeoutSec 60
      $result = $resp.Content | ConvertFrom-Json
      if ($result.ok -ne $true) {
        Write-Host "Clear-all failed: " ($result | ConvertTo-Json -Depth 5)
        continue
      }

      Write-Host "Clear-all OK."
      Write-Host ("usersTokens: " + $result.counts.usersTokens)
      Write-Host ("reads: " + $result.counts.reads)
      Write-Host ("writes: " + $result.counts.writes)
      Write-Host ("registers: " + $result.counts.registers)
    } catch {
      Write-Host "Request failed: " $_.Exception.Message
      continue
    }

    continue
  }

  $adminToken = Read-Host "Admin token (x-baity-admin-token)"
  if ([string]::IsNullOrWhiteSpace($adminToken)) {
    Write-Host "Empty token, abort."
    continue
  }

  try {
    $verify = Invoke-AdminVerify $adminToken
    if (-not $verify.ok) {
      Write-Host "Invalid admin token."
      continue
    }
  } catch {
    Write-Host "Admin verify failed: " $_.Exception.Message
    continue
  }

  try {
    if ($choice -eq "1") {
      $result = Invoke-PopLog $adminToken "reads"
      $logs = $result.logs
      Write-Host ("Fetched READ logs count: " + ($logs.Count))
      $file = Save-Logs $logs "reads"
      Write-Host ("Saved: " + $file)
      # Print a compact view to console
      $logs | Select-Object -Last 10 | ForEach-Object {
        Write-Host ("- " + $_.uuid + " @ " + $_.readAt)
      }
    } elseif ($choice -eq "2") {
      $result = Invoke-PopLog $adminToken "writes"
      $logs = $result.logs
      Write-Host ("Fetched WRITE logs count: " + ($logs.Count))
      $file = Save-Logs $logs "writes"
      Write-Host ("Saved: " + $file)
      $logs | Select-Object -Last 10 | ForEach-Object {
        Write-Host ("- " + $_.uuid + " @ " + $_.writeAt)
      }
    } else {
      Write-Host "Unknown choice."
      continue
    }
  } catch {
    Write-Host "Request failed: " $_.Exception.Message
    continue
  }
}

