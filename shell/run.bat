@echo off

REM 定义日志文件
set LOG_FILE=log.log

REM 执行 Maven clean install
echo Running Maven clean install...
mvn clean install -p1 ai-img -am

REM 检查 Maven 是否成功
if %ERRORLEVEL% neq 0 (
    echo Maven build failed!
    exit /b 1
)

REM 运行生成的 JAR 文件
echo Starting Java application...
start /b java -jar ..\mq-consumer\target\your-application.jar > %LOG_FILE% 2>&1

echo Application started in the background.
