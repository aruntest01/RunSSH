package net.neoremind.sshxcute.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Logger
{
  private static Logger instance = null;
  private static String pathToFiles;
  private static String instanceName;
  private static String logFile;
  private static String dbgFile;
  private static Map<String, String> classNames;
  private static int classCount;
  private static int logThreshold = 4;
  public static final int DIVIDER = 0;
  public static final int DEEP_DBUG = 1;
  public static final int MID_DBUG = 2;
  public static final int LITE_DBUG = 3;
  public static final int INFO = 4;
  public static final int WARN = 5;
  public static final int ERROR = 6;
  public static final int FATAL = 7;
  public static final int OFF = 99;
  
  public static Logger getLogger()
  {
    try
    {
      instance = new Logger(getCaller());
    }
    catch (Exception e)
    {
      return null;
    }
    instance.setThreshold(4);
    return instance;
  }
  
  public void setThreshold(int newThreshold)
  {
    setThreshold(getCaller(), newThreshold);
  }
  
  public void setThreshold(String caller, int newThreshold)
  {
    
    logThreshold = newThreshold;
  }
  
  public String getLogpath()
  {
    return pathToFiles;
  }
  
  public void close()
  {
    if (logThreshold < 4)
    {
      printDbg("----Classes----------------------", true);
      Set<String> keys = classNames.keySet();
      Iterator<String> it = keys.iterator();
      while (it.hasNext())
      {
        String k = (String)it.next();
        String v = (String)classNames.get(k);
        printDbg(v + " = " + k, true);
      }
    }
  }
  
  public void clearLogger()
  {
    clearLogger(getCaller());
  }
  
  public void clearLogger(String caller)
  {
    DateCalendar d = new DateCalendar();
    String outp = "\n--------------- [" + d.toTimeStamp() + ", " + caller + "] --- CLEARED ----------\n";
    printLog(outp, false);
    if (logThreshold < 4)
    {
      printDbg(outp, false);
    }
    else
    {
      File f = new File(dbgFile);
      f.delete();
    }
  }
  
  public void putMsg(int level, String msg)
  {
    if (level == 7)
    {
      DateCalendar d = new DateCalendar();
      String t = levelToString(level) + ":       ";
      String outp = "[" + d.timeString() + "] " + t.substring(0, 9) + msg;
      printLog(outp, true);
      if (logThreshold < 4) {
        printDbg(outp, true);
      }
    }
    else if ((level >= logThreshold) || (level == 0))
    {
      putMsg(getCaller(), level, msg);
    }
  }
  
  public void putMsg(String caller, int level, String msg)
  {
    if ((level >= logThreshold) || (level == 0))
    {
      DateCalendar d = new DateCalendar();
      if (level == 0)
      {
        String outp = "\n=============== [" + d.toTimeStamp() + ", " + caller + "] ====================\n";
        printLog(outp, true);
        if (logThreshold < 4) {
          printDbg(outp, true);
        }
      }
      else if (level < 4)
      {
        caller = shortHandCaller(caller);
        String outp = "[" + d.timeString() + " - " + caller + "] " + msg;
        printDbg(outp, true);
      }
      else
      {
        String t = levelToString(level) + ":       ";
        String outp = "[" + d.timeString() + "] " + t.substring(0, 9) + msg;
        printLog(outp, true);
//        System.out.println(msg);
      }
    }
  }
  
  public static synchronized String getCaller()
  {
    String[] callStack = getCallStackAsStringArray();
    for (int i = 0; i < callStack.length; i++) {
      if (callStack[i].contains("getCaller:")) {
        return callStack[(i + 2)];
      }
    }
    return callStack[(callStack.length - 1)];
  }
  
  public static int levelFmString(String level)
  {
    int lvl;
    if (level.equals("DEEP_DBUG"))
    {
      lvl = 1;
    }
    else
    {
      if (level.equals("MID_DBUG"))
      {
        lvl = 2;
      }
      else
      {
        if (level.equals("LITE_DBUG"))
        {
          lvl = 3;
        }
        else
        {
          if (level.equals("INFO"))
          {
            lvl = 4;
          }
          else
          {
            if (level.equals("WARN"))
            {
              lvl = 5;
            }
            else
            {
              if (level.equals("FATAL"))
              {
                lvl = 7;
              }
              else
              {
                if (level.equals("OFF")) {
                  lvl = 99;
                } else {
                  lvl = 6;
                }
              }
            }
          }
        }
      }
    }
    return lvl;
  }
  
  public static String levelToString(int level)
  {
    String result;
    switch (level)
    {
    case 1: 
      result = "DEEP_DBUG"; break;
    case 2: 
      result = "MID_DBUG"; break;
    case 3: 
      result = "LITE_DBUG"; break;
    case 4: 
      result = "INFO"; break;
    case 5: 
      result = "WARN"; break;
    case 7: 
      result = "FATAL"; break;
    case 99: 
      result = "OFF"; break;
    case 6: 
    default: 
      result = "ERROR:   ";
    }
    return result;
  }
  
  private Logger(String caller)
    throws Exception
  {
    if (instance != null)
    {
      instance.close();
      instance = null;
    }
    classNames = new HashMap<String, String>();
    classCount = 0;
    instanceName = "sshxcute";
    
    pathToFiles = System.getProperty("user.dir");
    
    logFile = pathToFiles + "/" + instanceName + ".log";
    dbgFile = pathToFiles + "/" + instanceName + ".dbg";
    putMsg(caller, 0, "");
  }
  
  private String shortHandCaller(String inp)
  {
    int pos = inp.lastIndexOf("/");
    if (pos == -1)
    {
      pos = inp.lastIndexOf(".");
      String prefix = inp.substring(0, pos);
      pos = prefix.lastIndexOf(".");
    }
    String prefix = inp.substring(0, pos);
    String shortName = (String)classNames.get(prefix);
    if (shortName == null)
    {
      classCount += 1;
      shortName = "#" + classCount;
      classNames.put(prefix, shortName);
    }
    String caller = shortName + "/" + inp.substring(pos + 1);
    return caller;
  }
  
  private synchronized void printLog(String msg, boolean append)
  {
    try
    {
      FileWriter w = new FileWriter(logFile, append);
      w.write(msg + "\n");
      w.flush();
      w.close();
    }
    catch (IOException e)
    {
      System.out.println("IOException in Logger, message: " + e.getMessage());
      System.out.println(msg);
    }
  }
  
  private synchronized void printDbg(String msg, boolean append)
  {
    try
    {
      FileWriter w = new FileWriter(dbgFile, append);
      w.write(msg + "\n");
      w.flush();
      w.close();
    }
    catch (IOException e)
    {
      System.out.println("IOException on debug message: " + e.getMessage());
      System.out.println("DEBUG: " + msg);
    }
  }
  
  private static synchronized String[] getCallStackAsStringArray()
  {
    ArrayList<String> list = new ArrayList<String>();
    String[] array = new String[1];
    StackTraceElement[] stackTraceElements = 
      Thread.currentThread().getStackTrace();
    for (int i = 0; i < stackTraceElements.length; i++)
    {
      StackTraceElement element = stackTraceElements[i];
      String classname = element.getClassName();
      String methodName = element.getMethodName();
      int lineNumber = element.getLineNumber();
      String entry = classname + "." + methodName + ":" + lineNumber;
      list.add(entry);
    }
    return (String[])list.toArray(array);
  }
  
  private class DateCalendar
    extends GregorianCalendar
  {
    private static final long serialVersionUID = -98734585L;
    
    public DateCalendar() {}
    
    public int get(int field)
    {
      int result = field == 2 ? super.get(field) + 1 : super.get(field);
      return result;
    }
    
    public String toString()
    {
      int year = get(1);
      int month = get(2);
      int day = get(5);
      int hour24 = get(11);
      int min = get(12);
      int sec = get(13);
      
      String datetimeString = String.valueOf(year) + "." + String.valueOf(month) + "." + String.valueOf(day) + 
        "." + String.valueOf(hour24) + "." + String.valueOf(min) + "." + String.valueOf(sec);
      
      return datetimeString;
    }
    
    public String timeString()
    {
      return toTimeStamp().substring(11);
    }
    
    public String toTimeStamp()
    {
      String year = String.valueOf(get(1));
      String month = String.valueOf(get(2));
      String day = String.valueOf(get(5));
      String hour24 = String.valueOf(get(11));
      String min = String.valueOf(get(12));
      String sec = String.valueOf(get(13));
      if (month.length() == 1) {
        month = "0" + month;
      }
      if (day.length() == 1) {
        day = "0" + day;
      }
      if (hour24.length() == 1) {
        hour24 = "0" + hour24;
      }
      if (min.length() == 1) {
        min = "0" + min;
      }
      if (sec.length() == 1) {
        sec = "0" + sec;
      }
      String datetimeString = year + "/" + month + "/" + day + " " + hour24 + ":" + min + ":" + sec;
      
      return datetimeString;
    }
  }
}
