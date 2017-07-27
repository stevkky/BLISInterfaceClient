/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hl7.Mindray;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author BLIS
 */
public class Segment {
    public String name;
    public String id;
    public String description;
    public int position;
    public int fieldlength;     
    public List<Field> Fields = new ArrayList<>();
    //get and setters are a waste of time for me
}
