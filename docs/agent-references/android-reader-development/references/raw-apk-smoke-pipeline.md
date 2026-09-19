# Raw APK Smoke-Test Pipeline (Gradle-free, verified 2026-08-25)

Use when you need to prove the Android toolchain works end-to-end
(build → dex → package → sign → install → launch) without waiting on a full
Gradle/AGP project or downloading dependencies. Validated on the mini PC SDK
against a Pixel 7 / GrapheneOS (Android 17).

## Environment (mini PC)

- `ANDROID_HOME=~/.local/android-sdk` (exported in ~/.profile)
  - platform android-36, build-tools 36.0.0, platform-tools 37.0.1
- Gradle 9.7.1 at `~/.local/opt/gradle-9.7.1`, OpenJDK 21
- Device: Pixel 7 "panther", serial 28261FDH200F50, GrapheneOS

## Pipeline

```bash
SDK=$HOME/.local/android-sdk; BT=$SDK/build-tools/36.0.0; cd $W
$BT/aapt2 compile --dir res -o res.zip
$BT/aapt2 link -o base.apk -I $SDK/platforms/android-36/android.jar \
    --manifest AndroidManifest.xml res.zip --java src_gen
javac --release 11 -cp $SDK/platforms/android-36/android.jar -d obj src/**/*.java
$BT/d8 --lib $SDK/platforms/android-36/android.jar --release --output out obj/**/*.class
cd out && zip -q ../base.apk classes.dex && cd ..
keytool -genkeypair -keystore ks.jks -alias k -storepass PW -dname CN=smoke ...
$BT/zipalign -f 4 base.apk aligned.apk
$BT/apksigner sign --ks ks.jks --ks-pass pass:PW --out signed.apk aligned.apk
adb install -r signed.apk && adb shell am start -n pkg/.Main
```

Verify launch via logcat: `adb shell logcat -d | grep '<pkg>' | tail`
should show an ActivityTaskManager "Displayed" line.

## Pitfall: INSTALL_FAILED_DEPRECATED_SDK_VERSION

An aapt2-linked manifest with NO `<uses-sdk>` element targets SDK 0 and modern
Android (16+) refuses install:

```
INSTALL_FAILED_DEPRECATED_SDK_VERSION: App package must target at least
SDK version 24, but found 0
```

Fix: add `<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>`
inside `<manifest>`. Gradle/AGP injects this from build.gradle automatically —
this only bites hand-written manifests (raw aapt2 pipelines).

## Manifest template (minimal launchable activity)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.smoke" android:versionCode="1" android:versionName="1.0">
  <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>
  <application>
    <activity android:name=".Main" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>
  </application>
</manifest>
```

Note: sed-injecting attributes into the manifest element is fragile (easy to
produce malformed XML); rewrite the whole file instead.
