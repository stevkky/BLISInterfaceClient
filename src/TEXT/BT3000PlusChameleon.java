/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TEXT;




import configuration.configuration;
import configuration.xmlparser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;


/**
 *
 * @author GHSS-BLIS
 */
public class BT3000PlusChameleon extends Thread {
    
     private static List<String> testIDs = new ArrayList<String>();
     static final char Start_Block = (char)2;
     static final char End_Block = (char)3;
     static final char CARRIAGE_RETURN = 13; 
     private static StringBuilder datarecieved = new StringBuilder();
     private boolean stopped = false;
     private static FileTime  ReadTime;
     private static long ReadLine = 1;   
     BufferedReader in=null;   
     
     
    private static final char STX = 0x02;
    private static final char ACK = 0x06;
    private static final char EOT = 0x04;
    private static final char NAK = 0x15;
    private static final char NUL = 0x00;
    private static final char ENQ = 0x05;
    private static final char ETX = 0x03;
    private static final char CR = 0x0D;
     private static final char LF = 0x0A;
    
    
     private static String getFileName()
     {
         return new utilities().getFileName( settings.FILE_NAME_FORMAT,settings.FILE_EXTENSION);
     }
      private static String getSubDIRName()
     {
         return new utilities().getFileName( settings.SUB_DIRECTORY_FORMAT,null);
     }
     
  @Override
    public void run() {
        log.AddToDisplay.Display("BT 3000Plus Chameleon handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking file availability  on this system...", DisplayMessageType.INFORMATION);
        setTestIDs();
        if(system.settings.ENABLE_AUTO_POOL)
         {
            while(!stopped)
            {             
                try {
                    //getBLISTests("",false);
                    manageResults();
                    Thread.sleep(system.settings.POOL_INTERVAL * 1000);
                } catch (InterruptedException ex) {
                    log.logger.PrintStackTrace(ex);
                }
            }
            log.AddToDisplay.Display("BT 3000Plus Chameleon Handler Stopped",log.DisplayMessageType.TITLE);
         }
         else
         {
             log.AddToDisplay.Display("Auto Pull Disabled. Only manual activity can be performed",log.DisplayMessageType.INFORMATION);
         }
       
        
        
      }
    
    private boolean openFile()
    {
        boolean flag = false;
        
         String path = settings.BASE_DIRECTORY 
                 + System.getProperty("file.separator");
                 if(settings.USE_SUB_DIRECTORIES)
                 {
                    path = path + getSubDIRName() + System.getProperty("file.separator");
                 }
                path = path +  getFileName();    
         
         File config_file = new File(path);
        Scanner scanner = null;
        try {
            scanner = new Scanner(config_file);
           flag = true;
        } catch (FileNotFoundException ex) {
            flag = false;
            log.logger.PrintStackTrace(ex);           
        }
        
        return  flag;
    }
    
    public static void HandleDataInput(String data)
    {
        String[] msgParts = data.split(String.valueOf(ETX));
        String pidParts[] = msgParts[1].split("\\|");
        if(pidParts.length > 5)
        {
            String patientid = pidParts[3].trim();
            String SampleID = msgParts[2].split("\\|")[2].trim();
            //SampleID = utilities.getSystemDate("YYYY") + SampleID;
            //SampleID =  patientid;
            int mID=0;
            float value = 0;
            boolean flag = false;
            for(int i=3;i<msgParts.length-1;i++)
            {
                if(msgParts[i].split("\\|")[0].endsWith("R"))
                {
                    mID = getMeasureID(msgParts[i].split("\\|")[2].split("\\^")[3].trim());
                    if(mID > 0)
                    {
                        try
                        {
                            value = Float.parseFloat(msgParts[i].split("\\|")[3]);
                        }catch(NumberFormatException e){
                            try{
                                   continue;
                            }catch(NumberFormatException ex){}

                        }
                        if(SaveResults(SampleID, mID,value))
                        {
                            flag = true;
                        }
                    }
                }
            }
            if(flag)
            {
                 log.AddToDisplay.Display("\nResults with Code: "+SampleID +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
            }
            else
            {
                 log.AddToDisplay.Display("\nTest with Code: "+SampleID +" not Found on BLIS",DisplayMessageType.WARNING);
            }
                           
        }        
           
    }
    
    private List<String> getFileList(String Dir, String name)
    {      
        List<String> FileList = new ArrayList<>();
        File folder = new File(Dir);

        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null)
         for (File listOfFile : listOfFiles) {
             String filename = listOfFile.getName();
             if(listOfFile.isDirectory())
             {
                 if(settings.USE_SUB_DIRECTORIES)
                 {
                    FileList.addAll(getFileList(Dir+System.getProperty("file.separator")+filename,name));
                 }
             }
             else if (filename.endsWith(name)) 
             {
                 FileList.add(Dir+System.getProperty("file.separator")+filename);
             }
         }
         
         return FileList;
    }
    private void manageResults() 
    {
       
        List<String> FileList = new ArrayList<>();
        FileList=getFileList(settings.BASE_DIRECTORY ,getFileName());
        for(int i=0;i<FileList.size();i++)
        {
            // log.AddToDisplay.Display("Working on "+FileList.get(i),DisplayMessageType.INFORMATION);
            File in_file = new File(FileList.get(i));
            String line="";
            String read="";
            try {
                in=new BufferedReader(new InputStreamReader(new FileInputStream(in_file)));
                while((line = in.readLine()) != null)
                {
                    read = read + line;
                }
                
                in.close();
                
                if(!read.isEmpty())
                {
                    HandleDataInput(read);
                    if(settings.DELETE_AFTER_READ)
                    {
                        in_file.delete();
                    }
                }
                
                
            } catch (FileNotFoundException ex) {
                log.logger.PrintStackTrace(ex);
            } catch (IOException ex) {
                log.logger.PrintStackTrace(ex);
            }
            catch (Exception ex)
            {
                log.logger.PrintStackTrace(ex);
            }
        }
        
       }
    
    private boolean shouldRead()
    {
        boolean flag = false;
         String path = settings.BASE_DIRECTORY 
                 + System.getProperty("file.separator")
                 + getSubDIRName()
                 + System.getProperty("file.separator")
                 + getFileName();         

        Path file = Paths.get(path);
         try {           
             BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
             if(null == ReadTime || (attr.lastModifiedTime().compareTo(ReadTime) > 0))
             {
                 flag = true;
                 
             }            
             else
             {
                 flag = false;
             }
                 
             
         } catch (IOException ex) {
             log.logger.PrintStackTrace(ex);
         }
        
        return flag;
    }   
   
    
    public void Stop()
    {
    
         log.AddToDisplay.Display("Stoping handler", log.DisplayMessageType.TITLE);
         
         stopped = true;           
         this.interrupt();
        /*if(Manager.closeOpenedPort())
        {
            log.AddToDisplay.Display("Port Closed sucessfully", log.DisplayMessageType.INFORMATION);
        }*/
    }
    
    private void setTestIDs()
     {
         String equipmentid = getSpecimenFilter(3);
         String blismeasureid = getSpecimenFilter(4);
        
         String[] equipmentids = equipmentid.split(",");
         String[] blismeasureids = blismeasureid.split(",");
         for(int i=0;i<equipmentids.length;i++)
         {
             testIDs.add(equipmentids[i]+";"+blismeasureids[i]);             
         }
        
     }
    
    private static String getSpecimenFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/BT3000Plus/bt3000pluschameleon.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
             log.logger.PrintStackTrace(ex);
        }        
        return data;        
    }
    
     private static int getMeasureID(String equipmentID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID))
             {
                 measureid = Integer.parseInt(testIDs.get(i).split(";")[1]);
                 break;
             }
         }
         
         return measureid;
     }
     
    private static boolean SaveResults(String barcode,int MeasureID, float value)
     {
         
         
          boolean flag = false;       
          if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value,0)))
           {
              flag = true;
            }
                          
         return flag;
         
     }    

    

}
