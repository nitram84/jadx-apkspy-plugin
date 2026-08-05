[![](https://jitpack.io/v/nitram84/jadx-apkspy-plugin.svg)](https://jitpack.io/#nitram84/jadx-apkspy-plugin)
![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

### JADX ApkSpy Plugin

This plugin for JADX adds support for editing Java source code of APK's, and then recompiling them. The JADX ApkSpy plugin is a port of the work of @LucasBaizer (https://github.com/LucasBaizer/apkSpy).

### Features

* Edit methods in JADX
* Add new classes
* Delete classes
* Deobfuscate or rename classes, methods and fields
* Export changes as modified APK

### Limitations

Currently, this plugin is in a proof of concept state.

#### Workarounds needed to recompile sources and to rebuild an APK

* **Disable option "Inline Methods" in preferences** to prevent smali merging issues on saving the modified apk.
* It is necessary to **set "Code cache mode" to 'MEMORY' in preferences** in order to recompile modified classes. Other cache modes are not yet supported.
* Only methods are editable. Deleting methods is not supported yet.
* Adding classes is only possible when "Code cache mode" is set to 'MEMORY' in preferences.
* Changes to source code have to be saved externally - this plugin saves changes as modified APK.
* Editing methods in anonymous classes is not yet supported.

##### Renaming and Deobfuscation

* Toggling deobfuscation resets all modifications without a warning. Make sure you saved your modified apk before toggling deobfuscation.
* Renaming in edited code sections in not supported. Rename classes first, before editing any classes. If you want to rename a class while having edited a class before, save your modified apk, open your modified apk and continue renaming.

### Building Jadx ApkSpy plugin from source

JDK 17 or higher must be installed.

Build the plugin:

```bash
git clone https://github.com/nitram84/jadx-apkspy-plugin.git
cd jadx-apkspy-plugin.
./gradlew shadowJar
# Install plugin
cp build/dist/jadx-apkspy-plugin-dev.jar ~/.config/jadx/plugins/dropins/
```

(on Windows, use gradlew.bat instead of ./gradlew)

### How it works

JADX ApkSpy plugin allows recompiling individual methods of classes, so only small pieces have to be recompiled, regardless of whether the other parts of the application are compiling or not. The result can be exported as an APK. APKs exported by this plugin are unsigned and have to be signed before installing.

#### Prerequisites

* To compile modified classes a JDK 17 or greater is required.
* Android SDK
* Locations for JDK and Android SDK have to be configured in plugin options.

#### How to use

* Open an APK in JADX
* Right-click on the name of method to be edited to open the context menu and select "Edit method"
* After modifications for the method are done, compile and save changes
* Save APK in plugins menu
* Modifications can be reset with "Reload files".

### Install

This plugin requires JADX in a version 1.5.6 or greater. Use the latest unstable build of JADX or latest git version if possible.

#### Install with JitPack:

Download the precompiled plugin from https://jitpack.io/com/github/nitram84/jadx-apkspy-plugin/main-SNAPSHOT/jadx-apkspy-plugin-main-SNAPSHOT.jar and save it to your JADX plugin dropins folder:

```bash
wget https://jitpack.io/com/github/nitram84/jadx-apkspy-plugin/main-SNAPSHOT/jadx-apkspy-plugin-main-SNAPSHOT.jar -o ~/.config/jadx/plugins/dropins/jadx-apkspy-plugin-main-SNAPSHOT.jar
```

Use the same command to update the plugin.
