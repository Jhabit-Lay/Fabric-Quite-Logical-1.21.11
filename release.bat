@echo off
setlocal enabledelayedexpansion

:: 1. Set UTF-8 encoding to prevent broken characters (Optional but recommended)
chcp 65001 >nul

echo ======================================================
echo    Fabric Mod Release System [MC 1.21.11]
echo ======================================================

:: 2. Check for gradlew.bat
if not exist "gradlew.bat" (
    echo [ERROR] 'gradlew.bat' not found in this directory.
    pause & exit /b
)

:: 3. Input Version and Message
set /p modversion="> Enter Release Version (e.g., 0.1.45): "
set /p commitmsg="> Enter Commit Message: "

echo.
echo [STEP 1/5] Staging changes...
git add .

echo [STEP 2/5] Creating commit...
git commit -m "release: v%modversion% - %commitmsg%"

echo [STEP 3/5] Creating Git tag (v%modversion%)...
git tag -a v%modversion% -m "Release v%modversion% for MC 1.21.11"

echo [STEP 4/5] Cleaning and Building Mod...
:: 'call' is necessary to return control to this script after gradlew finishes
call gradlew.bat clean build

echo [STEP 5/5] Pushing to Remote Repository...
:: Pushing both commit and tags
git push origin master --tags

echo.
echo ======================================================
echo    SUCCESS: Mod version v%modversion% is ready!
echo    Check 'build/libs' for your .jar file.
echo ======================================================
pause