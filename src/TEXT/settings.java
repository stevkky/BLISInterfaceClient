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
public class settings {
    public static String BASE_DIRECTORY;
    public static boolean USE_SUB_DIRECTORIES;
    public static String SUB_DIRECTORY_FORMAT;
    public static String FILE_NAME_FORMAT;
    public static String FILE_EXTENSION;
    public static String FILE_SEPERATOR;
    public static char SEPERATOR_CHAR;
    public static boolean DELETE_AFTER_READ;
    
    public static void setChar(String Seperator)
    {
        switch(Seperator)
        {
            case "TAB":
                SEPERATOR_CHAR = 0x09;
                break;
            case "COMMA":
                SEPERATOR_CHAR =0x2c;
                break;
            case "COLON":
                SEPERATOR_CHAR =0x3a;
                break;
            case "SEMI-COLON":
                SEPERATOR_CHAR =0x3b;
                break;
            case "SPACE":
                SEPERATOR_CHAR =0x20;
                break;
            case "ASTM":
                SEPERATOR_CHAR = 0x03;
                        break;
                
        }
    }
    
}
