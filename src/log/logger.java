/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package log;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import system.settings;
/**
 *
 * @author BLIS
 */
public class logger {
    
    public synchronized static void Logger(String log)
    {
         if(!settings.ENABLE_LOG)
                return;
        try
        {           
            PrintWriter printWriter;
            try (FileWriter fileWriter = new FileWriter(new File("BLISInterface"+ getLogDate() +".log"), true)) {
                printWriter = new PrintWriter(fileWriter);
                printWriter.println(getCurrentTimeStamp()+": "+log);
            }
            printWriter.close();
        }
        catch(Exception ex) { }
    }
    
    public synchronized static void PrintStackTrace(Exception e)
    {
         if(!settings.ENABLE_LOG)
                return;
        try
        {           
            PrintWriter printWriter;
            try (FileWriter fileWriter = new FileWriter(new File("BLISInterface"+ getLogDate() +".log"), true)) {
                printWriter = new PrintWriter(fileWriter);
                printWriter.println(getCurrentTimeStamp());
                e.printStackTrace(printWriter);                
            }
            printWriter.close();
        }
        catch(Exception ex) { }
    }
    
    public  static String getCurrentTimeStamp() 
    {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }   
    
    private static String getLogDate()
    {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
        Date now = new Date();
        return sdfDate.format(now);  
    }
}
