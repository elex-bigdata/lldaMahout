package com.elex.bigdata.llda.mahout.data.accumulateurlcount;

import com.elex.bigdata.util.MetricMapping;
import com.xingcloud.xa.hbase.filter.SkipScanFilter;
import com.xingcloud.xa.hbase.model.KeyRange;
import com.xingcloud.xa.hbase.model.KeyRangeComparator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: yb
 * Date: 4/11/14
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class Accumulate extends AbstractJob{
  String outputBase;
  String startTime;
  String endTime;
  private long startTimeStamp, endTimeStamp;
  private Map<Byte, String> projectMap = new HashMap<Byte, String>();
  private ExecutorService service = new ThreadPoolExecutor(3, 8, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(20));
  private FileSystem fs;
  private HBaseAdmin admin;
  private Configuration conf;
  private static String jogosUrl = "www.jogos.com", comprasUrl = "www.compras.com", otherUrl = "www.other.com",
    friendsUrl = "www.friends.com", tourismUrl = "www.tourism.com";

  private static String OUTPUT_BASE="outputBase";
  private static String STARTTIME="startTime";
  private static String ENDTIME="endTime";

  private static final String NAV_TABLE = "nav_all";
  private static final String[] Nav_Families = {"basis", "extend"};
  private static final String URL = "url", IP = "ip", CONTENT = "content";
  private static final int navUidIndex = 17;

  private static final String AD_TABLE = "ad_all_log";
  private static final String[] Ad_Families = {"basis", "h"};
  private static final String TITLE = "title", CATEGORY = "c";
  private static final int adUidIndex = 11;

  private static final String PLUGIN_TABLE = "dmp_user_action";
  private static final String YAC_TABLE="yac_user_action";
  //custom table structure
  private static final String[] Custom_Families = {"ua"};
  private static final String ACTION = "a", DURATION = "d", PROJECT_AREA = "pa", PROJECT = "p",  PLUG_CATEGORY = "cat",ORIG_URL="ou";
  private static final int customUidIndex = 9;

  private static  PutUrlCount putUrlCount=null;

  public Accumulate(){

  }

  public Accumulate(String outputBase,String startTime,String endTime) throws ParseException, IOException {
     init(outputBase,startTime,endTime);
  }

  private void init(String outputBase,String startTime,String endTime) throws ParseException, IOException {
    this.outputBase=outputBase;
    this.startTime=startTime;
    this.endTime=endTime;
    SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
    startTimeStamp=dateFormat.parse(startTime).getTime();
    endTimeStamp=dateFormat.parse(endTime).getTime();
    conf=HBaseConfiguration.create();
    fs=FileSystem.get(conf);
    admin=new HBaseAdmin(conf);
    putUrlCount=new PutUrlCount(fs);
  }



  //if timestamp in rk is long type or string ,rk should be different
  private List<KeyRange> getKeyRanges(String project, Set<String> nations, boolean timeAsLong) {
    List<KeyRange> keyRangeList = new ArrayList<KeyRange>();
    for (String nation : nations) {
      byte[] startRk = Bytes.add(new byte[]{MetricMapping.getInstance().getProjectURLByte(project)}, Bytes.toBytes(nation), timeAsLong ? Bytes.toBytes(startTimeStamp) : Bytes.toBytes(startTime));
      byte[] endRk = Bytes.add(new byte[]{MetricMapping.getInstance().getProjectURLByte(project)}, Bytes.toBytes(nation), timeAsLong ? Bytes.toBytes(endTimeStamp) : Bytes.toBytes(endTime));
      KeyRange keyRange = new KeyRange(startRk, true, endRk, false);
      keyRangeList.add(keyRange);
    }
    KeyRangeComparator comparator = new KeyRangeComparator();
    Collections.sort(keyRangeList, comparator);
    return keyRangeList;
  }

  private List<KeyRange> getSortedKeyRanges(boolean timeAsLong) {
    List<KeyRange> keyRanges = new ArrayList<KeyRange>();
    List<String> projects = new ArrayList<String>();
    //todo
    //list all projects and add to list projects
    for (String project : MetricMapping.getInstance().getAllProjectShortNameMapping().keySet())
      projects.add(project);
    for (String proj : projects) {
      Byte projectId = MetricMapping.getInstance().getProjectURLByte(proj);
      projectMap.put(projectId, proj);
      Set<String> nations = new HashSet<String>();
      System.out.println("projectId " + projectId + " project: " + proj);
      //todo
      //get nations according to proj and execute the runner.
      long t3 = System.currentTimeMillis();
      Set<String> nationSet = MetricMapping.getNationsByProjectID(projectId);
      for (String nation : nationSet) {
        nations.add(nation);
      }
      System.out.println("get nations use " + (System.currentTimeMillis() - t3) + " ms");

      if (nations.size() != 0 && projectId != null) {
        keyRanges.addAll(getKeyRanges(proj, nations, timeAsLong));
      }
    }
    KeyRangeComparator comparator = new KeyRangeComparator();
    Collections.sort(keyRanges, comparator);
    /*
    for (KeyRange keyRange : keyRanges) {
      System.out.println("add keyRange " + Bytes.toStringBinary(keyRange.getLowerRange()) + "---" + Bytes.toStringBinary(keyRange.getUpperRange()));
    }
    */
    return keyRanges;
  }


  private Scan getScan(Map<String, List<String>> familyColumns, boolean timeAsLong) {
    List<KeyRange> keyRanges = getSortedKeyRanges(timeAsLong);
    Scan scan = new Scan();
    Filter filter = new SkipScanFilter(keyRanges);
    scan.setFilter(filter);
    scan.setStartRow(keyRanges.get(0).getLowerRange());
    scan.setStopRow(keyRanges.get(keyRanges.size() - 1).getUpperRange());
    int cacheSize = 10000;
    scan.setCaching(cacheSize);
    for (Map.Entry<String, List<String>> entry : familyColumns.entrySet()) {
      String family = entry.getKey();
      for (String column : entry.getValue()) {
        scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(column));
      }
    }

    return scan;
  }

  public void getNavUidUrl() throws IOException, InterruptedException {
    if(!admin.tableExists(NAV_TABLE))
      return;
    HTable hTable = new HTable(conf, NAV_TABLE);
    Map<String, List<String>> familyColumns = new HashMap<String, List<String>>();
    List<String> columns = new ArrayList<String>();
    columns.add(URL);
    familyColumns.put(Nav_Families[0], columns);
    ResultScanner scanner = hTable.getScanner(getScan(familyColumns, false));
    int kvSize=0;
    Map<String, Map<String, Integer>> uidUrlCountMap = new HashMap<String, Map<String, Integer>>();
    for (Result result : scanner) {
      for (KeyValue kv : result.raw()) {
        byte[] rk = kv.getRow();
        String uid = Bytes.toString(Arrays.copyOfRange(rk, navUidIndex, rk.length));
        String url = Bytes.toString(kv.getValue());

        Map<String, Integer> urlCountMap = uidUrlCountMap.get(uid);
        if (urlCountMap == null) {
          urlCountMap = new HashMap<String, Integer>();
          uidUrlCountMap.put(uid, urlCountMap);
        }
        Integer count = urlCountMap.get(url);
        if (count == null)
          urlCountMap.put(url, new Integer(1));
        else
          urlCountMap.put(url, count + 1);
        kvSize++;
      }
      if(kvSize>=500000){
        kvSize=0;
        System.out.println("put to hdfs");
        putToHdfs(uidUrlCountMap,"navCustom");
        uidUrlCountMap=new HashMap<String, Map<String, Integer>>();
      }
    }
    putToHdfs(uidUrlCountMap,"navCustom");
  }
  /*
     rk:  \x01 (8 bit timestamp) uid
     url: ua:url
   */
  public void getCustomUidUrl(String tableName) throws IOException {
    if(!admin.tableExists(tableName))
      return;
    HTable hTable = new HTable(conf, tableName);
    Scan scan=new Scan();
    byte[] startRk=Bytes.add(Bytes.toBytesBinary("\\x01"),Bytes.toBytes(startTimeStamp));
    byte[] endRk=Bytes.add(Bytes.toBytesBinary("\\x01"),Bytes.toBytes(endTimeStamp));
    System.out.println("start: "+Bytes.toStringBinary(startRk)+", end: "+Bytes.toStringBinary(endRk));
    scan.setStartRow(startRk);
    scan.setStopRow(endRk);
    scan.addColumn(Bytes.toBytes(Custom_Families[0]),Bytes.toBytes(URL));
    int cacheSize = 10000;
    scan.setBatch(10);
    scan.setCaching(cacheSize);
    ResultScanner scanner = hTable.getScanner(scan);
    int kvSize=0;
    Map<String, Map<String, Integer>> uidUrlCountMap = new HashMap<String, Map<String, Integer>>();
    long transferCost=0,processCost=0,t1=System.currentTimeMillis();
    for (Result result : scanner) {
      for (KeyValue kv : result.raw()) {
        long t2=System.currentTimeMillis();
        byte[] rk = kv.getRow();
        String uid = Bytes.toString(Arrays.copyOfRange(rk, customUidIndex, rk.length));
        String url = Bytes.toString(kv.getValue());
        if(url.startsWith("http://"))
          url=url.substring(7);
        if(url.startsWith("https://"))
          url=url.substring(8);
        if(url.endsWith("/"))
          url=url.substring(0,url.length()-1);
        Map<String, Integer> urlCountMap = uidUrlCountMap.get(uid);
        if (urlCountMap == null) {
          urlCountMap = new HashMap<String, Integer>();
          uidUrlCountMap.put(uid, urlCountMap);
        }
        Integer count = urlCountMap.get(url);
        if (count == null)
          urlCountMap.put(url, new Integer(1));
        else
          urlCountMap.put(url, count + 1);
        kvSize++;
        processCost+=(System.currentTimeMillis()-t2);
      }
      if(kvSize>=500000){
        System.out.println("put to hdfs");
        kvSize=0;
        putToHdfs(uidUrlCountMap,tableName);
        uidUrlCountMap=new HashMap<String, Map<String, Integer>>();
      }
    }
    long totalCost=System.currentTimeMillis()-t1;
    System.out.println("process cost "+processCost+" ms; transfer cost "+(totalCost-transferCost)+" ms");

    putToHdfs(uidUrlCountMap,tableName);
  }

  /*
     get uid category from ad_all_log ;
     transfer category to a url,and give it a count 3.
   */
  public void getAdUidUrl() throws IOException, InterruptedException {
    if(!admin.tableExists(AD_TABLE))
      return;
    HTable hTable = new HTable(conf, AD_TABLE);
    Map<String, List<String>> familyColumns = new HashMap<String, List<String>>();
    List<String> columns = new ArrayList<String>();
    columns.add(CATEGORY);
    familyColumns.put(Ad_Families[0], columns);
    ResultScanner scanner = hTable.getScanner(getScan(familyColumns, true));

    byte[] family = Bytes.toBytes(Ad_Families[0]);
    byte[] categoryColumn = Bytes.toBytes(CATEGORY);
    Map<Integer, String> categoryToUrlMap = new HashMap<Integer, String>();
    categoryToUrlMap.put(new Integer(0), otherUrl);
    categoryToUrlMap.put(new Integer(1), jogosUrl);
    categoryToUrlMap.put(new Integer(2), comprasUrl);
    categoryToUrlMap.put(new Integer(3), friendsUrl);
    categoryToUrlMap.put(new Integer(4), tourismUrl);
    categoryToUrlMap.put(new Integer(99), otherUrl);
    Map<String, Map<String, Integer>> uidUrlCountMap = new HashMap<String, Map<String, Integer>>();
    int kvSize=0;
    for (Result result : scanner) {
      byte[] rk = result.getRow();
      String uid = Bytes.toString(Arrays.copyOfRange(rk, adUidIndex, rk.length));
      int category = Integer.parseInt(Bytes.toString(result.getValue(family, categoryColumn)));
      String url = categoryToUrlMap.get(category);
      Map<String, Integer> urlCountMap = uidUrlCountMap.get(uid);
      if (urlCountMap == null) {
        urlCountMap = new HashMap<String, Integer>();
        uidUrlCountMap.put(uid, urlCountMap);
      }
      Integer count = urlCountMap.get(url);
      if (count == null)
        urlCountMap.put(url, new Integer(8));
      else
        urlCountMap.put(url, count + 8);
      kvSize++;
      if(kvSize>=500000){
        kvSize=0;
        System.out.println("put to hdfs");
        putToHdfs(uidUrlCountMap,"navAd");
        uidUrlCountMap=new HashMap<String, Map<String, Integer>>();
      }
    }
    putToHdfs(uidUrlCountMap,"navAd");
  }

  /*
  public boolean accessOldTable() throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
    long boundTime = format.parse("20140403000000").getTime();
    return (startTimeStamp < boundTime);
  }
  */

  private void putToHdfs(Map<String, Map<String, Integer>> urlCountMap, String flag) throws IOException {
      Path filePath = getFinalPath(flag);
      service.execute(putUrlCount.getRunnable(filePath,urlCountMap));
  }
  /*
  private Path getOutputPath(String project, String flag) throws IOException {

    Path parentDir = new Path(outputBase + File.separator + project + File.separator + startTime + "_" + endTime);
    if (!fs.exists(parentDir)) {
      fs.mkdirs(parentDir);
    }
    Path outputFile = new Path(parentDir, "part-" + flag);
    return outputFile;
  }
  */
  private Path getFinalPath(String flag) throws IOException {
    Path parentDir = new Path(outputBase + File.separator  + startTime + "_" + endTime);
    if (!fs.exists(parentDir)) {
      fs.mkdirs(parentDir);
    }
    Path outputFile = new Path(parentDir, "part-" + flag);
    return outputFile;
  }

  public void shutdown() throws InterruptedException {
    service.shutdown();
    service.awaitTermination(10, TimeUnit.MINUTES);
  }

  public static void main(String[] args) throws Exception {
    /*input has output Path(named with day(hour(minute)))
      if has the second arg,then it is the startTime.the Time should be format of 'yyyyMMddHHmmss';
      if not then set the endTime to currentTime. and the start time should be set to scanUnit before it.
    */
    /*
      first get the length of args.
      if the length <1 or >2 then return;
      if the length =1 then set the endTime and get ScanStartTime
      else if the length=2
              get the first Char of args[1],
              if it is 's', parse to the ScanStartTime and get ScanEndTime
              else if it is 'e',parse to the ScanEndTime and getScanStartTime
    */
     ToolRunner.run(HBaseConfiguration.create(),new Accumulate(),args);

  }

  @Override
  public int run(String[] args) throws Exception {
    addOption(OUTPUT_BASE,"ob","output base dir in hdfs");
    addOption(STARTTIME,"st","start time of url access");
    addOption(ENDTIME,"et","end time of url access");
    if(parseArguments(args)==null)
      return -1;
    init(getOption(OUTPUT_BASE),getOption(STARTTIME),getOption(ENDTIME));
    long t1=System.currentTimeMillis();
    getNavUidUrl();
    getAdUidUrl();
    getCustomUidUrl(PLUGIN_TABLE);
    getCustomUidUrl(YAC_TABLE);
    shutdown();
    long t2=System.currentTimeMillis();
    System.out.println("accumulate cost "+(t2-t1)+" ms");
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
