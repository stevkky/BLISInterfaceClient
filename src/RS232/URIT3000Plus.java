/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package RS232;


import configuration.xmlparser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;


/**
 *
 * @author BLIS
 */
public class URIT3000Plus extends Thread {
    
     
     private static List<String> testIDs = new ArrayList<String>();
     static final char Start_Block = (char)2;
     static final char End_Block = (char)3;
     static final char CARRIAGE_RETURN = 13; 
     static final char VT = 0x0B;
     static final char FS = 0x1C;
     private static StringBuilder datarecieved = new StringBuilder();
    
  @Override
    public void run() {
        log.AddToDisplay.Display("URIT 3000Plus handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }            
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       
       
       if(Manager.openPortforData("URIT-3000Plus"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);   
           setTestIDs();
       }      
      
    }
    
    public static void HandleDataInput(String data)
    {
        synchronized(datarecieved)
        { 
            if(data.charAt(0) == VT)
            {
                datarecieved = new StringBuilder(); 
                log.logger.Logger("Started with VT");
            }
            if(data.contains(String.valueOf(FS)))
            {
                log.logger.Logger("Started with FS");
                int endindex = data.indexOf(String.valueOf(FS));
                datarecieved.append(data.substring(0, endindex));
                Manager.ResetPort();
                processMessage();
                datarecieved = new StringBuilder();
                if(data.substring(endindex).length()> 1)
                {
                     log.logger.Logger("Will call handler again..");
                    if(data.startsWith(String.valueOf(FS)))
                        HandleDataInput(data.substring(endindex+2));
                    else                        
                        HandleDataInput(data.substring(endindex));
                }
            }
            else
            {
                datarecieved.append(data);
            }
            

        }
            /*if(data.charAt(0) == VT)
            {
                datarecieved = new StringBuilder(); 
                log.logger.Logger("Started with VT");
            }
            if(data.contains(String.valueOf(FS)))
            {
                log.logger.Logger("Started with FS");
                int endindex = data.indexOf(String.valueOf(FS));
                datarecieved.append(data.substring(0, endindex));
                processMessage();
                datarecieved = new StringBuilder();
                if(data.substring(endindex).length()> 1)
                {
                     log.logger.Logger("Will call handler again..");
                    if(data.startsWith(String.valueOf(FS)))
                        HandleDataInput(data.substring(endindex+2));
                    else                        
                        HandleDataInput(data.substring(endindex));
                }
            }
            else
            {
                datarecieved.append(data);
            }
            */
       
           /* if(data.charAt(0) == VT)
            {
                datarecieved = new StringBuilder();
               
            }
            datarecieved.append(data);
            if(data.charAt(data.length()-1) == FS)
            {
                log.logger.Logger("End text with FS");
                processMessage();
            }  */       
       
           
    }
    private static void processMessage()
    {
        if(datarecieved.toString().length() < 1000)
        {
            return;
        }        
        
        log.logger.Logger("Now processing");
        String[] DataParts = datarecieved.toString().split("\\r");
        
        String pidParts[] = DataParts[1].split("\\|");
        if(DataParts.length > 20)
        {
            String Type  = pidParts[0].trim();
            int mID=0;
            float value = 0;
            boolean flag = false;
            if(Type.startsWith("PID"))//Only consider result values
            {
               String patientid = pidParts[2].trim();
               String SampleID = patientid ;   
               
                for(int i=3;i<DataParts.length;i++)
                {
                    if(DataParts[i].split("\\|")[0].endsWith("OBX") && !DataParts[i].split("\\|")[3].contains("^"))
                    {
                        mID = getMeasureID(DataParts[i].split("\\|")[3].trim());
                        if(mID > 0)
                        {
                            try
                            {
                                value = Float.parseFloat(DataParts[i].split("\\|")[5]);
                            }catch(NumberFormatException e){
                                continue;
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
                     log.AddToDisplay.Display("Results with Code: "+SampleID +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
                }
                else
                {
                     log.AddToDisplay.Display("Test with Code: "+SampleID +" not Found on BLIS",DisplayMessageType.WARNING);
                }
                
            }
            else
            {
                log.logger.Logger("Type do not start with PID its: "+Type);
            }
        }
        else
        {
            log.logger.Logger("Data length is less than 20: "+DataParts.length);
        }
       
    }
    
    public void Stop()
    {
        if(Manager.closeOpenedPort())
        {
            log.AddToDisplay.Display("Port Closed sucessfully", log.DisplayMessageType.INFORMATION);
        }
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
        xmlparser p = new xmlparser("configs/URIT/urit3000plus.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MICROS60.class.getName()).log(Level.SEVERE, null, ex);
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
