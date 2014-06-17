package com.elex.bigdata.llda.mahout.data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.util.bloom.BloomFilter;
import org.junit.Test;

import java.io.DataInput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 6/13/14
 * Time: 4:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileSystemTest {
  @Test
  public void testFileStatus() throws IOException, URISyntaxException {
    Path path=new Path("/home/yb/windows/share/categoryFilter");
    FileSystem fs=new RawLocalFileSystem();
    fs.initialize(new URI("localFs"),new Configuration());
    fs.exists(path);
    FileStatus fileStatus=fs.getFileStatus(path);
    FileStatus[] fileStatuses= fs.listStatus(path);
    for(FileStatus fileStatus1:fileStatuses){
      //System.out.println(fileStatus1.getPath().getName());
      //System.out.println(fileStatus1.getPath().toString());
    }
    DataInput dataInput=fs.open(new Path("/home/yb/windows/share/categoryFilter/Top.Arts.Animation"));
    BloomFilter bloomFilter=new BloomFilter();
    bloomFilter.readFields(dataInput);
    System.out.println("hhh") ;
  }
}