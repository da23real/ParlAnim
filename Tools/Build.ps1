param([string]$Sdk=$env:ANDROID_HOME,[string]$Jdk=$env:JAVA_HOME,[string]$KeyStore=$env:PARLANIME_KEYSTORE)
if(!$Sdk -or !$Jdk -or !$KeyStore){throw 'Définir ANDROID_HOME, JAVA_HOME et PARLANIME_KEYSTORE.'}
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$build=Join-Path $root 'Build'
$src=Join-Path $root 'Sources'
$bt=Join-Path $Sdk 'build-tools/35.0.0'
New-Item -ItemType Directory -Force "$root/Android","$build/classes","$build/dex" | Out-Null
function Run([string]$exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "Échec $exe : $LASTEXITCODE"} }
Copy-Item "$Sdk/platforms/android-35/android.jar" "$build/android.jar" -Force
$jar="$build/android.jar"
$sources=@(Get-ChildItem "$src/java" -Filter '*.java' -Recurse | ForEach-Object FullName)
Run "$Jdk/bin/javac.exe" (@('-g','-encoding','UTF-8','-source','8','-target','8','-classpath',$jar,'-d',"$build/classes")+$sources)
Run "$bt/aapt2.exe" @('compile','--dir',"$src/res",'-o',"$build/resources.zip")
Run "$bt/aapt2.exe" @('link','-I',$jar,'--manifest',"$src/AndroidManifest.xml",'--min-sdk-version','26','--target-sdk-version','35','--version-code','10001605','--version-name','VA01.16-R5','-A',"$src/assets",'-o',"$build/unsigned.apk","$build/resources.zip")
$classes=@(Get-ChildItem "$build/classes" -Filter '*.class' -Recurse | ForEach-Object FullName)
Run "$Jdk/bin/java.exe" (@('-cp',"$bt/lib/d8.jar",'com.android.tools.r8.D8','--debug','--lib',$jar,'--min-api','26','--output',"$build/dex")+$classes)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{
 foreach($entry in @($zip.Entries | Where-Object {$_.FullName.Contains('\')})) {
  $fixed=$zip.CreateEntry($entry.FullName.Replace('\','/'))
  $inputStream=$entry.Open(); $outputStream=$fixed.Open()
  try{$inputStream.CopyTo($outputStream)}finally{$inputStream.Dispose();$outputStream.Dispose()}
  $entry.Delete()
 }
 [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build/dex/classes.dex",'classes.dex')|Out-Null}finally{$zip.Dispose()}
Run "$bt/zipalign.exe" @('-f','-p','4',"$build/unsigned.apk","$build/aligned.apk")
$key=$KeyStore
if(!(Test-Path -LiteralPath $key)){throw 'Clé de signature absente. Utiliser votre propre clé privée.'}
$apk="$root/Android/ParlAnime-VA01.16-R5.apk"
Run "$Jdk/bin/java.exe" @('-jar',"$bt/lib/apksigner.jar",'sign','--ks',$key,'--ks-key-alias',$env:PARLANIME_KEY_ALIAS,'--ks-pass','env:PARLANIME_KEY_PASSWORD','--key-pass','env:PARLANIME_KEY_PASSWORD','--out',$apk,"$build/aligned.apk")
Run "$Jdk/bin/java.exe" @('-jar',"$bt/lib/apksigner.jar",'verify','--verbose',$apk)
Get-FileHash $apk -Algorithm SHA256




