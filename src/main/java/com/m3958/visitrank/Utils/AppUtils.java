package com.m3958.visitrank.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.platform.Container;

import com.m3958.visitrank.AppConstants;
import com.m3958.visitrank.interf.TestableVerticle;
import com.mongodb.DB;

public class AppUtils {

  public static String getDailyDbName(String filename, Pattern dailyDbPtn) {
    Matcher m = dailyDbPtn.matcher(filename);
    if (m.matches()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  public static String getHour(String filename, Pattern dailyDbPtn) {
    Matcher m = dailyDbPtn.matcher(filename);
    if (m.matches()) {
      return m.group(2);
    } else {
      return null;
    }
  }

  // public static long getLastPartialPosition(Path partialLogPath) {
  // String[] lines = new FileLineReader(partialLogPath.toString()).getLastLines(1);
  // if (lines.length == 1) {
  // String[] ss = lines[0].split(",");
  // if (ss.length == 2) { // 1000,1000 means 1000 has write to mongodb.
  // return Long.parseLong(ss[0], 10);
  // } else { // 1000, means 1000 - gap to 1000 has not write to mongodb.
  // return Long.parseLong(ss[0], 10) - AppConstants.LOGFILE_READ_GAP;
  // }
  // }
  // return 0;
  // }

  /**
   * this method record everything from client.
   * 
   * @param req
   * @return
   */
  public static JsonObject getParamsHeadersOb(HttpServerRequest req) {
    JsonObject jo = new JsonObject();

    JsonObject headerJo = new JsonObject();

    for (Map.Entry<String, String> header : req.headers().entries()) {
      String key = header.getKey();
      String value = header.getValue();
      if ("referer".equalsIgnoreCase(key)) {
        jo.putString(FieldNameAbbreviation.PageVisit.URL, value);
      } else {
        headerJo.putString(key, value);
      }
    }

    for (Map.Entry<String, String> param : req.params().entries()) {
      String key = param.getKey();
      String value = param.getValue();
      jo.putString(key, value);
    }
    headerJo.putString("ip", req.remoteAddress().getAddress().getHostAddress());
    jo.putNumber(FieldNameAbbreviation.PageVisit.TS, new Date().getTime()).putObject("headers",
        headerJo);

    return jo;
  }

  public static boolean colExist(AppConfig appConfig, String dbname, String colname) {
    DB db = appConfig.getMongoClient().getDB(dbname);
    Set<String> cols = db.getCollectionNames();
    for (String c : cols) {
      if (colname.equals(c)) {
        return true;
      }
    }
    return false;
  }

  public static boolean DbExists(AppConfig appConfig, String dbname) throws UnknownHostException {
    List<String> dbns = appConfig.getMongoClient().getDatabaseNames();
    boolean exist = false;
    for (String db : dbns) {
      if (db.equals(dbname)) {
        exist = true;
        break;
      }
    }
    return exist;
  }


  public static String toUtf(String wrongs) throws UnsupportedEncodingException {
    char[] buffer = wrongs.toCharArray();
    byte[] b = new byte[buffer.length];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) buffer[i];
    }
    return new String(b, "UTF-8");
  }

  public static void moveLogFiles(String logDir, String archiveDir, String filename,
      Path logfilePath) throws IOException {
    Path archiedPath = Paths.get(archiveDir);
    if (!archiedPath.toFile().exists()) {
      Files.createDirectories(archiedPath);
    }

    Path tp = archiedPath.resolve(filename);
    while (Files.exists(tp)) {
      tp = Paths.get(tp.toString() + ".duplicated");
    }

    Files.move(logfilePath, tp);

    if (Files.exists(Paths.get(logDir, filename + AppConstants.PARTIAL_POSTFIX),
        LinkOption.NOFOLLOW_LINKS)) {
      Files.delete(Paths.get(logDir, filename + AppConstants.PARTIAL_POSTFIX));
    }
  }

  public static Comparator<String> longFirstStringComparator() {
    return new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        if (o1.length() > o2.length()) {
          return 1;
        } else if (o1.length() < o2.length()) {
          return -1;
        } else {
          return o1.compareTo(o2);
        }
      }
    };
  }

  @SuppressWarnings("rawtypes")
  public static List<String> loadResourceLines(Class clazz, String fn) {
    if (!fn.startsWith("/")) {
      fn = "/" + fn;
    }
    try {
      InputStream is = clazz.getResourceAsStream(fn);
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      List<String> lines = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
      return lines;
    } catch (IOException e) {}
    return null;
  }

  public static List<String> loadFileLines(String fn) {
    return loadFileLines(Paths.get(fn));
  }

  public static List<String> loadFileLines(Path p) {
    try {
      return Files.readAllLines(p, Charset.forName("UTF-8"));
    } catch (IOException e) {
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  public static String loadResourceContent(Class clazz, String fn) {
    List<String> lines = AppUtils.loadResourceLines(clazz, fn);
    if (lines == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line);
    }
    return sb.toString();
  }

  @SuppressWarnings("rawtypes")
  public static JsonObject loadJsonResourceContent(Class clazz, String fn) {
    List<String> lines = loadFileLines(fn);
    if (lines == null) {
      lines = AppUtils.loadResourceLines(clazz, fn);
    }
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line);
    }
    return new JsonObject(sb.toString());
  }

  @SuppressWarnings("rawtypes")
  public static Map<String, String> getMrFunctions(AppConfig appConfig, Class clazz, String fn) {
    Path p = Paths.get(appConfig.getMrFuncFolder(), fn);
    List<String> lines = loadFileLines(p);
    if (lines == null) {
      lines = loadResourceLines(clazz, fn);
    }
    try {
      lines = Files.readAllLines(p, Charset.forName("UTF-8"));
    } catch (IOException e) {
      lines = new ArrayList<>();
    }
    return getMrFunctions(lines);
  }

  private static Map<String, String> getMrFunctions(List<String> lines) {
    StringBuilder mapfunc = new StringBuilder();
    StringBuilder reducefunc = new StringBuilder();
    StringBuilder finalizefunc = new StringBuilder();
    char s = 0;
    for (String line : lines) {
      if ("//mapfunc:".equals(line)) {
        s = 'm';
      } else if ("//reducefunc:".equals(line)) {
        s = 'r';
      } else if ("//finalizefunc:".equals(line)) {
        s = 'f';
      }

      switch (s) {
        case 'm':
          mapfunc.append(line).append(System.lineSeparator());
          break;
        case 'r':
          reducefunc.append(line).append(System.lineSeparator());
          break;
        case 'f':
          finalizefunc.append(line).append(System.lineSeparator());
          break;
        default:
          break;
      }
    }
    Map<String, String> map = new HashMap<String, String>();
    map.put(AppConstants.MapReduceFunctionName.MAP, mapfunc.toString());
    map.put(AppConstants.MapReduceFunctionName.REDUCE, reducefunc.toString());
    map.put(AppConstants.MapReduceFunctionName.FINALIZE, finalizefunc.toString());
    return map;
  }

  public static void logMongoDbConnectionError(Logger log) {
    log.error("mongodb connection failure.");
  }

  public static boolean deployTestableVerticle(TestableVerticle tv, Container c) {
    JsonObject testConfig = c.config();
    final Logger log = c.logger();
    if (testConfig.containsField(AppConstants.TEST_CONF_KEY)) {
      final AppConfig gcfg = new AppConfig(testConfig.getObject(AppConstants.TEST_CONF_KEY), true);
      tv.deployMe(gcfg, log);
      log.info("use test conf.");
      return true;
    } else {
      return false;
    }
  }
  
  public static void recordDeployed(SharedData sd,String name,String id){
    ConcurrentMap<String,String> deployedMap = sd.getMap(AppConstants.DEPLOYED_SHARED_MAP);
    String ids = deployedMap.get(name);
    if(ids == null){
      ids = "";
    }
    ids = ids.length() > 0 ? ids + "," + id : id;
    deployedMap.put(name, ids);
  }

}
