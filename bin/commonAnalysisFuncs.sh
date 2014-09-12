#!/bin/bash

function anaWordCountByUserTrainedDist(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log

   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.user.WordCountByUserTrainedDistDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1
}

function anaWordCountByUserDist(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log
   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.user.WordCountByUserDistDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1
}

function anaTopicModel(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log
   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.category.TopicTermModelDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1
}


function anaWordCountDist(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log
   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.word.WordCountDistDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1
}

function extractWordPrefix(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log

   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.word.WordPrefixExtractDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 >> $logFile 2>&1
}



function anaPreClassifiedWord(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log

   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.word.PreClassifiedWordDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 --resource_root ${rootPath}/resources >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 --resource_root ${rootPath}/resources >> $logFile 2>&1
}

function getWordsByUser(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log
   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.user.WordsByUserDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 --resource_root ${rootPath}/resources >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 --resource_root ${rootPath}/resources >> $logFile 2>&1
}

function anaWordLenDist(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log

   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.word.WordLenDistDriver
   echo "hadoop jar $JAR $MAIN $1 $2 >> $logFile 2>&1"
   hadoop jar $JAR $MAIN $1 $2 >> $logFile 2>&1
}

function anaIdf(){
   rootPath=/user/hadoop/user_category/lldaMahout
   local logFile=/data0/log/user_category/processLog/llda/analysis.log
   local MAIN=com.elex.bigdata.llda.mahout.mapreduce.analysis.word.IdfDriver
   echo "hadoop jar $JAR $MAIN --input $1 --output $2 --classified_url $3 --resource_root ${rootPath}/resources  >> $logFile 2>&1"
   hadoop jar $JAR $MAIN --input $1 --output $2 --classified_url $3 --resource_root ${rootPath}/resources  >> $logFile 2>&1
}

