package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class YarnAppResultDTO implements Serializable{
    
    private YarnAppResult yarnAppResult;
    private long totalExecutionDuration, runningTime;
    private String ownerFullName;

    public YarnAppResultDTO(){}
    
    public YarnAppResultDTO(YarnAppResult yarnAppResult){
        this.yarnAppResult = yarnAppResult;
    }
    
    public YarnAppResultDTO(YarnAppResult yarnAppResult, long totalExecutionDuration, long runningTime){
        this.yarnAppResult = yarnAppResult;
        this.totalExecutionDuration = totalExecutionDuration;
        this.runningTime = runningTime;
    }
    
    /**
     * @return the yarnAppResult
     */
    public YarnAppResult getYarnAppResult() {
        return yarnAppResult;
    }

    /**
     * @param yarnAppResult the yarnAppResult to set
     */
    public void setYarnAppResult(YarnAppResult yarnAppResult) {
        this.yarnAppResult = yarnAppResult;
    }

    /**
     * @return the totalExecutionDuration
     */
    public long getTotalExecutionDuration() {
        return totalExecutionDuration;
    }

    /**
     * @param totalExecutionDuration the totalExecutionDuration to set
     */
    public void setTotalExecutionDuration(long totalExecutionDuration) {
        this.totalExecutionDuration = totalExecutionDuration;
    }

    /**
     * @return the runningTime
     */
    public long getRunningTime() {
        return runningTime;
    }

    /**
     * @param runningTime the runningTime to set
     */
    public void setRunningTime(long runningTime) {
        this.runningTime = runningTime;
    }

    /**
     * @return the ownerFullName
     */
    public String getOwnerFullName() {
        return ownerFullName;
    }

    /**
     * @param ownerFullName the ownerFullName to set
     */
    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }
    
}
