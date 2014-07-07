package com.elex.bigdata.llda.mahout.mapreduce.analysis;

import com.elex.bigdata.llda.mahout.data.inputformat.CombineTextInputFormat;
import com.elex.bigdata.llda.mahout.util.FileSystemUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 7/7/14
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class WordUniqDriver {
  public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
    String inputPath = args[0];
    String uniqWordPath = args[1];
    String outputPath = args[2];
    Configuration conf = new Configuration();
    Job job = prepareJob(conf, new Path(inputPath), new Path(uniqWordPath));
    job.submit();
    job.waitForCompletion(true);

    Job analysisJob = new Job(conf);
    FileSystem fs=FileSystem.get(conf);
    if(fs.exists(new Path(outputPath)))
      fs.delete(new Path(outputPath),true);
    analysisJob.setMapperClass(UniqWordAnalysisMapper.class);
    analysisJob.setReducerClass(WordAnalysisDriver.WordAnalysisReducer.class);
    analysisJob.setCombinerClass(WordAnalysisDriver.WordAnalysisCombiner.class);
    analysisJob.setMapOutputKeyClass(Text.class);
    analysisJob.setMapOutputValueClass(IntWritable.class);
    analysisJob.setInputFormatClass(CombineTextInputFormat.class);
    FileInputFormat.addInputPath(analysisJob, new Path(uniqWordPath));
    analysisJob.setOutputFormatClass(TextOutputFormat.class);
    FileOutputFormat.setOutputPath(analysisJob, new Path(outputPath));
    analysisJob.setJarByClass(WordUniqDriver.class);
    analysisJob.setJobName("uniq word analysis " + inputPath.toString());
    analysisJob.submit();
    analysisJob.waitForCompletion(true);
  }

  public static Job prepareJob(Configuration conf, Path inputPath, Path outputPath) throws IOException {
    Job job = new Job(conf);
    JobClient jobClient=new JobClient(conf);
    ClusterStatus clusterStatus=jobClient.getClusterStatus();
    int maxMapTaskNum=clusterStatus.getMaxMapTasks();
    System.out.println("max Map Task Num "+maxMapTaskNum);
    long totalSize= FileSystemUtil.getLen(conf,inputPath);
    System.out.println("total input Size "+totalSize);

    conf.setLong("mapred.max.split.size", 1024 * 1024 * 1024); // 1G
    conf.setLong("mapreduce.input.fileinputformat.split.maxsize", 1024 * 1000 * 1000);
    conf.setLong("mapred.min.split.size.per.node",512*1024*1024);
    conf.setLong("mapreduce.input.fileinputformat.split.minsize.per.node",512 * 1000 * 1000);
    conf.setLong("mapred.min.split.size.per.rack",768*1024*1024);
    conf.setLong("mapreduce.input.fileinputformat.split.minsize.per.rack",768*1000*1000);

    job.setMapperClass(WordUniqMapper.class);
    job.setReducerClass(WordUniqReducer.class);
    job.setInputFormatClass(CombineTextInputFormat.class);
    FileInputFormat.addInputPath(job, inputPath);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(NullWritable.class);
    FileSystem fs = FileSystem.get(conf);
    if (fs.exists(outputPath))
      fs.delete(outputPath, true);
    TextOutputFormat.setOutputPath(job, outputPath);
    job.setJobName("word uniq " + inputPath.toString());
    job.setJarByClass(WordUniqDriver.class);
    return job;
  }

  public static class WordUniqReducer extends Reducer<Text, NullWritable, Text, IntWritable> {
    public void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
      context.write(key, new IntWritable(1));

    }
  }

  public static class WordUniqMapper extends Mapper<Object, Text, Text, NullWritable> {
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      String line = value.toString();
      String[] tokens = line.split("\t");
      if (tokens.length < 3)
        return;
      context.write(new Text(tokens[1]), NullWritable.get());
    }
  }

  public static class UniqWordAnalysisMapper extends Mapper<Object, Text, Text, IntWritable> {
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      String line = value.toString();
      String[] tokens = line.split("\t");
      if (tokens.length < 2)
        return;
      context.write(new Text(tokens[0]), new IntWritable(Integer.parseInt(tokens[1])));
    }
  }
}
