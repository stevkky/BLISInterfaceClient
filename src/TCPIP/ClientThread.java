/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TCPIP;
import java.io.*;
import java.net.*;
import java.util.*;
import log.*;
import ui.MainForm;

/**
 *
 * @author BLIS
 */
class ClientThread extends Thread
{   
    String read;   
    BufferedReader inFromEquipment=null;   
    Socket connSock = null;   
    String Equipmentname=null;
    private static final char CARRIAGE_RETURN = 13; 
    private static final char STX = 0x02;
    private static final char ACK = 0x06;
    private static final char EOT = 0x04;
    private static final char NAK = 0x15;
    private static final char NUL = 0x00;
    private static final char ENQ = 0x05;
    private static final char ETX = 0x03;  
    private static final char ETB = 0x17;
    public ClientThread( Socket conn, String Equipment)
    {
         this.connSock= conn;    
         this.Equipmentname = Equipment;
         //System.out.println("Client instance created");
         //log.AddToDisplay.Display("Client instance created", log.DisplayMessageType.INFORMATION);
         //logger.Logger("Client instance created");
         
    }
    @Override
    public void run() {
       try
       {
         
            System.out.println("Client has started");
            log.AddToDisplay.Display("Client thread has started", log.DisplayMessageType.INFORMATION);
            logger.Logger("Client thread has started");
           
           String input ="";
            while(true)
            {
                try
                {
                  inFromEquipment=new BufferedReader(new InputStreamReader (connSock.getInputStream()));
                 
                    read = "";
                    if(this.Equipmentname.equalsIgnoreCase("Mindray BS-200E") || this.Equipmentname.equalsIgnoreCase("Mindray BS-300") || this.Equipmentname.equalsIgnoreCase("Mindray BC-3600")
                            || this.Equipmentname.equalsIgnoreCase("Mindray BC-5380") || this.Equipmentname.equalsIgnoreCase("Mindray BS-240"))
                    {
                        while((input = inFromEquipment.readLine()).length()> 1)
                        {

                          read = read + input + "<::>";   
                             //count++;
                        }
                    }
                    else if (this.Equipmentname.equalsIgnoreCase("SYSMEX XS-500I") || this.Equipmentname.equalsIgnoreCase("COBASAMPLIPREP")
                            || this.Equipmentname.equalsIgnoreCase("SYSMEX XN-1000"))
                    {
                        int c=0;
                        int val;
                        String line ="";
                        while((val = inFromEquipment.read()) > -1)
                        {                     
                          if(val != 13)
                            line = line + (char)val;   
                          else
                          {
                             line = line + "\r";
                             read = read + line;
                             if(line.startsWith("L|1|N"))
                                 break;
                             line ="";                             
                             c++;
                          }
                          /*if(c>=29)
                              break;*/
                        }
                    }
                    else if(this.Equipmentname.equalsIgnoreCase("BT3000PlUSChameleon") || this.Equipmentname.equalsIgnoreCase("SYSMEX XT-2000i")
                            || this.Equipmentname.equalsIgnoreCase("FLEXOR JUNIOR") || this.Equipmentname.equalsIgnoreCase("BT3000PlusChameleon v1.0.7"))
                    {
                        int c=0;
                        int val;
                        String line ="";
                        while((val = inFromEquipment.read()) > -1)
                        {                     
                          if(val != 13)
                          {
                            line = line + (char)val; 
                           // log.AddToDisplay.Display((char)val+"",0);
                             if((char)val == ACK || (char)val == ENQ || (char)val == NAK || (char)val == EOT || (char)val == ETX)
                             {
                                 read = read + line;
                                 break;
                             }
                                 
                          }
                          else
                          {
                             line = line + "\r";
                             read = read + line;
                             if(line.startsWith("L|1|N") || line.endsWith("L|1|N"))
                                 break;                            
                             line ="";                             
                             c++;
                          }
                          /*if(c>=29)
                              break;*/
                        }
                    }
                    else if(this.Equipmentname.equalsIgnoreCase("GENEXPERT") || (this.Equipmentname.equalsIgnoreCase("SelectraProS")) || (this.Equipmentname.equalsIgnoreCase("URIT 5250")))
                    {
                        int c=0;
                        int val;
                        String line ="";
                        while((val = inFromEquipment.read()) > -1)
                        { 
                          if(val != 13)
                          {
                            line = line + (char)val;  
                             if((char)val == ACK || (char)val == ENQ || (char)val == NAK || (char)val == EOT || (char)val == ETX|| (char)val == ETB)
                             {
                                 read = read + line;
                                 break;
                             }
                            
                          }
                          else
                          {
                             line = line + "\r";
                             read = read + line;                                                       
                             line ="";                             
                             c++;
                          }                            
                        }
                    }                   
                    else
                    {
                        while((input = inFromEquipment.readLine())!= null)
                        {

                          read = read + input + "\r";   
                             //count++;
                        }
                    }
               
                 
                }catch(Exception ex)
                {                   
                     logger.Logger(ex.getMessage());
                     logger.PrintStackTrace(ex);
                     log.AddToDisplay.Display(ex.getMessage(), log.DisplayMessageType.ERROR);
                     
                     if(connSock.isClosed())
                     {                         
                         log.AddToDisplay.Display("SOcket Closed", log.DisplayMessageType.ERROR);
                        // log.AddToDisplay.Display("Trying to reset the connection", log.DisplayMessageType.INFORMATION);
                      
                     }  
                     else
                     {
                         log.AddToDisplay.Display("The socket has been closed at the other end", log.DisplayMessageType.ERROR);
                         try
                         {
                            connSock.close();
                         }
                         catch(IOException exx)
                         {
                            logger.Logger(ex.getMessage());
                            logger.PrintStackTrace(ex);
                         }
                        
                         
                     }
                     
                     break;
                }                  
                 
                  if(!read.isEmpty())
                  {
                    log.AddToDisplay.Display("New message recieved", log.DisplayMessageType.TITLE);     
                    log.AddToDisplay.Display(read, log.DisplayMessageType.INFORMATION);         
                    system.utilities.writetoFile(read.replaceAll("<::>", "\r"));
                  
                    switch(this.Equipmentname)
                    {
                        case "Mindray BS-200E":
                            MindrayBS200E.handleMessage(read);
                            break;
                        case "Mindray BS-300":
                            MindrayBS300.handleMessage(read);
                            break;
                        case "BT3000PlUSChameleon":
                            BT3000PlusChameleon.handleMessage(read);
                        case "BT3000PlusChameleon v1.0.7":
                            BT3000PlusChameleon1_0_7.handleMessage(read);
                            break;
                        case "SYSMEX XS-500i":
                            SYSMEXXS500i.handleMessage(read);
                            break;
                        case "FLEXOR JUNIOR":
                            FlexorJunior.handleMessage(read);
                            break;
                        case "CobasAmpliPrep":
                            CobasAmpliPrep.handleMessage(read);
                            break;
                        case "GENEXPERT":
                            GeneXpert.handleMessage(read);
                            break;
                        case "SYSMEX XT-2000i":
                            SYSMEXXT2000i.handleMessage(read);
                            break;
                        case "SYSMEX XN-1000":
                            SYSMEXXN1000.handleMessage(read);
                            break;
                        case "SelectraProS":
                            SelectraProS.handleMessage(read);
                            break;                                
                        case "Mindray BC-3600":
                            TCPIP.MindrayBC3600.handleMessage(read);
                            break;
                        case "Mindray BC-5380":
                           TCPIP.MindrayBC5380.handleMessage(read);
                            break;
                        case "URIT 5250":
                            TCPIP.URIT5250.handleMessage(read);
                            break;
                        case "Mindray BS-240":
                            TCPIP.MindrayBS240.handleMessage(read);
                            break;
                                
                    }   
                  }
                            
            }
           
       }catch(Exception e){
           logger.Logger(e.getMessage());
           logger.PrintStackTrace(e);
           log.AddToDisplay.Display(e.getMessage(), log.DisplayMessageType.ERROR);
       }
    }
    
}
