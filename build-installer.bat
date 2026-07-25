@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem RMC Framework - Windows installer (.exe) build script
rem ============================================================
rem
rem REQUIREMENTS on the machine running this script:
rem
rem   1. JDK 21+ WITH JAVAFX BUNDLED, including jpackage.
rem      Recommended: BellSoft Liberica Full JDK 21
rem        https://bell-sw.com/pages/downloads/
rem      or Azul Zulu 21 Bundled JavaFX (FX) build
rem        https://www.azul.com/downloads/
rem
rem      IMPORTANT: a regular (non-Full/FX) OpenJDK will also build an
rem      installer, but jpackage for classpath-based apps (our case - a
rem      thin jar plus dependencies in libs/, not a full JPMS module)
rem      usually cannot safely trim the runtime with jlink and instead
rem      bundles the ENTIRE JDK it was launched from. If that JDK does not
rem      include JavaFX native libraries, the resulting installer may fail
rem      to start the app on the user's machine. Hence the requirement for
rem      a JDK that already includes JavaFX.
rem
rem   2. Maven in PATH.
rem
rem   3. WiX Toolset v3 (candle.exe and light.exe in PATH) - jpackage on
rem      Windows uses it to build .exe/.msi.
rem      Download: https://github.com/wixtoolset/wix3/releases
rem      If WiX is not available, you can still build a plain folder with
rem      the app (no installer) by changing --type exe to --type app-image
rem      in the jpackage command below.
rem
rem ============================================================

set APP_NAME=RMC Framework
set VENDOR=RMC
set MAIN_CLASS=com.rmc.Launcher
set ICON=src\main\resources\images\app-icon.ico

echo.
echo [1/5] Checking jpackage...
where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: jpackage not found in PATH.
    echo A JDK 21+ with JavaFX is required - see the comment block at the top of this file.
    exit /b 1
)

echo [2/5] Building the project - mvn clean package ...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed.
    exit /b 1
)

echo [3/5] Locating the built jar file...
set MAIN_JAR=
for %%f in (target\rmc-framework-*.jar) do set MAIN_JAR=%%~nxf

if "%MAIN_JAR%"=="" (
    echo ERROR: target\rmc-framework-*.jar not found - check that the Maven build succeeded.
    exit /b 1
)

rem Extract the version straight from the jar file name
rem (rmc-framework-0.1.0.jar -> 0.1.0), so it never drifts out of sync
rem with pom.xml.
set APP_VERSION=%MAIN_JAR:rmc-framework-=%
set APP_VERSION=%APP_VERSION:.jar=%

echo     Found: %MAIN_JAR% - version %APP_VERSION%

echo [4/5] Preparing a clean input folder for jpackage...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy /y "target\%MAIN_JAR%" "target\jpackage-input\" >nul
xcopy /y /i /e "target\libs" "target\jpackage-input\libs\" >nul

if not exist "%ICON%" (
    echo WARNING: icon file %ICON% not found - the installer will use a default icon.
    set ICON_ARG=
) else (
    set ICON_ARG=--icon "%ICON%"
)

echo [5/5] Building the installer - jpackage ...
if exist target\installer rmdir /s /q target\installer

jpackage ^
    --type exe ^
    --input target\jpackage-input ^
    --main-jar "%MAIN_JAR%" ^
    --main-class %MAIN_CLASS% ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --vendor "%VENDOR%" ^
    %ICON_ARG% ^
    --dest target\installer ^
    --win-shortcut ^
    --win-menu ^
    --win-dir-chooser ^
    --win-per-user-install

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed.
    echo If this is a WiX Toolset issue - not installed or not in PATH - you can
    echo build a plain app folder without an installer instead by replacing
    echo --type exe with --type app-image in the jpackage command above.
    exit /b 1
)

echo.
echo ============================================================
echo Done! Installer:
echo   target\installer\%APP_NAME%-%APP_VERSION%.exe
echo ============================================================
echo.

endlocal
