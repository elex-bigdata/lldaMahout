#!/bin/bash
baseDir=`dirname $0`/..
JAR=$baseDir/target/lldaMahout-1.0-SNAPSHOT-jar-with-dependencies.jar
logFile=/data0/log/user_category/processLog/llda/dayEst.log
oneDayAgo=`date +%Y%m%d -d "-1 days"`
now=`date +%Y%m%d%H%M%S`
echo "-----------------------------------------------------------------------" >> $logFile
echo "                       script run time $now                            " >> $logFile
echo "-----------------------------------------------------------------------" >> $logFile
for((i=0;i<3;i++))do
  echo "                                                                        ">>$logFile
done
echo "                       dayEst $oneDayAgo                            " >> $logFile
for((i=0;i<3;i++))do
  echo "                                                                       ">>$logFile
done
echo "-----------------------------------------------------------------------" >> $logFile
source $baseDir/bin/infEstFuncs.sh

echo "                   updateEstByDay $oneDayAgo                     " >> $logFile
updateEstByDay $oneDayAgo
echo "                   updateEstByDay $oneDayAgo finished            " >> $logFile

source ${baseDir}/bin/categoryAnalysisFuncs.sh

echo "                   anaInfResult   $oneDayAgo                       " >> $logFile
anaInfResult $oneDayAgo
echo "                   anaInfResult   $oneDayAgo  finished            "  >> $logFile

echo "                   updateAnaCategoryDist $oneDayAgo              " >> $logFile
updateAnaCategoryDist $oneDayAgo
echo "                   updateAnaCategoryDist   $oneDayAgo  finished            "  >> $logFile

expired_day=`date +%Y%m%d -d "-10 days"`
hadoop fs -rm -r url_count/all_projects/${expired_day}*
hadoop fs -rm -r user_category/lldaMahout/docs/${expired_day}*
hadoop fs -rm -r user_category/lldaMahout/docs/to${expired_day}*
hadoop fs -rm -r url_count/all_projects/clean/${expired_day}*


