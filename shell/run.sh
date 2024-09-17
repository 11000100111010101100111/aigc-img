#!/bin/bash

# 定义日志文件
LOG_FILE="log.log"

# 执行 Maven clean install
echo "Running Maven clean install..."
mvn clean install -p1 ai-img -am

# 检查 Maven 是否成功
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# 运行生成的 JAR 文件
echo "Starting Java application..."
nohup java -jar /mq-consumer/target/mq-consumer-0.0.1-SNAPSHOT.jar > "$LOG_FILE" 2>&1 &

# 输出进程 ID
echo "Application started in the background with PID: $!"
