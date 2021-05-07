#
# Copyright (c) 2020, 2021 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Before you execute the script you need to grant system permissions as admin running the next command
# powershell Set-ExecutionPolicy RemoteSigned
param([parameter(ValueFromRemainingArguments=$true)]
    [string[]]$arguments)
$global:action="Invoke-Expression"
$global:command=$null

function usage {
    Write-Host
    Write-Host "Start <JAR_NAME> from this runtime image."
    Write-Host
    Write-Host "Usage: ${scriptName}.ps1 <options> [arg]..."
    Write-Host
    Write-Host "Options:"
    Write-Host
    Write-Host "    --jvm <option>  Add one or more JVM options, replacing defaults."
    Write-Host "    --noCds         Do not use CDS."
    Write-Host "    --debug         Add JVM debug options."
    Write-Host "    --test          Exit when started."
    Write-Host "    --dryRun        Display the command rather than executing it."
    Write-Host "    --help          Display usage."
    Write-Host
    Write-Host "Unrecognized options are passed as arguments to <JAR_NAME>, replacing defaults."
    Write-Host
    Write-Host "Supported environment variables:"
    Write-Host
    Write-Host "    DEFAULT_APP_JVM     <DEFAULT_APP_JVM_DESC>"
    Write-Host "    DEFAULT_APP_ARGS    <DEFAULT_APP_ARGS_DESC>"
    Write-Host "    DEFAULT_APP_DEBUG   <DEFAULT_APP_DEBUG_DESC>"
    Write-Host
    exit 0
}

function init {
    param ( [String]$script )
    $scriptName = (Get-Item $script).Basename
    $binDir = (Get-Item $script).DirectoryName
    $homeDir=(Get-Item $binDir).parent.FullName
    $jarName="<JAR_NAME>"
    $defaultDebug="<DEFAULT_APP_DEBUG>"
    $defaultJvm="<DEFAULT_APP_JVM>"
    $defaultArgs="<DEFAULT_APP_ARGS>"
    $cdsOption="<CDS_UNLOCK>-XX:SharedArchiveFile=$homeDir\lib\start.jsa -Xshare:"
    $exitOption="`"-Dexit.on.started=<EXIT_ON_STARTED>`""
    $debugDefaults = if (Test-Path env:DEFAULT_APP_DEBUG) { $env:DEFAULT_APP_DEBUG } else { $defaultDebug }
    $jvmDefaults = if (Test-Path env:DEFAULT_APP_JVM) { $env:DEFAULT_APP_JVM } else { $defaultJvm }
    $argDefaults = if (Test-Path env:DEFAULT_APP_ARGS) { $env:DEFAULT_APP_ARGS } else { $defaultArgs }
    $args
    $jvm
    $test=$false
    $share="auto"
    $useCds=$true
    $debug=$false
    $i=0
    while ($i -lt $arguments.Length) {
        switch ($($arguments[$i])) {
           "--jvm"  {$jvm = appendVar $jvm $($arguments[++$i]); break}
           "--noCds"   {$useCds=$false; break}
           "--debug" {$debug=$true; break}
           "--test"  {$test=$true; $share="on"; break}
           "--dryRun" {$global:action="Write-Host"; break}
           "-h" {usage; break}
           "--help" {usage; break}
           default {$args = appendVar $args $($arguments[$i]); break}
        }
        $i++
    }
    $jvmOptions = if ($jvm) { $jvm } else { $jvmDefaults }
    if (${useCds}) { setupCds }
    if (${debug}) { $jvmOptions = appendVar "$jvmOptions" "$debugDefaults" }
    if ($test) {
    	$jvmOptions = appendVar "$jvmOptions" "$exitOption"
    	if ($useCds) { checkTimeStamps }
    }
    $commandArgs = if ($args) { $args } else { $argDefaults }
    $global:command="$homeDir\bin\java.exe $jvmOptions -jar $homeDir\app\$jarName $commandArgs"
}

function appendVar {
    param ( [String]$var,
            [String]$value)
    return $("$var $value")
}

function checkTimeStamps {
    $modulesTimeStamp = getLastWriteTotalSeconds ${homeDir}/lib/modules
    $jarTimeStamp = getLastWriteTotalSeconds ${homeDir}/app/$jarName
    if ( (${modulesTimeStamp} -ne "<MODULES_TIME_STAMP>") -Or (${jarTimeStamp} -ne "<JAR_TIME_STAMP>") ) {
        Write-Host "WARNING: CDS will likely fail since it appears this image is a copy (timestamps differ)."
        Write-Host "         <COPY_INSTRUCTIONS>"
    }
}

function setupCds {
    $jvmOptions = appendVar "$jvmOptions" "${cdsOption}${share}"
    Set-Location -Path "$homeDir"
    $pathPrefix=""
}

function getLastWriteTotalSeconds {
    param ( [String]$file)
    $statFormat="<STAT_FORMAT>"
    $lastModified = (Get-ItemProperty -Path $file -Name LastWriteTime).lastwritetime
    $seconds = (Get-Date -Date $lastModified -UFormat $statFormat).split('.')[0].split(',')[0]
    $secondsTimeZone = ([int](Get-Date -Date $lastModified -UFormat %Z)) *60 *60
    $lastWriteTotalSeconds = ($seconds - $secondsTimeZone)
    return $lastWriteTotalSeconds
}

function main {
    $script = $PSCommandPath
    #Write-Host "Script: $script"
    init $script
    #Write-Host "$global:action $global:command"
    & "$global:action" "$global:command"
}

main

