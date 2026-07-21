@echo off
echo 正在构建 同行 v0.1.8...
echo.

REM 清理构建缓存
echo 步骤1：清理构建缓存...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo 清理失败！
    pause
    exit /b 1
)

echo.
echo 步骤2：构建Release APK...
echo 这可能需要10-20分钟，请耐心等待...
echo.

REM 构建Release版本
call gradlew.bat assembleRelease --no-daemon --stacktrace
if %errorlevel% neq 0 (
    echo.
    echo 构建失败！
    echo 请检查：
    echo 1. 网络连接是否正常
    echo 2. 是否有足够的磁盘空间
    echo 3. Android SDK配置是否正确
    pause
    exit /b 1
)

echo.
echo ============================================
echo ✅ 构建成功！
echo ============================================
echo.
echo APK文件位置：app\build\outputs\apk\release\app-release.apk
echo.
echo 版本信息：
echo   应用名称：同行
echo   版本号：0.1.8
echo   版本代码：8
echo   包名：com.tongxingapp
echo.
echo 导出系统修复：
echo   ✅ 添加了WRITE_EXTERNAL_STORAGE权限
echo   ✅ 添加了MANAGE_EXTERNAL_STORAGE权限
echo   ✅ 实现了三级导出策略
echo   ✅ 改进了错误处理和用户提示
echo.
pause