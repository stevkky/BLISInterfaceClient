/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package RS232;


import configuration.xmlparser;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;
import system.settings;
import system.utilities;


/**
 *
 * @author BLIS
 */
public class SYSMEXKX21N extends Thread {
    
     
     private static List<String> testIDs = new ArrayList<String>();
     static final char Start_Block = (char)2;
     static final char End_Block = (char)3;
     static final char CARRIAGE_RETURN = 13;
     static final char SUB = 0x1A; 
     private static StringBuilder datarecieved = new StringBuilder();     
    
  @Override
    public void run() {
        log.AddToDisplay.Display("SYSMEX KX-21N handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }            
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       if(Manager.openPortforData("SYSMEX KX-21N"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);   
           setTestIDs();
       }      
            
    }
    
    public static void HandleDataInput(String data)
    {  
        if(data.charAt(0) == Start_Block)
        {
            datarecieved = new StringBuilder();                
        }
        if(data.contains(String.valueOf(End_Block)))
        {
            int endindex = data.indexOf(String.valueOf(End_Block));
            datarecieved.append(data.substring(0, endindex));
            processMessage();
            datarecieved = new StringBuilder();
            if(data.substring(endindex).length()> 1)
            {
                if(data.startsWith(String.valueOf(End_Block)))
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
    
    private static String[] normalizeData(String data)
    {       
        String[] normformat = {"###.##","##.###","###.##","###.##",
            "###.##","###.##","###.##","####.#","###.##",
            "###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##","###.##"};
        
         DecimalFormat myFormatter;
      
        String[] norm = new String[19];
       // int i=13;
        for(int i=0,start=0;i<data.length();i+=5,start++)
        {           
            norm[start] = customFormat(data.substring(i, i+5),normformat[start]);
            //norm[start] = myFormatter.format(data.substring(indstart, indstart+normkeys[i])));          
            
        }
                
        return norm;
        
    }
    
    private static String customFormat(String value, String pattern)
    {
        String formated ="";
       int ind = 0;
       try
       {
           formated = String.valueOf(Integer.parseInt(value));
           for(int i = pattern.length()-1,in=0;i>=0;i--,in++)
           {
               if(pattern.charAt(i) == '.')
               {
                   ind = in;
                   break;
               }
           }

            if (ind > 0)
            {
                 for(int i = value.length()-1,in=0;i>=0;i--,in++)
                 {
                    if(in == ind)
                    {
                        formated = value.substring(0,i+1)+"."+value.substring(i+1);
                        formated =String.valueOf(Float.parseFloat(formated));
                        break;
                    }

                 }
            }
       }catch(NumberFormatException ex){ formated = "-";}
          
        
        return formated;
    }
    
    private static String getCorrectSpecimenID(String raw)
    {
        String ID = "";
        //String temp =raw;      
        while(true)
        {
            if(raw.startsWith("0"))            
                raw = raw.substring(1);              
            else
                break;
        }
       
        if(settings.AUTO_SPECIMEN_ID)
        {
            if(raw.length() == 9)
                
                ID = utilities.getSystemDate("yyyy").substring(0,2) + raw;
            else
                ID = raw;
        }
        
        return ID;
    }
    private static void processMessage()
    {
         
        if(null == datarecieved.toString() || datarecieved.toString().isEmpty())
            return;
               
        String DataTrimed = datarecieved.toString().substring(35);
                 
            int mID=0;
            float value = 0;
            boolean flag = false;
                           
                    String specimen_id = datarecieved.toString().substring(13,28);
                    specimen_id = getCorrectSpecimenID(specimen_id);
                    
                    String[] DataParts = normalizeData(DataTrimed);
                    
                    for(int i=0;i<DataParts.length;i++)
                    {
                        mID = getMeasureID(i+1);
                        if(mID > 0)
                        {
                            try
                            {
                                value = Float.parseFloat(DataParts[i].trim());
                            }catch(NumberFormatException e){
                                try{
                                continue;
                                }catch(NumberFormatException ex){}
                            
                            }
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
        xmlparser p = new xmlparser("configs/SYSMEX/SYSMEXKX21N.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MICROS60.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }
    
     private static int getMeasureID(int equipmentID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID+""))
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
