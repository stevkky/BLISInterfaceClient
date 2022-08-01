/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TCPIP;

import BLIS.sampledata;
import configuration.xmlparser;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import log.DisplayMessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import system.SampleDataJSON;
import system.settings;
import system.utilities;
import ui.MainForm;

/**
 *
 * @author BLIS
 */
public class CobasAmpliPrep extends Thread{
        
    String read;
    boolean first =true;   
    DataOutputStream outToEquipment=null;
    ServerSocket welcomeSocket=null;
    Socket connSock = null;
    Iterator list= null;
    static Queue<String> OutQueue=new LinkedList<>();
    private static List<String> testIDs = new ArrayList<>();
    private static Queue<String> PatientTest = new LinkedList<>();
    private static String ASTMMsgs ="";
     
    boolean stopped = false;
    //Queue<String> InQueue=new LinkedList<>();
    private static final char CARRIAGE_RETURN = 13; 
    private static final char STX = 0x02;
    private static final char ACK = 0x06;
    private static final char EOT = 0x04;
    private static final char NAK = 0x15;
    private static final char NUL = 0x00;
    private static final char ENQ = 0x05;
    private static final char ETX = 0x03;
    private static final char CR = 0x0D;
    private static final char LF = 0x0A;
    private static final char ETB = 0x17;
   
    private static MODE appMode = MODE.IDLE;
    public enum  MSGTYPE
    {
        QUERY(0),
        RESULTS(1),
        ACK_RECEIVED(2),
        ACK(3),
        ENQ(4),
        NAK(5),
        EOT(6),
        STX(7),
        ETX(8),
        UNKNOWN(-1);
       
        
        private MSGTYPE(int value)
        {
            this.Value = value;
        }
        
        private int Value;
        
    }
    
   enum MODE
   {
       SENDING_QUERY,
       RECEIVEING_RESULTS,
       IDLE;
   }
    public void Stop()
    {
        try {
            
            stopped = true;
            if(null != connSock)
                connSock.close();
            if(null !=welcomeSocket)
                 welcomeSocket.close();
//            connSock.close();
             log.AddToDisplay.Display("Cobas AmpliPrep handler stopped", DisplayMessageType.TITLE);
        } catch (IOException ex) {
            Logger.getLogger(BT3000PlusChameleon.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
      @Override
    public void run() {             
         
        try
        {
            if(tcpsettings.SERVER_MODE)
            {
                log.AddToDisplay.Display("Cobas AmpliPrep handler started...", DisplayMessageType.TITLE); 
                log.AddToDisplay.Display("Starting Server socket on port "+tcpsettings.PORT, DisplayMessageType.INFORMATION);
                welcomeSocket = new ServerSocket(tcpsettings.PORT);
  		log.AddToDisplay.Display("Waiting for Equipment connection...", DisplayMessageType.INFORMATION);
  		log.AddToDisplay.Display("Listening on port "+ tcpsettings.PORT+"...",DisplayMessageType.INFORMATION);
                connSock = welcomeSocket.accept();    
            }
            else
            {
                log.AddToDisplay.Display("Starting Client socket on IP "+tcpsettings.EQUIPMENT_IP +" on port  "+tcpsettings.PORT, DisplayMessageType.INFORMATION);
               connSock = new Socket(tcpsettings.EQUIPMENT_IP, tcpsettings.PORT);
            }
                log.AddToDisplay.Display("Cobas AmpliPrep is now Connected...",DisplayMessageType.INFORMATION);
                first=false;
                if(!tcpsettings.SERVER_MODE)
                {
                    connSock.setKeepAlive(true);
                }
                ClientThread client = new ClientThread(connSock,"CobasAmpliPrep");
                client.start();
                String message ;
                outToEquipment= new DataOutputStream(connSock.getOutputStream());
                 setTestIDs();
                while(!stopped)
                {                 
                    synchronized(OutQueue)
                    {                        
                        while(!OutQueue.isEmpty())
                        {
                            System.out.println("Message found in sending queue");
                            log.AddToDisplay.Display("Message found in sending queue",DisplayMessageType.TITLE);
                            //log.logger.Logger("Message found in sending queue");
                            message =(String) OutQueue.poll();                             
                            outToEquipment.writeBytes(message);
                            //System.out.println(message+ "sent sucessfully");
                            log.AddToDisplay.Display(message+ "sent successfully",DisplayMessageType.INFORMATION);
                            //log.logger.Logger(message+ "sent sucessfully");
                        }
                    }
                    
                
                }


         }
         catch(IOException e)
         {
                if(first)
		{
                    if(tcpsettings.SERVER_MODE)
                        log.AddToDisplay.Display("could not listen on port :"+tcpsettings.PORT + " "+e.getMessage(),DisplayMessageType.ERROR);
                    else
                        log.AddToDisplay.Display("could not connect to server: "+tcpsettings.EQUIPMENT_IP+" on port :"+tcpsettings.PORT + " "+e.getMessage(),DisplayMessageType.ERROR);
                   // log.logger.Logger(e.getMessage());
		}
		else
		{
                    log.AddToDisplay.Display("Cobas AmpliPrep client is now disconnected!",DisplayMessageType.WARNING);
                    log.logger.Logger(e.getMessage());
		}


	}
       
    }
    
    public void getFromBlis(String barcode)
     {   
         
       getBLISTests(barcode,true);
        
     }
    
    private static void resetCon()
    {
        /* if(!tcpsettings.SERVER_MODE)
               {
                 stopped = true;
                 if(null != connSock)
                 try {
                     connSock.close();
                 } catch (IOException ex) {
                     Logger.getLogger(BT3000PlusChameleon.class.getName()).log(Level.SEVERE, null, ex);
                 }
                   MainForm.btobj = new BT3000PlusChameleon();
                   MainForm.btobj.start();
               }*/
    }
   private static void getBLISTests(String aux_id, boolean flag)
     {
         try
         {
            String data = BLIS.blis.getSampleData(aux_id,"","",getSpecimenFilter(2), getSpecimenFilter(4));
            List<sampledata> SampleList = SampleDataJSON.getSampleObject(data);
            SampleList = SampleDataJSON.normaliseResults(SampleList);
            if(SampleList.size() > 0)
            {            
                for (int i=0;i<SampleList.size();i++) 
                {     
                       appMode = MODE.SENDING_QUERY;
                       log.AddToDisplay.Display("Sending test with Code: "+SampleList.get(i).aux_id + " to BT 300 PLUS Chameleon",DisplayMessageType.INFORMATION);
                       prepare(SampleList.get(i)); 
                       AddtoQueue(ENQ);
                       /*while(appMode != MODE.IDLE)
                       {
                           Thread.sleep(100);
                       }*/
                       
                }

            }
             else
              {
                 // AddtoQueue(null, query);
                 if(flag)                         
                   log.AddToDisplay.Display("Sample with barcode: "+aux_id +" does not exist in BLIS",DisplayMessageType.INFORMATION);
             }
         }catch(Exception ex)
         {
             log.logger.PrintStackTrace(ex);
         }
     }
   private static void AddtoQueue(char value) 
   {
       synchronized(OutQueue)
        {
            OutQueue.add(String.valueOf(value));
            //log.logger.Logger("New message added to sending queue\n"+strData.toString());                                                          
       }
       
   }
   private static void prepare(sampledata get)
   {
        PatientTest.clear();
       StringBuffer strData = new StringBuffer();
       strData.append(STX); 
       StringBuffer strTemp = new StringBuffer();
       strTemp.append("1H|\\^&|||LIS|||||||P|E1394-97|"); 
       strTemp.append(utilities.getSystemDate("yyyyMMddHHmmss"));
       strTemp.append(CR);
       strTemp.append(ETX);       
       strData.append(strTemp.toString());
       strData.append(utilities.getCheckSum(strTemp.toString()));
       strData.append(CR);
       strData.append(LF);   
       PatientTest.add(strData.toString());
       strData = new StringBuffer();
       strTemp = new StringBuffer();       
       strData.append(STX);      
       strTemp.append("2P|1||");
       strTemp.append(get.surr_id);
       strTemp.append("||");
       strTemp.append(get.name.trim().replaceFirst(" ", "^"));
       strTemp.append("||");
       String[] parts = utilities.getNormalizedDate(get.dob,get.partial_dob).split("-");
       strTemp.append(parts[0]).append(parts[1]).append(parts[2]);
       strTemp.append("|");
       strTemp.append(get.sex);
       strTemp.append("|||||||||||||||||");          
       strTemp.append("LAB");
       strTemp.append(CR);
       strTemp.append(ETX);       
       strData.append(strTemp.toString());
       strData.append(utilities.getCheckSum(strTemp.toString()));
       strData.append(CR);
       strData.append(LF); 
       PatientTest.add(strData.toString());             
       String[] testparts = get.measure_id.split(",");   
       int j=3;
       for(int i=0;i<testparts.length;i++,j++)
       {
            strData = new StringBuffer();
            strTemp = new StringBuffer();  
            strData.append(STX);
           if(j > 7)
               j=0;
            strTemp.append(j);
            strTemp.append("O");
            strTemp.append("|");
            strTemp.append(i+1);
            strTemp.append("|");
            strTemp.append(get.aux_id);
            strTemp.append("||");
            strTemp.append(getEquipmentID(testparts[i]));
            strTemp.append("|||");
            strTemp.append(utilities.getSystemDate("yyyyMMddHHmmss"));
            strTemp.append("||||||||||||||||||F");
            strTemp.append(CR);
            strTemp.append(ETX);
            strData.append(strTemp.toString());
            strData.append(utilities.getCheckSum(strTemp.toString()));
            strData.append(CR);
            strData.append(LF); 
            PatientTest.add(strData.toString());          
       }
            strData = new StringBuffer();
            strTemp = new StringBuffer();  
            strData.append(STX);
            strTemp.append(j);
            strTemp.append("L");
            strTemp.append("|");
            strTemp.append(1);
            strTemp.append("|N");
            strTemp.append(CR);
            strTemp.append(ETX);  
            strData.append(strTemp.toString());
            strData.append(utilities.getCheckSum(strTemp.toString()));
            strData.append(CR);
            strData.append(LF); 
            PatientTest.add(strData.toString());  
       
   }
    
      private static void AddtoQueue(String data) 
      {        
            
              synchronized(OutQueue)
               {
                   OutQueue.add(data);
                   log.logger.Logger("New message added to sending queue\n"+data);                                                          
               }
      }
    public static void handleMessage(String message)
    {         
         synchronized(MainForm.set)
          {
             MainForm.set = MainForm.RESET.WAIT;
          }
        try
        {
            //String[] msgParts = message.split("\r");
            MSGTYPE type =getMessageType(message);           
            if(type == MSGTYPE.ENQ)
            {                
                AddtoQueue(ACK);
               //ASTMMsgs="";
               appMode = MODE.RECEIVEING_RESULTS;
            }
            else if(type == MSGTYPE.STX)
            {
                AddtoQueue(ACK);
               ASTMMsgs = ASTMMsgs + "\r"+message;
               appMode = MODE.RECEIVEING_RESULTS;
               if(ASTMMsgs.endsWith(String.valueOf(EOT)))
               {
                   process();
               }
                
            }
            else if (type == MSGTYPE.EOT)
            {     
               process();
                
            }
            else if (type == MSGTYPE.NAK)
            {
                 log.AddToDisplay.Display("NAK Response from Analyzer",DisplayMessageType.ERROR);
            }
            else if(type == MSGTYPE.ACK)
            {
                if(appMode == MODE.SENDING_QUERY)
                {
                    if(PatientTest.size()>0)
                    {
                        AddtoQueue(PatientTest.poll());
                    }
                    else
                    {
                        AddtoQueue(EOT);
                        Thread.sleep(500);
                        appMode = MODE.IDLE;
                        synchronized(MainForm.set)
                        {
                            MainForm.set = MainForm.RESET.NOW;
                        }
                    }
                }
                
               
               
            }         
            
        }catch(Exception ex)
        {
            log.AddToDisplay.Display("Processing Error Occured!",DisplayMessageType.ERROR);
            log.AddToDisplay.Display("Data format of Details received from Analyzer UNKNOWN",DisplayMessageType.ERROR);
            log.logger.PrintStackTrace(ex);
        }
       
    }
    
    private static void process()
      {        
          
           if(!ASTMMsgs.isEmpty())
            {
                   ASTMMsgs = ASTMMsgs.replaceAll(String.valueOf(STX)+"[\\d]", "");
                   ASTMMsgs = ASTMMsgs.replaceAll(String.valueOf(ETB) + String.valueOf(CR), "");
                   
                   String[] PatientParts = ASTMMsgs.trim().split("L\\|1\\|N"+String.valueOf(CR));
                    for(int p=0;p<PatientParts.length;p++)
                    {
                        String[] msgParts = PatientParts[p].trim().split("\r");
                         int id=0;
                         if(msgParts.length == 3)
                             id =1;
                         else
                             id =2;
                        String pidParts[] = msgParts[id].split("\\|");
                        if(pidParts.length > 4)
                        {
                            if("Q".equals(pidParts[0].trim()))
                            {
                                 String patientid = pidParts[2].split("\\^")[0].trim();   
                                 if(!patientid.equalsIgnoreCase("ALL"))
                                 {
                                    String SampleID = pidParts[2].split("\\^")[1].trim();
                                     getBLISTests(SampleID,true);
                                 }
                                 else
                                 {
                                     appMode = MODE.SENDING_QUERY;
                                     AddtoQueue(ENQ);
                                     noDataResponse();
                                     log.AddToDisplay.Display("Fetching of All samples not enabled",DisplayMessageType.INFORMATION);
                                      appMode = MODE.IDLE;
                                 }
                                  
                            }
                            else if("C".equals(pidParts[0].trim()))
                            {
                                log.AddToDisplay.Display("last request has been cancelled by Cobas AmpliPrep",DisplayMessageType.INFORMATION);
                                 appMode = MODE.IDLE;
                                synchronized(MainForm.set)
                                {
                                    MainForm.set = MainForm.RESET.NOW;
                                }
                            }
                            else
                            {
                                pidParts = msgParts[2].split("\\|");
                                String patientid = "";
                                if(msgParts[1].split("\\|").length > 4)
                                {
                                    patientid = msgParts[1].split("\\|")[4].trim();

                                }
                                
                                String SampleID = pidParts[2].split("\\^")[0].trim(); 
                                
                                String testdate = utilities.formatAsDate("yyyyMMddhhmmss", pidParts[6].trim()); 
                               
                                int mID=0;
                                String  value = "";
                                boolean flag = false;
                                
                                
                                    for(int i=0;i<msgParts.length;i++)
                                    {
                                        if(msgParts[i].split("\\|")[0].endsWith("R"))
                                        {
                                            mID = getMeasureID(msgParts[i].split("\\|")[2].split("\\^")[3].trim());
                                          
                                            if(mID > 0)
                                            {   
                                                try
                                                { 
                                                    value = msgParts[i].split("\\|")[3].trim();
                                                    
                                                }catch(ArrayIndexOutOfBoundsException ex)
                                                { 
                                                  continue;
                                                }
                                               
                                                if(SaveResults(SampleID, mID,value))
                                                {
                                                    log.AddToDisplay.Display("\nSent to bliss:  Specimen ID: ["+SampleID +"]",DisplayMessageType.INFORMATION);
                                                }
                                                else
                                                {
                                                    log.AddToDisplay.Display("\nError  Specimen ID: ["+SampleID +"] could not be queued",DisplayMessageType.WARNING);
                                                }
                                                        
                                            }
                                        }
                                    }
                                    
                                 }
                                
                                
                            }

                        appMode = MODE.IDLE;
                        synchronized(MainForm.set)
                        {
                            MainForm.set = MainForm.RESET.NOW;
                        }
                        ASTMMsgs="";

                        }
                                              
            }
           
        }
    
    private static void noDataResponse()
   {
       Random rand = new Random();

       PatientTest.clear();
       StringBuffer strData = new StringBuffer();
       strData.append(STX); 
       StringBuffer strTemp = new StringBuffer();
       strTemp.append("1H|@^\\|");
       strTemp.append(rand.nextInt(999999));
       strTemp.append("||BLIS|||||GeneXpert PC^GeneXpert^4.3||P|1394-97|");       
       strTemp.append(utilities.getSystemDate("yyyyMMddHHmmss"));
       strTemp.append(CR);   
       strTemp.append("L");
       strTemp.append("|");
       strTemp.append(1);
       strTemp.append("|I");
       strTemp.append(CR);            
       strTemp.append(ETX);  
       strData.append(strTemp.toString());
       strData.append(utilities.getCheckSum(strTemp.toString()));
       strData.append(CR);
       strData.append(LF); 
       PatientTest.add(strData.toString());  
   }
    private static MSGTYPE getMessageType(String msg)
    {
        MSGTYPE type = null;
        switch (msg.charAt(0))
        {
            case ACK:
                type = MSGTYPE.ACK;                
                break;
            case ENQ:
                type = MSGTYPE.ENQ;
                break;
            case EOT:
                type =MSGTYPE.EOT;
                break;
            case NAK:
                type = MSGTYPE.NAK;
                break;
            case STX:
                type = MSGTYPE.STX;
                break;
            case ETX:
                type = MSGTYPE.ETX;
                break;
            default:
                String[] parts = msg.split("\r");
                if(parts.length > 1 )
                {
                    if(parts[1].startsWith("2Q|"))
                    {
                        type= MSGTYPE.QUERY;
                    }
                    else if (parts[1].startsWith("2P|"))
                    {
                        type = MSGTYPE.RESULTS;
                    }
                    else
                    {
                        type =MSGTYPE.UNKNOWN;
                    }
                }
        }
            
        return type;
        
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
        xmlparser p = new xmlparser("configs/CobasAmpliPrep/cobasampliprep.xml");
        try {
            data = p.getMicros60Filter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(SYSMEXXS500i.class.getName()).log(Level.SEVERE, null, ex);
            log.logger.PrintStackTrace(ex);
            log.AddToDisplay.Display(ex.getMessage(), log.DisplayMessageType.ERROR);
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
     private static String getEquipmentID(String measureID)
     {
         String equipmentID = "";
         for(int i=0;i<testIDs.size();i++)
         {
             if(testIDs.get(i).split(";")[1].equalsIgnoreCase(measureID))
             {
                 equipmentID = testIDs.get(i).split(";")[0];
                 break;
             }
         }
         
         return equipmentID;
     }
     
    private static boolean SaveResults(String barcode,int MeasureID, String value)
     {
         
         
          boolean flag = false;       
          if("1".equals(BLIS.blis.saveResults(barcode,MeasureID,value)))
           {
              flag = true;
            }
                          
         return flag;
         
     } 
    
    
    
   }
