/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package TEXT;

/**
 *
 * @author GHSS-BLIS
 */
public class utilities {
    
    public String getFileName(String Format,String extension)
    {
       String name = "";
       if(!Format.contains("*"))
            name = system.utilities.getSystemDate(Format);
       
        if(null == extension || extension.isEmpty()) 
            return name;
        else
            return name +"."+extension;        
        
    }
    
}
