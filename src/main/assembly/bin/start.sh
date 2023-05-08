#!/bin/sh

#source $(dirname $0)/../../env.sh
# 进入当前文件目录
cd `dirname $0`
# 返回上一级
cd ..

# jar包名称
#PROJECT_VERSION=`sed '/^application.version=/!d;s/.*=//' conf/application.properties`
#SERVER_JAR="$SERVER_NAME-$PROJECT_VERSION.jar"
SERVER_JAR="springboot-init-1.0-SNAPSHOT.jar"
BASE_DIR=`pwd`

# 获取java路径
if [ "$JAVA_HOME" != "" ]; then
 JAVA="$JAVA_HOME/bin/java"
else
 JAVA=java
fi

# 指定日志输出路径
LOGS_DIR=""
if [ -n "$LOGS_FILE" ]; then
    LOGS_DIR=`dirname $LOGS_FILE`
else
    LOGS_DIR=$BASE_DIR/logs
fi
if [ ! -d $LOGS_DIR ]; then
    mkdir $LOGS_DIR
fi
STDOUT_FILE=$LOGS_DIR/stdout.log

# 设置jvm参数
JAVA_OPTS="-server -Xms512m -Xmx512m -Xmn256m -Xss1m \
-XX:SurvivorRatio=4 -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection \
-XX:CMSInitiatingOccupancyFraction=70 \
-Xloggc:$LOGS_DIR/gc.log"

# 如果项目已经启动则之前停止项目
echo -n "Starting server ..."
 PID=$(ps -ef | grep $SERVER_JAR | grep -v grep |awk '{print $2}')
if [ -z "$PID" ]; then
 echo Application is already stopped
else
 echo kill $PID
 kill -9 $PID
fi

# 以指定参数启动项目
nohup $JAVA $JAVA_OPTS $JAVA_DEBUG_OPT -jar lib/$SERVER_JAR "--spring.profiles.active=prod" > $STDOUT_FILE 2>&1 &

if [ $? -eq 0 ];then
 # echo -n $! > "$PIDFILE"
 if [ $? -eq 0 ]
 then
 sleep 1
 echo STARTED
 else
 echo FAILED TO WRITE PID
 exit 1
 fi
else
 echo SERVER DID NOT START
 exit 1
fi

PID_NOW=`ps -ef | grep java | grep -v grep | grep "$SERVER_JAR" | awk '{print $2}'`
# 打印参数
echo "进程ID: $PID_NOW"
echo "输出日志：$STDOUT_FILE"

