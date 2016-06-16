/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vasileios
 */

@XmlRootElement
public class JobDetailDTO implements Serializable{
    
    private String className;
    
    private String selectedJar;
    
    private String inputArgs;
    
    private String jobType;
    
    public JobDetailDTO(){
    }
    
    public JobDetailDTO(String className){
        this.className = className;
    }
    
    public JobDetailDTO(String className, String selectedJar, String inputArgs, String jobType){
        this.className = className;
        this.selectedJar = selectedJar;
        this.inputArgs = inputArgs;
        this.jobType = jobType;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the selectedJar
     */
    public String getSelectedJar() {
        return selectedJar;
    }

    /**
     * @param selectedJar the selectedJar to set
     */
    public void setSelectedJar(String selectedJar) {
        this.selectedJar = selectedJar;
    }

    /**
     * @return the inputArgs
     */
    public String getInputArgs() {
        return inputArgs;
    }

    /**
     * @param inputArgs the inputArgs to set
     */
    public void setInputArgs(String inputArgs) {
        this.inputArgs = inputArgs;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType the jobType to set
     */
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    
}
