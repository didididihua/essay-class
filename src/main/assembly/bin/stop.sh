#!/usr/bin/env bash

# jar包名称
SERVER_JAR="springboot-init-1.0-SNAPSHOT.jar"

# 停止项目
echo -n "Stopping server ..."
    PID=$(ps -ef | grep $SERVER_JAR | grep -v grep |awk '{print $2}')
if [ -z "$PID" ]; then
  echo Application is already stopped
else
  echo kill $PID
  kill -9 $PID
fi
exit 0
