@echo off
setlocal enabledelayedexpansion

REM === CONFIGURATION ===
set "JAVAFX_HOME=C:\javafx-sdk-21.0.9\lib"
set "CP=lib/*;src"

REM === COMPILE ===
echo Collecting source files...
del sources.txt 2>nul
for /r src %%f in (*.java) do (
    echo %%f>>sources.txt
)
if not exist out mkdir out

echo Compiling sources...
javac -cp "%CP%" -d out @sources.txt
if %errorlevel% neq 0 (
    echo ❌ Compilation failed.
    exit /b 1
)
echo ✅ Compilation successful.

REM === RUN ===
echo Running application...
java --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml -cp "out;lib/*" ui.Main

endlocal
