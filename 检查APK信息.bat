@echo off
echo === 检查APK文件信息 ===
echo.

echo 1. APK文件位置:
dir "app\build\outputs\apk\release\app-release.apk"

echo.
echo 2. APK文件大小:
for %%I in ("app\build\outputs\apk\release\app-release.apk") do (
    echo 文件大小: %%~zI 字节
    echo 约: %%~zI / 1048576 MB
)

echo.
echo 3. 版本信息检查 (通过aapt):
if exist "%ANDROID_HOME%\build-tools\*" (
    for /f "tokens=*" %%d in ('dir /b "%ANDROID_HOME%\build-tools"') do (
        set "latest_build_tools=%%d"
    )
    echo 使用构建工具: %latest_build_tools%
    "%ANDROID_HOME%\build-tools\%latest_build_tools%\aapt.exe" dump badging "app\build\outputs\apk\release\app-release.apk" | findstr /i "version package-name"
) else (
    echo 警告: ANDROID_HOME环境变量未设置或构建工具未找到
    echo 请手动检查build.gradle中的版本配置:
    echo   - versionCode: 8
    echo   - versionName: "0.1.8"
)

echo.
echo 4. 输出元数据文件:
type "app\build\outputs\apk\release\output-metadata.json"

echo.
echo === 检查完成 ===
pause