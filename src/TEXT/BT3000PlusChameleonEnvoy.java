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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


/**
 *
 * @author GHSS-BLIS
 */
public class BT3000PlusChameleonEnvoy extends Thread {
    
     
     private static List<String> testIDs = new ArrayList<String>();
     private static List<String> calctestIDs = new ArrayList<String>();
     static final char Start_Block = (char)2;
     static final char End_Block = (char)3;
     static final char CARRIAGE_RETURN = 13; 
     private static StringBuilder datarecieved = new StringBuilder();
     private boolean stopped = false;
     private static FileTime  ReadTime;
     private static long ReadLine = 1;   
     BufferedReader in=null;   
     private static int TestStart;
    
    
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
        log.AddToDisplay.Display("BT 3000Plus Envoy handler started...", DisplayMessageType.TITLE);
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
        data = data.trim();        
         if(null == data || data.isEmpty())
            return;
         
          log.AddToDisplay.Display("New Data Read fom file",DisplayMessageType.TITLE);
          log.AddToDisplay.Display(data,DisplayMessageType.INFORMATION);
         
        String[] DataParts = data.split(String.valueOf(settings.SEPERATOR_CHAR));       
        if(DataParts.length > 13)
        {
                       
            int mID=0;
            float value = 0;
            boolean flag = false;                           
            String specimen_id = DataParts[0].trim();

            mID = getMeasureID(DataParts[8].trim());
            if(mID > 0)
            {
                try
                {
                    value = Float.parseFloat(DataParts[14].trim());
                }catch(NumberFormatException e){
                    try{
                    value = 0;
                    }catch(NumberFormatException ex){}

                }
                PrepareCalcTests(DataParts[8].trim(),value);
                if(SaveResults(specimen_id, mID,value))
                {
                    flag = true;
                }
            }

            if(flag)
            {
                 log.AddToDisplay.Display(DataParts[8].trim()+ " with Code: "+specimen_id +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
            }
            else
            {
                 log.AddToDisplay.Display(DataParts[8].trim() + " with Code: "+specimen_id +" not Found on BLIS",DisplayMessageType.WARNING);
            }
                
            
        }                           
           
           
    }
    
    private static void PrepareCalcTests(String equipmentID, float value)
     {
        // int measureid = 0;
         List<String> temp = new ArrayList<>();
         
         for(int i=0;i<calctestIDs.size();i++)
         {
             String temp2=calctestIDs.get(i).replaceAll(equipmentID, String.valueOf(value)); 
             temp.add(temp2);                        
             
         }  
         calctestIDs = temp;
        
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
            try 
            {
                
                in=new BufferedReader(new InputStreamReader(new FileInputStream(in_file)));
                while((line = in.readLine()) != null)
                {
                   HandleDataInput(line);
                }                
                in.close();
                                  
                if(settings.DELETE_AFTER_READ)
                {
                    in_file.delete();
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
    
    private void ManageCalculatedTest(String SpecimenID)
    {
        int mID=0;
        float value = 0;
        boolean flag = false;                           
        String specimen_id = SpecimenID;
            
        for(int i=0;i<calctestIDs.size();i++)
                    {
                        mID = getMeasureID(calctestIDs.get(i).split(";")[0].trim());
                        if(mID > 0)
                        {
                            try
                            {
                                ScriptEngineManager mgr = new ScriptEngineManager();
                                ScriptEngine engine = mgr.getEngineByName("JavaScript");                               
                                
                                value = Float.parseFloat(String.valueOf(engine.eval(calctestIDs.get(i).split(";")[1])));
                            }catch(NumberFormatException e){
                                value = 0;
                            } catch (ScriptException ex) {
                                log.logger.Logger("Could not perform calculation:"+calctestIDs.get(i).split(";")[1]);
                                //log.logger.PrintStackTrace(ex);
                                value = 0;
                            }
                            //PrepareCalcTests(DataParts[i].trim(),value);
                            if(SaveResults(specimen_id, mID,value))
                            {
                                flag = true;
                            }
                        }                        

                    }  
                    if(flag)
                    {
                         log.AddToDisplay.Display("Results with Code: "+specimen_id +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
                    }
                    else
                    {
                         log.AddToDisplay.Display("Test with Code: "+specimen_id +" not Found on BLIS",DisplayMessageType.WARNING);
                    }
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
        xmlparser p = new xmlparser("configs/BT3000Plus/bt3000plus.xml");
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
