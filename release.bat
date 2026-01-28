@echo off
setlocal enabledelayedexpansion

:: 1. 파일 위치 확인
if not exist "gradlew.bat" (
    echo [ERROR] 이 파일은 프로젝트 루트(build.gradle이 있는 곳)에 있어야 합니다.
    pause & exit /b
)

:: 2. 정보 입력
set /p modversion="1. 배포할 버전 숫자를 입력하세요 (예: 0.1.44): "
set /p commitmsg="2. 커밋 메시지를 입력하세요: "

echo.
echo [1/5] 변경 사항 스테이징 중...
git add .

echo [2/5] 커밋 생성 중...
git commit -m "release: v%modversion% - %commitmsg%"

echo [3/5] Git 태그 생성 중 (v%modversion%)...
git tag -a v%modversion% -m "Release v%modversion% for MC 1.21.11"

echo [4/5] 이전 빌드 청소 및 새 빌드 시작...
call gradlew.bat clean build

echo [5/5] 서버로 푸시 중 (선택 사항)...
git push origin main --tags

echo.
echo ======================================================
echo 빌드 완료! v%modversion% 버전이 생성되었습니다.
echo ======================================================
pause