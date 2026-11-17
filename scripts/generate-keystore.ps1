# Generates a production release keystore for the Video Editor Android app
param(
    [string]$KeystorePath = "videoeditor-release.keystore",
    [string]$Alias = "videoeditor",
    [string]$Password = ""
)

if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = Read-Host -Prompt "Enter a secure password for your release keystore" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password)
    $Password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

Write-Host "Generating Android release keystore: $KeystorePath..."
keytool -genkeypair -v `
  -keystore $KeystorePath `
  -alias $Alias `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storepass $Password `
  -keypass $Password `
  -dname "CN=Video Editor, O=mosamircs, C=US"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Production keystore created at: $KeystorePath"
    Write-Host "Alias: $Alias"
    Write-Host "IMPORTANT: Keep this file and your password safe! You will need it for all Google Play Store updates."
    
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
    Write-Host "`nTo use with GitHub Actions CI, add this secret:"
    Write-Host "ANDROID_KEYSTORE = (Base64 of $KeystorePath)"
    Write-Host "ANDROID_KEYSTORE_PASSWORD = $Password"
    Write-Host "ANDROID_KEY_ALIAS = $Alias"
} else {
    Write-Host "[ERROR] Failed to generate keystore. Make sure 'keytool' (JDK) is on your PATH."
}
