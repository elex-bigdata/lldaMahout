#!/bin/bash
baseDir=`dirname $0`/..
JAR=$baseDir/target/lldaMahout-1.0-SNAPSHOT-jar-with-dependencies.jar

rootPath=/user/hadoop/user_category/lldaMahout
logFile=/data/log/user_category/processLog/llda/analysis.log
if [[ $# < 3 ]]
then
  echo " args < 3"
  exit 1
fi
type=$1
if [[ $type = user ]]
  then
     MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.UserAnalysisDriver
     echo "hadoop jar $JAR $MAIN --input ${rootPath}/$2 --output ${rootPath}/$3 >> $logFile 2>&1"
     hadoop jar $JAR $MAIN --input ${rootPath}/$2 --output ${rootPath}/$3 >> $logFile 2>&1
  elif [[ $type = uniqWord ]]
    then
     MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.WordUniqDriver
     echo "hadoop jar $JAR $MAIN $2 ${rootPath}/analysis/tmp >> $logFile 2>&1"
     hadoop jar $JAR $MAIN $2 ${rootPath}/analysis/tmp >> $logFile 2>&1
     MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.WordAnalysisDriver
     echo "hadoop jar $JAR $MAIN ${rootPath}/analysis/tmp $3 >> $logFile 2>&1"
     hadoop jar $JAR $MAIN ${rootPath}/analysis/tmp $3 >> $logFile 2>&1
  else
    then
     MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.WordAnalysisDriver
     echo "hadoop jar $JAR $MAIN $2 $3 >> $logFile 2>&1"
     hadoop jar $JAR $MAIN $2 $3 >> $logFile 2>&1
fi

