package com.elex.bigdata.llda.mahout.mapreduce.etl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 5/28/14
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResultEtlDriver extends AbstractJob {
  public static String LOCAL_RESULT_ROOT = "local_result_root";
  public static final String RESULT_TIME = "result_time";

  @Override
  public int run(String[] args) throws Exception {
    addInputOption();
    addOutputOption();
    addOption(LOCAL_RESULT_ROOT, "lrp", "local result output path", "/data/log/user_category_result/pr");
    addOption(RESULT_TIME, "result_time", "specify the inf result time", false);
    if (parseArguments(args) == null)
      return -1;
    String day;
    int hour, index;
    if (hasOption(RESULT_TIME)) {
      String time = getOption(RESULT_TIME);
      day = time.substring(0, 8);
      hour = Integer.parseInt(time.substring(8, 10));
      index = Integer.parseInt(time.substring(10, 12)) / 5;
    } else {
      Date date = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      day = dateFormat.format(date);
      hour = date.getHours();
      index = date.getMinutes() / 5;
    }
    Path inputPath = getInputPath();
    Path outputPath = getOutputPath();
    File localResultRoot = new File(getOption(LOCAL_RESULT_ROOT));
    File localResultDir=new File(localResultRoot,day);
    if(!localResultDir.exists())
      localResultDir.mkdirs();
    File localResultFile=new File(localResultDir, hour + "." + index);
    Job etlJob = prepareJob(getConf(), inputPath, outputPath);
    etlJob.waitForCompletion(true);
    Runtime.getRuntime().exec("hadoop fs -getmerge " + outputPath.toString() + " " + localResultFile.toString());
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public static Job prepareJob(Configuration conf, Path inputPath, Path outputPath) throws IOException, ClassNotFoundException, InterruptedException {
    Job job = new Job(conf);
    FileSystem fs = FileSystem.get(conf);
    if (fs.exists(outputPath))
      fs.delete(outputPath);
    job.setMapperClass(ResultEtlMapper.class);
    job.setReducerClass(ResultEtlReducer.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, inputPath);
    TextOutputFormat.setOutputPath(job, outputPath);
    job.setJobName("etl " + inputPath.toString());
    job.setJarByClass(ResultEtlDriver.class);
    job.submit();
    return job;
  }

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new ResultEtlDriver(), args);
  }
}
