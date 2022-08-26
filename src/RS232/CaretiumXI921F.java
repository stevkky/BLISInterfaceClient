/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package RS232;


import configuration.xmlparser;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;


/**
 *
 * @author BLIS
 */
public class CaretiumXI921F extends Thread {
    
    public enum  MSGMODE
    {
        WAIT_FOR_ACK(0),
        ACK_RECEIVED(1),
        IDLE(2),
        WORKING(3),
        PATIENT_SENT(4),      
        PATIENT_SENT_ACK(5),
        PATIENT_SENT_ERR(6),
        RESULTS_REQUESTED(7),
        RESULTS_RECEIVING(8),
        RESULTS_RECEIVED(9),
        NO_RESULTS(10);
        
        private MSGMODE(int value)
        {
            this.Value = value;
        }
        
        private int Value;
        
    }
   
    
     private static List<String> testIDs = new ArrayList<>();
     static final char Start_Block = (char)2;
     static final char End_Block = 0x03;
     static final char CARRIAGE_RETURN = 13; 
     private static final char STX = 0x02;
     private static final char RSEP = 0x1E;
     private static final char GSEP = 0x1D;
     private static final char ACK = 0x06;
     private static final char EOT = 0x04;
     private static final char NAK = 0x15;
     private static final char NUL = 0x00;
     private static final char ENQ = 0x05;
     private static final char EXC = 0x10;
    // private static boolean AckWait = false;
    // private static boolean CommInit = false;
     
     private static StringBuilder datarecieved = new StringBuilder();
     private boolean stopped = false;
     
     private static MSGMODE appState = MSGMODE.IDLE;
    
  @Override
    public void run() {
        log.AddToDisplay.Display("Caretium XI-921F  handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }            
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       if(Manager.openPortforData("Caretium XI-921F"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);   
           setTestIDs();
       }
       
       
       //setTestIDs();
       //HandleDataInput("001  000000020220825009  007 58.7 ");
       //HandleDataInput(" 51.0    53.1     0.00 0.00   0.0");
      
    }
    
    public static  boolean sendToAnalyzer(String data)
    {
        boolean flag = false;  
        
        try
        {
           flag = Manager.writeToSerialPort(data);
           
        }catch(Exception ex)
        {
            flag = false;
            log.AddToDisplay.Display(ex.getMessage(), log.DisplayMessageType.ERROR);
        }
         
        return flag;
    }
    
    
    public static void HandleDataInput(String data)
    {
       datarecieved.append(data);
       int datalength = datarecieved.toString().trim().split("\\s+").length;
       if(datalength >=9)
       {
            StringJoiner j = new StringJoiner(" ");
            String[] parts = datarecieved.toString().trim().split("\\s+");
            datarecieved = new StringBuilder();
            for(int p=0;p<datalength;p++)
            {  
                if(p > 0 && p % 9 == 0)
                {
                    String datain = j.toString();
                    processMessage(datain);
                    j = new StringJoiner(" ");
                }
                
                 j.add(parts[p]);
            }
            
             datarecieved.append(j.toString().trim());
             if(datarecieved.toString().trim().split("\\s+").length == 9)
             {
                  processMessage(datarecieved.toString().trim());
                  datarecieved = new StringBuilder();
             }
             
             if(datarecieved.toString().trim().split("\\s+").length < 9)
             {
               HandleDataInput(""); 
             }
       }
           
    }
    private static void processMessage(String datain)
    {
        String[] DataParts = datain.toString().trim().split("\\s+");
        
        if(DataParts.length < 9)
        {
              log.AddToDisplay.Display("Data error"+datain,DisplayMessageType.WARNING);
            return;
        }
        
        String auxid = DataParts[1].trim().substring(7);
        Float KResult = null; //DataParts[3].trim();
        Float NaResult = null; //DataParts[4].trim();
        Float ClResult = null; //DataParts[5].trim();
        int KID=0,NaID=0,ClID=0;
        boolean flag = false;
        
        try
        {
             KResult = Float.parseFloat(DataParts[3].trim());
            KID = getMeasureID("3");
            if(KID > 0 && SaveResults(auxid, KID,KResult.toString()))
            {
                flag = true;
            }
        }catch(NumberFormatException e){

        }
        
        try
        {
            NaResult = Float.parseFloat(DataParts[4].trim());
            NaID = getMeasureID("4");
            if(NaID > 0 && SaveResults(auxid, NaID,NaResult.toString()))
            {
                flag = true;
            }
        }catch(NumberFormatException e){

        }
        
        try
        {
            ClResult = Float.parseFloat(DataParts[5].trim());
            ClID = getMeasureID("5");
            if(ClID > 0 && SaveResults(auxid, ClID,ClResult.toString()))
            {
                flag = true;
            }
        }catch(NumberFormatException e){

        }
        
        
        if(flag)
        {	
            
            log.AddToDisplay.Display("Results with Code: "+auxid +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
        }
        else
        {
             log.AddToDisplay.Display("Test with Code: "+auxid +" not sent to BLIS",DisplayMessageType.WARNING);
        }
              
    }
    
    public void Stop()
    {
        stopped = true; 
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
        xmlparser p = new xmlparser("configs/Caretium/xi_921f.xml");
        try {
            data = p.getBT3000PLUSFilter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(CaretiumXI921F.class.getName()).log(Level.SEVERE, null, ex);
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
     
    private static boolean SaveResults(String barcode,int MeasureID, String value)
     {
         
         barcode = barcode.trim();
          boolean flag = false;       
          if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value)))
           {
              flag = true;
            }
                          
         return flag;
         
     }
    
    private static int getCheckSum(String data)
    {
        int checksum =0;
        
        for(int i=0;i<data.length();i++)
        {
            checksum = checksum + (int)data.charAt(i);          
        }
        
        return checksum % 256;
    }
    
     private static String padString(String value, int length, String padWith)
     {
         return padString(value,length,padWith,true);
     }

    private static String padString(String value, int length, String padWith, boolean rightPad)
    {
        String Padded = value;
        String padvalue ="";
        for(int i=value.length();i<length;i++)
        {
           padvalue = padvalue + padWith;
        }
        if(rightPad)
        {
            Padded = Padded + padvalue;
        }
        else
        {
            Padded = padvalue + Padded;
        }
        
        return Padded;
    }
    
    
    private static String getTestCodes(String BLISTestID)
    {
        String found="";
        String[] BLISTestIDs = BLISTestID.split(",");        
        for(int i =0;i<BLISTestIDs.length;i++)
        {
            for(int j=0;j<testIDs.size();j++)
            {
                if(testIDs.get(j).split(";")[1].equalsIgnoreCase(BLISTestIDs[i]))
                {
                    found = found + padString(testIDs.get(j).split(";")[0],4," ");
                    break;
                }
            }
        }
        
        return found;
    }
    private static String getSampleCode(String BLISsampleID)
    {
        String[] EqID = getSpecimenFilter(5).split(",");
        String[] BLISID = getSpecimenFilter(2).split(",");
        String found ="";
        for(int i =0;i<BLISID.length;i++)
        {
            if(BLISID[i].equalsIgnoreCase(BLISsampleID))
            {
                found = EqID[i];
                break;
            }
        }
        
        return found;
        
    }
   
}
