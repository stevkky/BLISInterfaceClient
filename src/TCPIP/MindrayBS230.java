/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TCPIP;

import BLIS.sampledata;
import static TCPIP.MindrayBC5380.testIDs;
import configuration.xmlparser;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import log.DisplayMessageType;
import hl7.Mindray.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import system.SampleDataJSON;

/**
 *
 * @author BLIS
 */
public class MindrayBS230 extends Thread{
        
    String read;
    boolean first =true;   
    DataOutputStream outToEquipment=null;
    ServerSocket welcomeSocket=null;
    Socket connSock = null;
    Iterator list= null;
    static Queue<String> OutQueue=new LinkedList<>();
    
    boolean stopped = false;
    //Queue<String> InQueue=new LinkedList<>();
    
   
    public void Stop()
    {
        try {
            
            stopped = true;
            if(null != connSock)
                connSock.close();
            
            welcomeSocket.close();
//            connSock.close();
             log.AddToDisplay.Display("Mindray BS-230 handler stopped", DisplayMessageType.TITLE);
        } catch (IOException ex) {
            Logger.getLogger(MindrayBS200E.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
      @Override
    public void run() {
        log.AddToDisplay.Display("Mindray BS-230 handler started...", DisplayMessageType.TITLE);     
         
        try
        {
             if(tcpsettings.SERVER_MODE)
            {   
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
                         
                log.AddToDisplay.Display("Mindray BS-230 is now Connected...",DisplayMessageType.INFORMATION);
                first=false;
                ClientThread client = new ClientThread(connSock,"Mindray BS-230");
                client.start();
                String message ;
                outToEquipment= new DataOutputStream(connSock.getOutputStream());
                
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
                    log.AddToDisplay.Display("could not listen on port :"+tcpsettings.PORT + " "+e.getMessage(),DisplayMessageType.ERROR);
                   // log.logger.Logger(e.getMessage());
		}
		else
		{
                    log.AddToDisplay.Display("Mindray client is now disconnected!",DisplayMessageType.WARNING);
                    log.logger.Logger(e.getMessage());
		}


	}
       
    }
    
    
    
    private static String getValue( Message msg, String segmentname,int position)
    {
       String value="";
       for(int i=0;i<msg.Segments.size();i++)
       {
           if(msg.Segments.get(i).name.equalsIgnoreCase(segmentname))
           {
              for(int j=0; j<msg.Segments.get(i).Fields.size();j++)
              {
                  if(msg.Segments.get(i).Fields.get(j).position == position)
                  {
                      value = msg.Segments.get(i).Fields.get(j).realValue;
                      break;
                  }
              }
           }
       }
       return value;
    }
    
    public static void handleMessage(String message)
    {        
        xmlparser p = new xmlparser("configs/hl7/MindrayInterface.xml");
        try {
            Message msg = p.getMindrayMessage(message);                      
            String MessageDate = msg.Segments.get(0).Fields.get(4).realValue;
             Message ackMessage = getReplyMessage(msg, msg.replymessage);            
            if(MessageType.QUERY.toString().equalsIgnoreCase(msg.type))
            {
                String sampleID = getValue(msg, "QRD", 8);
                String subjectFilter =  getValue(msg, "QRD", 9);
                String startDate = getValue(msg, "QRF", 2);
                String endDate = getValue(msg, "QRF", 3); 
                String data = BLIS.blis.getSampleData(sampleID, startDate, endDate, getSpecimenFilter(1),getSpecimenFilter(2));              
                Message resultsMessage = null;
                List<String> datamessageList = new ArrayList<>();
                if(data.equals("0"))
                {
                    log.AddToDisplay.Display("No test found for request!",DisplayMessageType.INFORMATION);
                    //log.logger.Logger("No results found for request!");
                    ackMessage.setValue("QAK", 2, MessageAcknowledgmentCode.OK_NODATA_FOUND.getCode());
                }
                else if(data.equals("-1"))
                {
                    log.AddToDisplay.Display("Login error. Please check BLIS login credentials in configurations file!",DisplayMessageType.ERROR);
                   // log.logger.Logger("Login error. Please check BLIS login credentials in configurations file");
                    ackMessage.setValue("MSA", 1, MessageAcknowledgmentCode.REJECTED_RECORD_LOCKED.getCode());
                    ackMessage.setValue("MSA", 3, MessageAcknowledgmentCode.REJECTED_RECORD_LOCKED.getStatusText());
                    ackMessage.setValue("MSA", 6, Integer.toString(MessageAcknowledgmentCode.REJECTED_RECORD_LOCKED.getStatusCode()));
                    ackMessage.setValue("ERR", 1, Integer.toString(MessageAcknowledgmentCode.REJECTED_RECORD_LOCKED.getStatusCode()));
                    ackMessage.setValue("QAK", 2, MessageAcknowledgmentCode.REJECTED.getCode());
                }
                else
                {
                     List<sampledata> SampleList = SampleDataJSON.getSampleObject(data);
                     log.AddToDisplay.Display(SampleList.size()+" tests found!",DisplayMessageType.INFORMATION);
                     //log.logger.Logger(SampleList.size()+" results found!"); 
                     resultsMessage = getReplyMessage(msg,"DSR^Q03"); 
                     datamessageList = resultsMessage.getHL7Message(SampleList);
                     
                }               
                
                synchronized(OutQueue)
                {
                     String mindrayHL7Message = ackMessage.getHL7Message();
                    OutQueue.add(mindrayHL7Message);
                    log.logger.Logger("New message added to sending queue\n"+mindrayHL7Message);
                  // System.out.println(mindrayHL7Message);
                    for(int list =0;list < datamessageList.size();list++)
                    {
                        OutQueue.add(datamessageList.get(list));                                
                        //System.out.println(datamessageList.get(list));
                        log.logger.Logger("New message added to sending queue\n"+datamessageList.get(list));
                    }
                }
            }
            else if(MessageType.OBSERVE_RESULT.toString().equalsIgnoreCase(msg.type))
            {
                 String mindrayHL7Message = ackMessage.getHL7Message();
                synchronized(OutQueue)
                 {
                     OutQueue.add(mindrayHL7Message);
                     log.logger.Logger("New message added to sending queue\n"+mindrayHL7Message);                                                          
                 }
                
                message = message.replaceAll("<::>", "\r");
                  String[] msgParts = message.split("\r");
                 String pidParts[] = msgParts[1].split("\\|");
                if(pidParts.length > 5)
                {
                    String patientid = pidParts[14];
                    String SampleID = patientid;
                    
                    //SampleID = "20220805011";
                    
                    if(SampleID.isEmpty())
                    {
                        log.AddToDisplay.Display("\nInvalid Sample ID ["+SampleID +"]. Result rejected",DisplayMessageType.WARNING);
                        return;                        
                    }
                    
                    //SampleID = utilities.getSystemDate("YYYY") + SampleID;
                   // SampleID =  patientid;
                    int mID=0;
                    float value = 0;
                    boolean flag = false;
                   
                            if(pidParts[0].startsWith("OBR"))
                            {
                                   try
                                  {
                                    mID = Integer.parseInt(pidParts[2]);
                                  }catch(NumberFormatException ex){
                                    mID =0;
                                  }

                               //mID = 1;
                                if(mID > 0)
                                {
                                    try
                                    {
                                        value = Float.parseFloat(pidParts[20].trim());
                                    }catch(NumberFormatException e){
                                     
                                    }
                                    if(SaveResults(SampleID, mID,value))
                                    {
                                        flag = true;
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
            else
            {
               System.out.println("Received\n"+message); 
            }
            
        } catch (Exception ex) {
            Logger.getLogger(MindrayBS200E.class.getName()).log(Level.SEVERE, null, ex);
            log.AddToDisplay.Display(ex.getMessage(),DisplayMessageType.ERROR);
            log.logger.Logger(ex.getMessage());
        }
       
    }
    
    public static int getMeasureID(String equipmentID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             //log.logger.Logger(testIDs.get(i));
             //if(testIDs.get(i).split(";").length > )
             if(testIDs.get(i).split(";")[0].equalsIgnoreCase(equipmentID))
             {
                 measureid = Integer.parseInt(testIDs.get(i).split(";")[1]);
                 break;
             }
         }
         
         return measureid;
     }
    
   
    
    private static String getSpecimenFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/hl7/MindrayBS300.xml");
        try {
            data = p.getMindrayFilter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MindrayBS300.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
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
    
    private static String getSpecimenTestFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/hl7/MindrayInterface.xml");
        try {
            data = p.getMindrayFilter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MindrayBS200E.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }    
     
    private static Message getReplyMessage(Message firstMessage, String type)
    {
        Message data  = null;
        xmlparser p = new xmlparser("configs/hl7/MindrayInterface.xml");
        try {
            data = p.getReplyMessage(firstMessage,type);
        } catch (Exception ex) {
            Logger.getLogger(MindrayBS200E.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }
    
    
    
   }
