@echo off

REM 定义日志文件
set LOG_FILE=log.log

REM 运行生成的 JAR 文件
echo Starting Java application...
start /b java -jar ..\mq-consumer\target\mq-consumer.jar > %LOG_FILE% 2>&1

echo Application started in the background.
