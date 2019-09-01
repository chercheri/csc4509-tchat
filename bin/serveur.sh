#!/bin/bash

ARGS=$*

# default values
MEMORY_MIN=128	
MEMORY_MAX=1024	
START_BG="&"
PATHSEP=':'

MAVEN_REPOS=${HOME}/.m2/repository
LOG4J_JAR=${MAVEN_REPOS}/log4j/log4j/1.2.17/log4j-1.2.17.jar

CLASSPATH=${LOG4J_JAR}${PATHSEP}:./target/classes


# Start the broker
CMD="java -Xms${MEMORY_MIN}m -Xmx${MEMORY_MAX}m 
          -cp $CLASSPATH chat.server.Main ${ARGS}"

$CMD

