/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPIP;

/**
 * Client Connection on port 5100
 * @author Stevkkys
 */

import BLIS.sampledata;
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


public class MindrayBC5000 extends Thread 
{
         
    
     String read;
    boolean first =true;   
    DataOutputStream outToEquipment=null;
    ServerSocket welcomeSocket=null;
    Socket connSock = null;
    Iterator list= null;
    static Queue<String> OutQueue=new LinkedList<>();
    public static List<String> testIDs = new ArrayList<>();
    
    boolean stopped = false;
    //Queue<String> InQueue=new LinkedList<>();
    
   public enum  MSGTYPE
    {
        QUERY(0),
        RESULTS(1),
        ACK_RECEIVED(3),
        UNKNOWN(-1);
       
        
        private MSGTYPE(int value)
        {
            this.Value = value;
        }
        
        private int Value;
        
    }
   
    public void Stop()
    {
        try {
            
            stopped = true;
            if(null != connSock)
                connSock.close();
            
            welcomeSocket.close();
//            connSock.close();
             log.AddToDisplay.Display("Mindray BC-5000 handler stopped", DisplayMessageType.TITLE);
        } catch (IOException ex) {
            Logger.getLogger(MindrayBC5000.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
      @Override
    public void run() {
        log.AddToDisplay.Display("Mindray BC-5000 handler started...", DisplayMessageType.TITLE);
        log.AddToDisplay.Display("Starting Server socket on port "+tcpsettings.PORT, DisplayMessageType.INFORMATION);
         
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
              
                log.AddToDisplay.Display("Mindray BC-5000 is now Connected...",DisplayMessageType.INFORMATION);
                first=false;
                ClientThread client = new ClientThread(connSock,"Mindray BC-5000");
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
    
     private void setTestIDs()
     {
         String equipmentid = getSpecimenFilter(3);
         String blismeasureid = getSpecimenFilter(2);
        
         String[] equipmentids = equipmentid.split(",");
         String[] blismeasureids = blismeasureid.split(",");
         for(int i=0;i<equipmentids.length;i++)
         {
             testIDs.add(equipmentids[i]+";"+blismeasureids[i]);             
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
     
     public static int getEquipmentTestID(String measureID)
     {
         int measureid = 0;
         for(int i=0;i<testIDs.size();i++)
         {
             log.logger.Logger(testIDs.get(i));
             //if(testIDs.get(i).split(";").length > )
             if(testIDs.get(i).split(";")[1].equalsIgnoreCase(measureID))
             {
                 measureid = Integer.parseInt(testIDs.get(i).split(";")[0]);
                 break;
             }
         }
         
         return measureid;
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
        try
        {
            MSGTYPE type =getMessageType(message);
            String[] msgParts = message.split("\r");
            if (type == MSGTYPE.RESULTS)
            {
              
                    //String patientid = pidParts[4];
                    String SampleID = msgParts[3].split("\\|")[3].trim();
                    //SampleID = utilities.getSystemDate("YYYY") + SampleID;
                    //SampleID ="20220802006";
                    int mID=0;
                    float value = 0;
                    boolean flag = false;
                    for(int i=8;i<msgParts.length;i++)
                    {
                            mID = getMeasureID(msgParts[i].split("\\|")[1]);
                            if(mID > 0)
                            {
                                try
                                {
                                    value = Float.parseFloat(msgParts[i].split("\\|")[5]);
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
                    
                     if(flag)
                        {
                             log.AddToDisplay.Display("\nResults with Code: "+SampleID +" sent to BLIS sucessfully",DisplayMessageType.INFORMATION);
                        }
                        else
                        {
                             log.AddToDisplay.Display("\nTest with Code: "+SampleID +" not Found on BLIS",DisplayMessageType.WARNING);
                        }


                

            }
        }catch(Exception ex)
        {
            log.AddToDisplay.Display("Processing Error Occured!",DisplayMessageType.ERROR);
            log.AddToDisplay.Display("Data format of Details received from Analyzer UNKNOWN",DisplayMessageType.ERROR);
            log.logger.PrintStackTrace(ex);
        }
       
    }
    
    private static MSGTYPE getMessageType(String msg)
    {
        MSGTYPE type = null;
        String[] parts = msg.split("\r");
        if(parts.length > 1 )
        {
            if(parts[1].startsWith("Q|"))
            {
                type= MSGTYPE.QUERY;
            }
            else if (parts[1].startsWith("PID|"))
            {
                type = MSGTYPE.RESULTS;
            }
            else
            {
                type =MSGTYPE.UNKNOWN;
            }
        }
        
        return type;
        
    }
    
    private static boolean SaveResults(String barcode,int MeasureID, float value)
     {
         
         
          boolean flag = false;       
          String send = BLIS.blis.saveResults(barcode,MeasureID,value,0);
          if(send.equalsIgnoreCase("1"))
           {
              flag = true;
           }
                          
         return flag;
         
     } 
    
    private static String getSpecimenFilter(int whichdata)
    {
        String data = "";
        xmlparser p = new xmlparser("configs/mindray/mindraybc5000.xml");
        try {
            data = p.getMindrayFilter(whichdata);           
        } catch (Exception ex) {
            Logger.getLogger(MindrayBC5000.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }    
     
    private static Message getReplyMessage(Message firstMessage, String type)
    {
        Message data  = null;
        xmlparser p = new xmlparser("configs/mindray/mindraybc5000.xml");
        try {
            data = p.getReplyMessage(firstMessage,type);
        } catch (Exception ex) {
            Logger.getLogger(MindrayBC5000.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return data;        
    }
    
}
