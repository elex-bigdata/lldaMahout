#!/bin/bash
baseDir=`dirname $0`/..
JAR=$baseDir/target/lldaMahout-1.0-SNAPSHOT-jar-with-dependencies.jar
MAIN=com.elex.bigdata.llda.mahout.crond.CrondInfDriver
rootPath=/user/hadoop/user_category/lldaMahout
inputPath=$1
logFile=/data0/log/user_category/processLog/llda/crondInf.log
now=`date`
echo ${now} >> $logFile
echo "hadoop jar $JAR $MAIN --input $inputPath --doc_root ${rootPath}/docs --dict_root ${rootPath}/dictionary \
      --resource_root ${rootPath}/resources --num_topics 5 --model_input ${rootPath}/models --output ${rootPath}/docTopics/inf >> $logFile 2>&1"
hadoop jar $JAR $MAIN --input $inputPath --doc_root ${rootPath}/docs --dict_root ${rootPath}/dictionary \
--resource_root ${rootPath}/resources --num_topics 5 --model_input ${rootPath}/models --output ${rootPath}/docTopics/inf  >> $logFile 2>&1