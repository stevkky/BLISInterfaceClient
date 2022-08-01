/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package system;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GHSS-BLIS
 */
public class utilities 
{
    public static String getSystemDate(String Format)
    {
        SimpleDateFormat sdfDate = new SimpleDateFormat(Format);
        Date now = new Date();
        return sdfDate.format(now);       
    }
    
    public static String formatAsDate(String format, String date)
    {
        try { 
            
            Date date1 = new SimpleDateFormat(format).parse(date);
            
            return  new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date1);
            
        } catch (Exception ex) {
            Logger.getLogger(utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        return LocalDateTime.now().toString();
    }
    public static String getHL7Date(String Date,String PDate, String Format)
    {
       try
       {
            if(Date != null)
            {
                String[] parts = Date.split("-");
                return parts[0]+  parts[1]+ parts[2]+ "000000"; 
            }
            else
            {    
                String[] parts = PDate.split("-");
                return parts[0]+  parts[1]+ parts[2]+ "000000";   
            }
       }catch(Exception ex){
       
           return "00000000000000";
       }
    }
    
     public static String getHL7DateOnly(String Date,String PDate)
    {
       try
       {
            if(Date != null)
            {
                String[] parts = Date.split("-");
                return parts[0]+  parts[1]+ parts[2]; 
            }
            else
            {    
                String[] parts = PDate.split("-");
                return parts[0]+  parts[1]+ parts[2];   
            }
       }catch(Exception ex){
       
           return "00000000";
       }
    }
    
    public static String getNormalizedDate(String Date,String PDate)
    {
       try
       {
            if(Date != null)
            {
                return Date;
            }
            else
            {    
                return PDate;
            }
       }catch(Exception ex){
       
           return "0000-00-00";
       }
    }
    
    public static synchronized void writetoFile(String data)
    {
        if(settings.WRITE_TO_FILE)
         {
            try
            {           
                PrintWriter printWriter;
                try (FileWriter fileWriter = new FileWriter(new File("BLISInterfaceDataInput.txt"), true)) {
                    printWriter = new PrintWriter(fileWriter);
                    printWriter.print(data);

                }
                printWriter.close();
            }
            catch(Exception ex) { }

         }
    }
    
    public static String getCheckSum(String data)
    {
        int checksum =0;
        
        for(int i=0;i<data.length();i++)
        {
            checksum = checksum + (int)data.charAt(i);          
        }        
        checksum = checksum % 256;
        String hex = Integer.toHexString(checksum).toUpperCase();
        if(hex.length()== 1)
            hex = "0"+hex;
        return hex;
    }
    
}
