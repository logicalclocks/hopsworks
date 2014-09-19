/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

/**
 *
 * @author roshan
 */
public class SampleIdDisplay {
  
    private String id;   
    private String displayName;  
   
    public SampleIdDisplay() {}
 
    public SampleIdDisplay(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
 
    public String getId() {
        return id;
    }
 
    public void setId(String id) {
        this.id = id;
    }
 
    public String getDisplayName() {
        return displayName;
    }
 
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
     
    @Override
    public String toString() {
        return  id ;
    }
}
    
    

