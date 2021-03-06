package com.elex.bigdata.llda.mahout.dictionary;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 5/13/14
 * Time: 10:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateDictMapper extends Mapper<Object, Text, Text, IntWritable> {
  private static final Logger log = LoggerFactory.getLogger(UpdateDictMapper.class);

  public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
    String[] uidUrlCount = value.toString().split("\t");
    if (uidUrlCount.length < 3) {
      log.warn("wrong uidUrlCount " + value.toString());
      return;
    }
    String url = uidUrlCount[1];
    Integer count = Integer.parseInt(uidUrlCount[2]);
    int index = url.indexOf('?');
    if (index != -1)
      url = url.substring(0, index);
    int frequent = 0;
    for (int i = 0; i < url.length(); i++) {
      if (url.charAt(i) == '/') {
        frequent++;
        if (frequent == 3) {
          url = url.substring(0, i);
          break;
        }
      }
    }
    context.write(new Text(url), new IntWritable(count));
  }
}
