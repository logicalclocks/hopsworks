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
public class JobHeuristicDetailsDTO implements Serializable{
    
    private String appId;
    
    // The total severity of that job
    private String totalSeverity;

    // MemoryLimitHeuristic
    private String memorySeverity;
    private String totalDriverMemory, totalExecutorMemory, executorCores, executorMemory, memoryForStorage;
    
    // StageRuntimeHeuristic
    private String stageRuntimeSeverity;
    private String averageStageFailure, problematicStages, completedStages, failedStages;
    
    // JobRuntimeHeuristic
    private String jobRuntimeSeverity;
    private String averageJobFailure, completedJobsNumber, failedJobsNumber;
    
    
    public JobHeuristicDetailsDTO(){}
    
    public JobHeuristicDetailsDTO(String appId, String totalSeverity){
        this.appId = appId;
        this.totalSeverity = totalSeverity;
    }
    
    public JobHeuristicDetailsDTO(String appId, String totalSeverity, String totalDriverMemory,
                                  String totalExecutorMemory, String memoryForStorage){
        this.appId = appId;
        this.totalSeverity = totalSeverity;
        this.totalDriverMemory = totalDriverMemory;
        this.totalExecutorMemory = totalExecutorMemory;
        this.memoryForStorage = memoryForStorage;
    }

    /**
     * @return the appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId the appId to set
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return the totalSeverity
     */
    public String getTotalSeverity() {
        return totalSeverity;
    }

    /**
     * @param totalSeverity the totalSeverity to set
     */
    public void setTotalSeverity(String totalSeverity) {
        this.totalSeverity = totalSeverity;
    }

    /**
     * @return the totalDriverMemory
     */
    public String getTotalDriverMemory() {
        return totalDriverMemory;
    }

    /**
     * @param totalDriverMemory the totalDriverMemory to set
     */
    public void setTotalDriverMemory(String totalDriverMemory) {
        this.totalDriverMemory = totalDriverMemory;
    }

    /**
     * @return the totalExecutorMemory
     */
    public String getTotalExecutorMemory() {
        return totalExecutorMemory;
    }

    /**
     * @param totalExecutorMemory the totalExecutorMemory to set
     */
    public void setTotalExecutorMemory(String totalExecutorMemory) {
        this.totalExecutorMemory = totalExecutorMemory;
    }

    /**
     * @return the memoryForStorage
     */
    public String getMemoryForStorage() {
        return memoryForStorage;
    }

    /**
     * @param memoryForStorage the memoryForStorage to set
     */
    public void setMemoryForStorage(String memoryForStorage) {
        this.memoryForStorage = memoryForStorage;
    }

    /**
     * @return the executorCores
     */
    public String getExecutorCores() {
        return executorCores;
    }

    /**
     * @param executorCores the executorCores to set
     */
    public void setExecutorCores(String executorCores) {
        this.executorCores = executorCores;
    }

    /**
     * @return the executorMemory
     */
    public String getExecutorMemory() {
        return executorMemory;
    }

    /**
     * @param executorMemory the executorMemory to set
     */
    public void setExecutorMemory(String executorMemory) {
        this.executorMemory = executorMemory;
    }

    /**
     * @return the memorySeverity
     */
    public String getMemorySeverity() {
        return memorySeverity;
    }

    /**
     * @param memorySeverity the memorySeverity to set
     */
    public void setMemorySeverity(String memorySeverity) {
        this.memorySeverity = memorySeverity;
    }

    /**
     * @return the stageRuntimeSeverity
     */
    public String getStageRuntimeSeverity() {
        return stageRuntimeSeverity;
    }

    /**
     * @param stageRuntimeSeverity the stageRuntimeSeverity to set
     */
    public void setStageRuntimeSeverity(String stageRuntimeSeverity) {
        this.stageRuntimeSeverity = stageRuntimeSeverity;
    }

    /**
     * @return the averageStageFailure
     */
    public String getAverageStageFailure() {
        return averageStageFailure;
    }

    /**
     * @param averageStageFailure the averageStageFailure to set
     */
    public void setAverageStageFailure(String averageStageFailure) {
        this.averageStageFailure = averageStageFailure;
    }

    /**
     * @return the problematicStages
     */
    public String getProblematicStages() {
        return problematicStages;
    }

    /**
     * @param problematicStages the problematicStages to set
     */
    public void setProblematicStages(String problematicStages) {
        this.problematicStages = problematicStages;
    }

    /**
     * @return the completedStages
     */
    public String getCompletedStages() {
        return completedStages;
    }

    /**
     * @param completedStages the completedStages to set
     */
    public void setCompletedStages(String completedStages) {
        this.completedStages = completedStages;
    }

    /**
     * @return the failedStages
     */
    public String getFailedStages() {
        return failedStages;
    }

    /**
     * @param failedStages the failedStages to set
     */
    public void setFailedStages(String failedStages) {
        this.failedStages = failedStages;
    }

    /**
     * @return the jobRuntimeSeverity
     */
    public String getJobRuntimeSeverity() {
        return jobRuntimeSeverity;
    }

    /**
     * @param jobRuntimeSeverity the jobRuntimeSeverity to set
     */
    public void setJobRuntimeSeverity(String jobRuntimeSeverity) {
        this.jobRuntimeSeverity = jobRuntimeSeverity;
    }

    /**
     * @return the averageJobFailure
     */
    public String getAverageJobFailure() {
        return averageJobFailure;
    }

    /**
     * @param averageJobFailure the averageJobFailure to set
     */
    public void setAverageJobFailure(String averageJobFailure) {
        this.averageJobFailure = averageJobFailure;
    }

    /**
     * @return the completedJobsNumber
     */
    public String getCompletedJobsNumber() {
        return completedJobsNumber;
    }

    /**
     * @param completedJobsNumber the completedJobsNumber to set
     */
    public void setCompletedJobsNumber(String completedJobsNumber) {
        this.completedJobsNumber = completedJobsNumber;
    }

    /**
     * @return the failedJobsNumber
     */
    public String getFailedJobsNumber() {
        return failedJobsNumber;
    }

    /**
     * @param failedJobsNumber the failedJobsNumber to set
     */
    public void setFailedJobsNumber(String failedJobsNumber) {
        this.failedJobsNumber = failedJobsNumber;
    }
    
    
    
    
}
