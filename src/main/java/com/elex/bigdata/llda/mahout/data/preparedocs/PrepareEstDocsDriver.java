package com.elex.bigdata.llda.mahout.data.preparedocs;

import com.elex.bigdata.llda.mahout.data.LabeledDocumentWritable;
import com.elex.bigdata.llda.mahout.data.generatedocs.GenerateLDocDriver;
import com.elex.bigdata.llda.mahout.data.generatedocs.GenerateLDocMapper;
import com.elex.bigdata.llda.mahout.data.generatedocs.GenerateLDocReducer;
import com.elex.bigdata.llda.mahout.data.mergedocs.MergeLDocMapper;
import com.elex.bigdata.llda.mahout.data.mergedocs.MergeLDocReducer;
import com.elex.bigdata.llda.mahout.dictionary.UpdateDictDriver;
import com.elex.bigdata.llda.mahout.dictionary.UpdateDictMapper;
import com.elex.bigdata.llda.mahout.dictionary.UpdateDictReducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 5/15/14
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class PrepareEstDocsDriver extends AbstractJob {
  @Override
  public int run(String[] args) throws Exception {
    addInputOption();
    /*
      dictRoot--dict,dictSize,tmpDict
     */
    addOption(UpdateDictDriver.DICT_OPTION_NAME,"dict","dictionary root Path",true);

    /*
      lDocRoot:day--sequenceFile(uidUrlCount seperated by day),total(_day)(total url info to day),inf(the docs to inf)
     */
    addOption(GenerateLDocDriver.DOC_ROOT_OPTION_NAME,"docsRoot","specify the lDocs Root Directory");
    addOption(PrepareInfDocsDriver.PRE_LDOC_OPTION_NAME,"lIn","InputPath for previous lDocs");
    addOption(GenerateLDocDriver.DOC_OPTION_NAME,"docsDir","specify the lDocs directory");
    /*
      resources:url_category,category_label

     */
    addOption(GenerateLDocDriver.RESOURCE_OPTION_NAME,"rDir","specify the resources Dir");

    if(parseArguments(args)==null){
      return -1;
    }
    Path textInputPath=getInputPath();
    String dictRoot=getOption(UpdateDictDriver.DICT_OPTION_NAME);
    String dictPath=dictRoot+ File.separator+"dict";
    String tmpDictPath=dictRoot+File.separator+"tmpDict";
    String dictSizePath=dictRoot+File.separator+"dictSize";
    Configuration conf=getConf();
    conf.set(UpdateDictDriver.DICT_PATH,dictPath);
    conf.set(UpdateDictDriver.DICT_SIZE_PATH,dictSizePath);
    conf.set(UpdateDictDriver.TMP_DICT_PATH,tmpDictPath);
    setConf(conf);
    Path dictOutputPath=new Path(dictRoot+File.separator+"updateDicOut");
    Job updateDictJob=prepareJob(textInputPath,dictOutputPath, TextInputFormat.class, UpdateDictMapper.class, LongWritable.class,Text.class, UpdateDictReducer.class,Text.class, IntWritable.class, SequenceFileOutputFormat.class);
    updateDictJob.setJobName("updateDict");
    JobControl jobControl=new JobControl("prepareInfDocs");
    ControlledJob controlledDictJob=new ControlledJob(conf);
    controlledDictJob.setJob(updateDictJob);
    jobControl.addJob(controlledDictJob);

    String docsRoot=getOption(GenerateLDocDriver.DOC_ROOT_OPTION_NAME);
    String docsDir=getOption(GenerateLDocDriver.DOC_OPTION_NAME);
    String docsPath=docsRoot+File.separator+docsDir;
    String uidPath=docsRoot+File.separator+"uid";

    String resourceDir=getOption(GenerateLDocDriver.RESOURCE_OPTION_NAME);
    String urlCategoryPath=resourceDir+File.separator+"url_category";
    String categoryLabelPath=resourceDir+File.separator+"category_label";

    conf.set(GenerateLDocDriver.URL_CATEGORY_PATH,urlCategoryPath);
    conf.set(GenerateLDocDriver.CATEGORY_LABEL_PATH,categoryLabelPath);
    conf.set(GenerateLDocDriver.UID_PATH,uidPath);

    Job generateDocJob=prepareJob(textInputPath,new Path(docsPath),TextInputFormat.class, GenerateLDocMapper.class,LongWritable.class,Text.class, GenerateLDocReducer.class,Text.class, LabeledDocumentWritable.class,SequenceFileOutputFormat.class);
    ControlledJob controlledGenLDocJob=new ControlledJob(conf);
    controlledGenLDocJob.setJob(generateDocJob);
    controlledGenLDocJob.addDependingJob(controlledDictJob);
    jobControl.addJob(controlledGenLDocJob);

    String estLDocPath=docsRoot+File.separator+"_"+docsDir;
    String preLDocPath=docsRoot+File.separator+getOption(PrepareInfDocsDriver.PRE_LDOC_OPTION_NAME);
    String currentDocPath=docsPath;
    Job mergeDocsJob=prepareJob(new Path(preLDocPath),new Path(estLDocPath),SequenceFileInputFormat.class, MergeLDocMapper.class,
      Text.class,LabeledDocumentWritable.class,MergeLDocReducer.class,Text.class,LabeledDocumentWritable.class,SequenceFileOutputFormat.class);
    SequenceFileInputFormat.addInputPath(mergeDocsJob,new Path(currentDocPath));
    ControlledJob controlledMergeDocsJob=new ControlledJob(conf);
    controlledMergeDocsJob.setJob(mergeDocsJob);
    jobControl.addJob(controlledMergeDocsJob);

    Thread jcThread=new Thread(jobControl);
    jcThread.start();
    while(true){
      if(jobControl.allFinished()){
        System.out.println(jobControl.getSuccessfulJobList());
        jobControl.stop();
        return 0;
      }
      if(jobControl.getFailedJobList().size()>0){
        System.out.println(jobControl.getFailedJobList());
        jobControl.stop();
        return 1;
      }
    }
  }

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(),new PrepareEstDocsDriver(),args);
  }
}
