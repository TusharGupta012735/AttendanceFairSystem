@echo off
echo Building Attendance Fair System...

REM Clean previous builds
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist

REM Create directories
mkdir build
mkdir dist

REM Compile
echo Compiling Java files...
javac -d build -cp "lib/*" src/**/*.java

REM Create JAR
echo Creating JAR file...
cd build
jar cvfe ../dist/AttendanceFairSystem.jar ui.Main .
cd ..

REM Package as EXE
echo Creating executable...
jpackage --input dist ^
         --main-jar AttendanceFairSystem.jar ^
         --main-class ui.Main ^
         --name AttendanceFairSystem ^
         --app-version 1.0.0 ^
         --dest dist ^
         --win-dir-chooser ^
         --win-menu ^
         --win-shortcut ^
         --description "Attendance Fair System with NFC Card Support" ^
         --vendor "Your Organization Name" ^
         --win-console

echo Build complete! Check the dist folder for the installer.