param(
	[string]$InstallDir = "",
	[switch]$SkipJdk,
	[switch]$SkipGradle
)

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($InstallDir)) {
	$InstallDir = Join-Path $repoRoot ".test-env"
}

$jdkDir = Join-Path $InstallDir "jdk21"
$gradleUserHome = Join-Path $InstallDir "gradle"
$tempDir = Join-Path $InstallDir "downloads"
New-Item -ItemType Directory -Force -Path $jdkDir, $gradleUserHome, $tempDir | Out-Null

function Get-InstalledJava21 {
	$candidates = @()
	if ($env:JAVA_HOME) {
		$candidates += (Join-Path $env:JAVA_HOME "bin\java.exe")
	}
	$candidates += "java"

	foreach ($candidate in $candidates) {
		try {
			if ($candidate -ne "java") {
				if (-not (Test-Path $candidate)) {
					continue
				}
			}

			$output = & $candidate -version 2>&1
			if ($LASTEXITCODE -ne 0) {
				continue
			}

			$match = [regex]::Match(($output -join "`n"), 'version "?([0-9]+)')
			if ($match.Success -and [int]$match.Groups[1].Value -ge 21) {
				if ($candidate -eq "java") {
					return (Get-Command java.exe -ErrorAction Stop).Source
				}
				return $candidate
			}
		} catch {
		}
	}
	return $null
}

$javaExe = $null
if (-not $SkipJdk) {
	$downloadedJava = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
		Where-Object { $_.FullName -like "*\bin\java.exe" } |
		Select-Object -First 1

	if ($downloadedJava) {
		$javaExe = $downloadedJava.FullName
	} else {
		$javaExe = Get-InstalledJava21
	}
}

if (-not $javaExe -and -not $SkipJdk) {
	$jdkZip = Join-Path $tempDir "microsoft-jdk-21.zip"
	$jdkUrls = @(
		"https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip",
		"https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
	)

	$downloaded = $false
	foreach ($url in $jdkUrls) {
		Write-Host "Downloading JDK 21 from $url"
		try {
			[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
			Invoke-WebRequest -Uri $url -OutFile $jdkZip -UseBasicParsing
			if (Test-Path $jdkZip) {
				$downloaded = $true
				break
			}
		} catch {
			Write-Warning "JDK download failed: $($_.Exception.Message)"
		}
	}

	if (-not $downloaded) {
		throw "Could not download JDK 21."
	}

	$extractDir = Join-Path $tempDir "jdk-extract"
	if (Test-Path $extractDir) {
		Remove-Item -Recurse -Force $extractDir
	}
	Expand-Archive -Path $jdkZip -DestinationPath $extractDir -Force

	$root = Get-ChildItem -Path $extractDir -Directory |
		Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } |
		Select-Object -First 1
	if (-not $root) {
		$root = Get-Item $extractDir
	}

	Remove-Item -Recurse -Force $jdkDir
	Move-Item -Path $root.FullName -Destination $jdkDir
	$javaExe = Join-Path $jdkDir "bin\java.exe"
}

if ($javaExe) {
	$javaHome = Split-Path -Parent (Split-Path -Parent $javaExe)
	$env:JAVA_HOME = $javaHome
	$env:PATH = "$javaHome\bin;$env:PATH"
	$env:GRADLE_USER_HOME = $gradleUserHome
	& $javaExe -version
}

$initScript = Join-Path $InstallDir "init.gradle"
$initScriptContent = @'
beforeSettings { settings ->
	settings.pluginManagement.repositories {
		maven { url = uri("https://maven.fabricmc.net/") }
		maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		gradlePluginPortal()
	}
}

allprojects {
	repositories {
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		mavenCentral()
	}
}
'@
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($initScript, $initScriptContent.Replace("`r`n", "`n"), $utf8NoBom)

if (-not $SkipGradle) {
	if (-not $javaExe) {
		throw "JDK 21 is required to verify Gradle."
	}
	Push-Location $repoRoot
	try {
		& .\gradlew.bat --no-daemon --version
		if ($LASTEXITCODE -ne 0) {
			throw "Gradle Wrapper verification failed."
		}
	} finally {
		Pop-Location
	}
}

Write-Host ""
Write-Host "Local test environment is ready."
Write-Host "InstallDir: $InstallDir"
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME: $env:GRADLE_USER_HOME"
Write-Host ""
Write-Host "Compile manually:"
Write-Host "  `$env:JAVA_HOME = '$env:JAVA_HOME'"
Write-Host "  `$env:GRADLE_USER_HOME = '$gradleUserHome'"
Write-Host "  cd '$repoRoot'"
Write-Host "  .\gradlew.bat --no-daemon -I '$initScript' compileJava"
