/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package RS232;


import BLIS.sampledata;
import MSACCESS.Result;
import configuration.configuration;
import configuration.xmlparser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.DisplayMessageType;
import system.SampleDataJSON;
import system.settings;


/**
 *
 * @author BLIS
 */
public class MiniVidas extends Thread {
    
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
        log.AddToDisplay.Display("MINI VIDAS  handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Checking available ports on this system...", DisplayMessageType.INFORMATION);
        String[] ports = Manager.getSerialPorts();
        log.AddToDisplay.Display("Avaliable ports:", DisplayMessageType.TITLE);
       for(int i = 0; i < ports.length; i++){           
           log.AddToDisplay.Display(ports[i],log.DisplayMessageType.INFORMATION);
        }            
       log.AddToDisplay.Display("Now connecting to port "+RS232Settings.COMPORT , DisplayMessageType.TITLE);
       if(Manager.openPortforData("MINI VIDAS"))
       {
           log.AddToDisplay.Display("Connected sucessfully",DisplayMessageType.INFORMATION);   
           setTestIDs();
       }
       
       
       //setTestIDs();
       //
       //datarecieved.append(" mtr|ppnsiniFAUST|rtx=rnwti-HCV|t12:02|td2/09/21|qlegative|g]G0a");
      //datarecieved.append("trsl|i|n|i|i20210215007|tHCV|nAnti-HCV|t14:18|d02/15/21|gVgative|n0.13|bVx");
     //processMessage();
       //
      
    }
    
    
    public void getBLISTests(String aux_id, boolean flag)
     {
         try
         {
         String data = BLIS.blis.getTestData(getSpecimenFilter(2), "",aux_id,settings.POOL_DAY);
         List<sampledata> SampleList = SampleDataJSON.getSampleObject(data);
         SampleList = SampleDataJSON.normaliseResults(SampleList);
         if(SampleList.size() > 0)
         {            
             for (int i=0;i<SampleList.size();i++) 
             {                 
                 if(!testExist(SampleList.get(i).aux_id))
                 {
                    // log.AddToDisplay.Display(SampleList.size()+" result(s) test found in BLIS!",DisplayMessageType.INFORMATION);
                     //log.AddToDisplay.Display("Sending test to Analyzer",DisplayMessageType.INFORMATION);
                     log.AddToDisplay.Display("Sending test with CODE: "+SampleList.get(i).aux_id + " to Analyzer BT3000 Plus",DisplayMessageType.INFORMATION);
                     if(sendTesttoAnalyzer(SampleList.get(i)))
                     {
                         addToQueue(SampleList.get(i).aux_id);
                         log.AddToDisplay.Display("Test sent sucessfully",DisplayMessageType.INFORMATION);
                     }
                 }
                 else
                 {
                     if(flag)                         
                         log.AddToDisplay.Display("Sample with code: "+aux_id +" already exist in Analyzer",DisplayMessageType.ERROR);
                 }
             }
             
         }
          else
           {
              if(flag)                         
                log.AddToDisplay.Display("Sample with code: "+aux_id +" does not exist in BLIS",DisplayMessageType.ERROR);
          }
         }catch(Exception ex)
         {
             log.logger.PrintStackTrace(ex);
         }
     }
     
    private boolean testExist(String auxid)
    {
         boolean flag = false;
         List<String> barcodes = getQueue("configs/BT3000Plus/sentlist.txt");
         for(int i=0;i<barcodes.size();i++)
         {
           if(barcodes.get(i).equalsIgnoreCase(auxid))
           {
               flag = true;
               break;
           }
                  
         }
         
         return flag;
    }
     
     private boolean addToQueue(String SampleBarcode)
     {
         boolean flag = false;
         try
        {           
            PrintWriter printWriter;
            try (FileWriter fileWriter = new FileWriter(new File("configs/BT3000Plus/queue.txt"), true)) {
                printWriter = new PrintWriter(fileWriter);
                printWriter.println(SampleBarcode);                           
            }
            printWriter.close();
            
            try (FileWriter fileWriter = new FileWriter(new File("configs/BT3000Plus/sentlist.txt"), true)) {
                printWriter = new PrintWriter(fileWriter);
                printWriter.println(SampleBarcode);                           
            }
            printWriter.close();
            flag = true;
        }
        catch(Exception ex) { 
            log.logger.Logger(ex.getMessage());
            flag = false;
        }
         
         
         return flag;
     }
     
     private boolean addToQueue(List<String> barcodes)
     {
         boolean flag = false;
         try
        {           
            PrintWriter printWriter;
            try (FileWriter fileWriter = new FileWriter(new File("configs/BT3000Plus/queue.txt"))) {
                printWriter = new PrintWriter(fileWriter);
                printWriter.print("");
                for(int i=0;i<barcodes.size();i++)
                {
                    printWriter.println(barcodes.get(i));
                }
            }
            printWriter.close();
            flag = true;
        }
        catch(Exception ex) { 
            log.logger.Logger(ex.getMessage());
            flag = false;
        }
         
         
         return flag;
     }
     private List<String> getQueue(String path )
     {
         List<String> barcode = new ArrayList<>();
         
        
        Scanner scanner = null;
        try {
             File config_file = new File(path);
            scanner = new Scanner(config_file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        String code="";
        while(scanner.hasNextLine())
        {
            code = scanner.nextLine().trim();
            if(!code.isEmpty())
            {
                barcode.add(code); 
            }
        }
        return barcode;         
     }
     
     private void manageResults()
     {
         askResultFromAnalyzer();
           /*List<String> barcodes = getQueue("configs/BT3000Plus/queue.txt");
           List<String> maintainbarcodes = new ArrayList<>();
           for(int i=0;i<barcodes.size();i++)
           {
               
               if(!getAndSaveResults(barcodes.get(i)))
               {
                   maintainbarcodes.add(barcodes.get(i));                   
               }
               else
               {
                   log.AddToDisplay.Display("Sent results of code:"+barcodes.get(i)+" to BLIS sucessfully",log.DisplayMessageType.INFORMATION);
               }
           }           
          addToQueue(maintainbarcodes);
                   */
           
     }
     
    
     
    private static boolean SendInitiateComm()
    {
        boolean flag = false;  
       
        appState = MSGMODE.WORKING;
        try
        {
           if( Manager.writeToSerialPort(STX))
           {
                         
               flag = true;   
               appState = MSGMODE.WAIT_FOR_ACK;
           }
           //flag = false;
        }catch(Exception ex)
        {
            flag = false;
            log.AddToDisplay.Display(ex.getMessage(), log.DisplayMessageType.ERROR);
        }
        
        
        return flag;
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
        if(data.charAt(0) == ENQ)
        {
          Manager.writeToSerialPort(ACK);
          appState = MSGMODE.RESULTS_REQUESTED;
        }
        
       if(appState == MSGMODE.WAIT_FOR_ACK)
       {
           if(data.charAt(0) == ACK)
           {
              appState = MSGMODE.ACK_RECEIVED;
           }
       }
       else if(appState == MSGMODE.RESULTS_REQUESTED)
       {
    	   if(data.contains(String.valueOf(STX)))
    	   {
    		   datarecieved = new StringBuilder();
    	   }
           
    	   if(data.contains(String.valueOf(RSEP)) || data.contains(String.valueOf(GSEP)) || data.contains(String.valueOf(End_Block)) || data.contains(String.valueOf(EXC)))
           {
    		   Manager.writeToSerialPort(ACK);
           }
           
           
    	   datarecieved.append(data);
    	   if(data.charAt(data.length()-1) == EOT)
           {
           		Manager.writeToSerialPort(ACK);
               appState = MSGMODE.WAIT_FOR_ACK;
               
               log.AddToDisplay.Display( datarecieved.toString(),DisplayMessageType.INFORMATION);
               processMessage();
           }     
    	   
       }
       
           
    }
    private static void processMessage()
    {
    	       
        String report = datarecieved.toString().replace(String.valueOf(STX), "").replace(String.valueOf(STX), "").replace(String.valueOf(STX), "");
        String[] DataParts = report.trim().split("\\|");
        
        String ttype = DataParts[5].trim();
        String auxid = DataParts[4].trim();
        String result = DataParts[9].trim();
        String nval = DataParts[10].trim();
        
        if(result.isEmpty())
        {
        	log.AddToDisplay.Display("Result not valid",DisplayMessageType.WARNING);
        	return;
        }
        
        if(result.endsWith("gative"))
        {
        	result = "Negative";
        }
        if(result.endsWith("sitive"))
        {
        	result = "Positive";
        }
        
        if(!auxid.isEmpty())
        {
        	auxid = auxid.substring(1);
        }
        
        if(!nval.isEmpty())
        {
        	nval = nval.substring(1);
        }
        
        int mID=0;
        boolean flag = false;
        
        mID = getMeasureID(ttype);
        if(mID <= 0)
        {
        	log.AddToDisplay.Display("Analyzer UD: "+ttype +" not Configured ",DisplayMessageType.WARNING);
        	return;
        }
        
        if(SaveResults(auxid, mID,result))
        {
        	flag = true;
        }
        
        if(flag)
        {
        	mID = getMeasureID("10");
        	if(mID > 0)
        	{
        		SaveResults(auxid, mID,result);
        	}
            
        	log.AddToDisplay.Display("Results with Code: "+auxid +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
        }
        else
        {
             log.AddToDisplay.Display("Test with Code: "+auxid +" not Found on BLIS",DisplayMessageType.WARNING);
        }   
         
         appState = MSGMODE.IDLE;
              
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
        xmlparser p = new xmlparser("configs/vidas/minividas.xml");
        try {
            data = p.getBT3000PLUSFilter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MiniVidas.class.getName()).log(Level.SEVERE, null, ex);
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
    private static boolean sendTesttoAnalyzer(sampledata get) 
    {
       boolean flag = false;
       if(SendInitiateComm())
       {
           while(appState != MSGMODE.ACK_RECEIVED)
           {
               try {
                   Thread.sleep(100);
               } catch (InterruptedException ex) {
                   Logger.getLogger(MiniVidas.class.getName()).log(Level.SEVERE, null, ex);
               }
           }            
          String AnalyzerPatient = getBT3000PlusPatientString(get);
          Manager.writeToSerialPort(AnalyzerPatient);
          flag = true;
       }
       else
       {
           flag = false;
       }
       
       return flag;
    }
    
       private static boolean askResultFromAnalyzer()
       {
           return askResultFromAnalyzer('R');
       }
    private static boolean askResultFromAnalyzer(char Type) 
    {
       boolean flag = false;
       if(SendInitiateComm())
       {
           while(appState != MSGMODE.ACK_RECEIVED)
           {
               try {
                   Thread.sleep(100);
               } catch (InterruptedException ex) {
                   Logger.getLogger(MiniVidas.class.getName()).log(Level.SEVERE, null, ex);
               }
           } 
            StringBuilder data = new StringBuilder();
            data.append(Type);
            data.append(EOT);            
            Manager.writeToSerialPort(data.toString());
            appState = MSGMODE.RESULTS_REQUESTED;
          
       }
       else
       {
           flag = false;
       }
       
       return flag;
    }
    
    private static String getBT3000PlusPatientString(sampledata patient)
    {
      StringBuilder data = new StringBuilder();
      data.append(padString(patient.aux_id,15," "));
      data.append("T");
      data.append(getSampleCode(patient.specimen_type_id.split(",")[0]));
      data.append("N");
      data.append("00");
      data.append(padString(String.valueOf(patient.measure_id.split(",").length),2,"0",false ));
      data.append(getTestCodes(patient.measure_id));
      data.append(getCheckSum(data.toString()));
      data.append(EOT);      
      
      return data.toString();
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
