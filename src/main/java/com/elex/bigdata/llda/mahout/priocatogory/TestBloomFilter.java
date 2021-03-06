package com.elex.bigdata.llda.mahout.priocatogory;


import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.bloom.BloomFilter;
import org.apache.hadoop.util.bloom.Key;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 6/13/14
 * Time: 5:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestBloomFilter {
  private static String tableName="dmp_user_action";
  public static void main(String[] args) throws IOException, ParseException {
    int urlCount=0,hitCount=0;
    Map<String,Integer> categoryUrlCount=new HashMap<String, Integer>();
    String inputDir="/user/hadoop/user_category/lldaMahout/resources/categoryFilter";
    FileSystem fs=FileSystem.get(HBaseConfiguration.create());
    PrioCategoriesLoader prioCategoriesLoader=PrioCategoriesLoader.getCategoriesLoader(inputDir, fs);
    BloomFilter globalFilter=prioCategoriesLoader.getGlobalFilter();
    System.out.println("globalFilter ==null "+(globalFilter==null));
    Map<String,BloomFilter> bloomFilterMap=prioCategoriesLoader.getCategoryFilters();
    HTable table=new HTable(HBaseConfiguration.create(),tableName);
    String startTime=args[0];
    String endTime=args[1];
    SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
    long startTimeStamp=dateFormat.parse(startTime).getTime();
    long endTimeStamp=dateFormat.parse(endTime).getTime();
    byte[] startRk=Bytes.add(Bytes.toBytesBinary("\\x01"),Bytes.toBytes(startTimeStamp));
    byte[] endRk=Bytes.add(Bytes.toBytesBinary("\\x01"),Bytes.toBytes(endTimeStamp));
    System.out.println(Bytes.toStringBinary(startRk)+"  "+Bytes.toStringBinary(endRk));
    Scan scan=new Scan();
    scan.setStartRow(startRk);
    scan.setStopRow(endRk);
    scan.addColumn(Bytes.toBytes("ua"),Bytes.toBytes("url"));
    scan.setCaching(5096);
    ResultScanner results=table.getScanner(scan);
    long timeCost=0l;
    for (Result result : results) {
      for (KeyValue kv : result.raw()) {
        String url = Bytes.toString(kv.getValue());
        if(url.startsWith("http://"))
          url=url.substring(7);
        if(url.startsWith("https://"))
          url=url.substring(8);
        if(url.endsWith("/"))
          url=url.substring(0,url.length()-1);
        Key key=new Key(Bytes.toBytes(url));
        urlCount++;
        long t1=System.currentTimeMillis();
        if(globalFilter.membershipTest(key)){
           hitCount++;
           for(Map.Entry<String,BloomFilter> entry:bloomFilterMap.entrySet()){
             if(entry.getValue().membershipTest(key)){
               Integer count=categoryUrlCount.get(entry.getKey());
               if(count==null){
                 count=new Integer(0);
               }
               categoryUrlCount.put(entry.getKey(),count+1);
               break;
             }
           }
        }
        timeCost+=(System.currentTimeMillis()-t1);
      }
    }
    System.out.println("global url count "+urlCount+" hitCount"+hitCount);
    System.out.println("check bloomFilter cost time "+timeCost+" ms");
    for(Map.Entry<String,Integer> entry:categoryUrlCount.entrySet()){
      System.out.println(entry.getKey()+":"+entry.getValue());
    }
  }
}
