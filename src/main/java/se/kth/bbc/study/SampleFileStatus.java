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
public enum SampleFileStatus {
    
    UPLOADING("uploading"),
    COPYING_TO_HDFS("copying_to_hdfs"),
    AVAILABLE("available");
    
    String status;
   
    SampleFileStatus(String status){
        this.status = status;
    }
  
    public String getFileStatus() {
        return this.status;
    }

}
