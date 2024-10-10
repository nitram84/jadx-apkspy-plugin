### JADX ApkSpy Plugin

This plugin for JADX adds support for editing Java source code of APK's, and then recompiling them. The JADX ApkSpy plugin is a port of the work of @LucasBaizer (https://github.com/LucasBaizer/apkSpy).

### Limitations

Currently, this plugin is in a proof of concept state. Some featured of the original fork like adding or deleting classes, i18n, ... are not yet ported.

#### Workarounds needed to recompile sources and to rebuild an APK

* It is necessary to set the "Code cache mode" to 'MEMORY' in preferences in order to recompile modified classes. Other cache modes are not yet supported
* Obfuscation and renaming features can not be used together with this plugin.
* Only methods are editable. Adding or deleting methods is not supported yet.
* Classes within the android.* packages cannot be edited.

### Building Jadx ApkSpy plugin from source

JDK 11 or higher must be installed.

This plugin has a compile dependency to "jadx-gui" which is not available as a library. Therefore, you have to build "jadx-gui" as a library:

```bash
git clone https://github.com/nitram84/jadx.git
cd jadx
git checkout jadx_gui_lib
JADX_VERSION="1.5.1-SNAPSHOT" ./gradlew publishToMavenLocal
```

Build the plugin:

```bash
git clone https://github.com/nitram84/jadx-apkspy-plugin.git
cd jadx-apkspy-plugin.
./gradlew shadowJar
```

(on Windows, use gradlew.bat instead of ./gradlew)

### How it works

JADX ApkSpy plugin allows recompiling individual methods of classes, so only small pieces have to be recompiled, regardless of whether the other parts of the application are compiling or not. The result can be exported as an APK. APKs exported by this plugin are unsigned and have to be signed before installing.

#### Prerequisites

* To compile modified classes a JDK 8 is required.
* Android SDK
* Apktool (https://github.com/iBotPeaches/Apktool)
* Locations for JDK, Apktool and Android SDK have to be configured in plugin options.

#### How to use

* Open an APK in JADX
* Right-click on the name of method to be edited to open the context menu and select "Edit method"
* After modifications for the method are done, compile and save changes
* Save APK in plugins menu

### Install

This plugin requires JADX in a version greater than 1.5.0. Use the latest unstable build of JADX or latest git version if possible.

Install using location id: `github:nitram84:jadx-apkspy-plugin`

In jadx-cli:
```bash
  jadx plugins --install "github:nitram84:jadx-apkspy-plugin"
```
