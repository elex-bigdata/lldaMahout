package com.elex.bigdata.llda.mahout.data.generatedocs;

import com.elex.bigdata.hashing.BDMD5;
import com.elex.bigdata.hashing.HashingException;
import com.elex.bigdata.llda.mahout.dictionary.Dictionary;
import com.elex.bigdata.llda.mahout.dictionary.UpdateDictDriver;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.math.MultiLabelVectorWritable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 5/14/14
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class GenerateLDocReducer extends Reducer<Text,Text,Text,MultiLabelVectorWritable> {
  Dictionary dict;
  BDMD5 bdmd5;
  Map<String,String> url_category_map=new HashMap<String,String>();
  Map<String,Integer> category_label_map=new HashMap<String, Integer>();
  SequenceFile.Writer uidWriter;
  //int termSize;
  public void setup(Context context) throws IOException {
    /*
       load dict;
       load url--category map
       get uid Path(write uid to hdfs) and create a sequenceFileWriter
    */
    Configuration conf=context.getConfiguration();
    FileSystem fs=FileSystem.get(conf);
    Path urlCategoryPath=new Path(conf.get(GenerateLDocDriver.URL_CATEGORY_PATH));
    Path categoryLabelPath=new Path(conf.get(GenerateLDocDriver.CATEGORY_LABEL_PATH));
    Path uidPath=new Path(conf.get(GenerateLDocDriver.UID_PATH));
    if(fs.exists(uidPath))
      fs.delete(uidPath);
    try {
      bdmd5=BDMD5.getInstance();
      dict=new Dictionary(conf.get(UpdateDictDriver.DICT_ROOT),fs,conf);
    } catch (HashingException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    //termSize=dictionary.size();

    BufferedReader urlCategoryReader=new BufferedReader(new InputStreamReader(fs.open(urlCategoryPath)));
    String line="";
    while((line=urlCategoryReader.readLine())!=null){
      String[] categoryUrls=line.split(" ");
      for(int i=1;i<categoryUrls.length;i++){
        url_category_map.put(categoryUrls[i],categoryUrls[0]);
      }
    }
    urlCategoryReader.close();

    BufferedReader categoryLabelReader=new BufferedReader(new InputStreamReader(fs.open(categoryLabelPath)));
    while((line=categoryLabelReader.readLine())!=null){
      String[] categoryLabels=line.split("=");
      category_label_map.put(categoryLabels[0],Integer.parseInt(categoryLabels[1]));
    }

    uidWriter=SequenceFile.createWriter(fs,conf,uidPath,Text.class, NullWritable.class);
  }
  public void reduce(Text key,Iterable<Text> values,Context context) throws IOException, InterruptedException {
     /*
       create a SparseVector with size of dictSize
       in loop for values:
          get id of url
          vector set value in index id
          get category url and it's label,then set labels

       uidWriter.write(key,NullWritable)

     */
    Map<Integer,Double> urlCounts=new HashMap<Integer, Double>();
    Set<Integer> labelSet=new HashSet<Integer>();

    for(Text url: values){
      String wordMd5= null;
      try {
        wordMd5 = bdmd5.toMD5(url.toString());
      } catch (HashingException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      if(!dict.contains(wordMd5))
        continue;
      int id=dict.getId(wordMd5);
      if(urlCounts.containsKey(id))
        urlCounts.put(id,urlCounts.get(id)+1l);
      else
        urlCounts.put(id,1.0);
      String category=url_category_map.get(url.toString());
      if(category!=null){
        Integer label=category_label_map.get(category);
        if(label!=null){
           labelSet.add(label);
        }
      }
    }
    Vector urlCountsVector=new RandomAccessSparseVector(urlCounts.size()*2);
    for(Map.Entry<Integer,Double> urlCount: urlCounts.entrySet()){
       urlCountsVector.setQuick(urlCount.getKey(),urlCount.getValue());
    }
    int[] labels=new int[labelSet.size()];
    int i=0;
    for(Integer label:labelSet){
      labels[i++]=label;
    }
    MultiLabelVectorWritable labelVectorWritable=new MultiLabelVectorWritable(urlCountsVector,labels);
    uidWriter.append(key,NullWritable.get());
    context.write(key,labelVectorWritable);
  }

  public void cleanup(Context context) throws IOException {
    uidWriter.hflush();
    uidWriter.close();
  }
}
