package com.elex.bigdata.llda.mahout.mapreduce.inf;

import com.elex.bigdata.llda.mahout.mapreduce.est.LLDADriver;
import com.elex.bigdata.llda.mahout.model.LabeledModelTrainer;
import com.elex.bigdata.llda.mahout.model.LabeledTopicModel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 4/22/14
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class LLDAInferenceMapper extends Mapper<Text, MultiLabelVectorWritable, Text, Text> {
  private static final Logger log = LoggerFactory.getLogger(LLDAInferenceMapper.class);
  private LabeledModelTrainer modelTrainer;
  private LabeledTopicModel readModel;
  private LabeledTopicModel writeModel;
  private int maxIters;
  private int numTopics;


  protected LabeledModelTrainer getModelTrainer() {
    return modelTrainer;
  }

  protected int getMaxIters() {
    return maxIters;
  }

  protected int getNumTopics() {
    return numTopics;
  }

  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    log.info("Retrieving configuration");
    Configuration conf = context.getConfiguration();
    float eta = conf.getFloat(LLDADriver.TERM_TOPIC_SMOOTHING, Float.NaN);
    float alpha = conf.getFloat(LLDADriver.DOC_TOPIC_SMOOTHING, Float.NaN);
    long seed = conf.getLong(LLDADriver.RANDOM_SEED, 1234L);
    numTopics = conf.getInt(LLDADriver.NUM_TOPICS, -1);
    int numTerms = conf.getInt(LLDADriver.NUM_TERMS, -1);
    int numUpdateThreads = conf.getInt(LLDADriver.NUM_UPDATE_THREADS, 1);
    int numTrainThreads = conf.getInt(LLDADriver.NUM_TRAIN_THREADS, 4);
    maxIters = conf.getInt(LLDADriver.MAX_ITERATIONS_PER_DOC, 10);
    float modelWeight = conf.getFloat(LLDADriver.MODEL_WEIGHT, 1.0f);

    System.out.println("Initializing read model");
    Path[] modelPaths = LLDADriver.getModelPaths(conf);
    if (modelPaths != null && modelPaths.length > 0) {
      readModel = new LabeledTopicModel(conf, eta, alpha, null, numUpdateThreads, modelWeight, modelPaths);
      numTerms=readModel.getNumTerms();
    } else {
      log.info("No model files found");
      readModel = new LabeledTopicModel(numTopics, numTerms, eta, alpha, RandomUtils.getRandom(seed), null,
        numTrainThreads, modelWeight);
    }

    System.out.println("Initializing write model");
    writeModel = modelWeight == 1
      ? new LabeledTopicModel(numTopics, numTerms, eta, alpha, null, numUpdateThreads)
      : readModel;

    System.out.println("Initializing model trainer");
    modelTrainer = new LabeledModelTrainer(readModel, writeModel, numTrainThreads, numTopics, numTerms);
    modelTrainer.start();
  }


  @Override
  public void map(Text uid, MultiLabelVectorWritable doc, Context context)
    throws IOException, InterruptedException {
    /*
    int numTopics = getNumTopics();
    int[] labels = doc.getLabels();
    Vector topicVector = new DenseVector(numTopics);
    if (labels.length > 0) {
      topicVector.assign(0);
      for (int i = 0; i < labels.length; i++) {
        topicVector.set(labels[i], (double) 1l / labels.length);
      }
    } else {
      topicVector.assign(1l/topicVector.size());
    }
    Matrix docModel = new SparseRowMatrix(numTopics, doc.getVector().size());
    int maxIters = getMaxIters();
    ModelTrainer modelTrainer = getModelTrainer();
    for (int i = 0; i < maxIters; i++) {
      modelTrainer.getReadModel().trainDocTopicModel(doc.getVector(), topicVector, docModel);
    }
    topics.set(topicVector);
    context.write(uid, topics);
    */
    Vector labels=new RandomAccessSparseVector(numTopics);
    //labels.assign(0.0);
    StringBuilder builder=new StringBuilder();
    builder.append("[");
    for(int label: doc.getLabels()){
      labels.set(label,1.0);
      builder.append(label+" ");
    }
    builder.append("]\t");
    if(doc.getLabels().length==0){
      labels.assign(1.0);
    }
    Matrix docModel = new SparseRowMatrix(numTopics,doc.getVector().size());
    //int maxIters=getMaxIters();
    LabeledModelTrainer modelTrainer=getModelTrainer();
    //System.out.println("labels is "+labels.toString()+"\r\n vector is "+doc.getVector().toString());
    //for(int i=0;i<maxIters;i++){
    modelTrainer.getReadModel().trainDocTopicModel(doc.getVector(),labels,docModel,true);
    //}
    for(Vector.Element e: labels){
       builder.append(e.index()+":"+e.get()+",");
    }
    builder.deleteCharAt(builder.length()-1);
    context.write(uid,new Text(builder.toString()));
  }

  @Override
  protected void cleanup(Context context) {
    getModelTrainer().stop();
  }
}
