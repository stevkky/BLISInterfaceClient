/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hl7.Mindray;

/**
 *
 * @author BLIS
 */
public enum MessageType 
{
     OBSERVE_RESULT("ORU^R01"),
     RESULT_ACKNOWLEDGED ("ACK^R01"),
     QUERY("QRY^Q02"),
     QUERY_ACKNOWLEDGED("QCK^Q02"),
     DISPLAY_RESPONSE("DSR^Q03"),
     RESPONSE_ACKNOWLEDGED("ACK^Q03");
    
     private String type;
     private MessageType(String type)
     {
         this.type = type;
     }
     
     @Override
     public String toString()
     {
         return this.type;
     }
       
}

