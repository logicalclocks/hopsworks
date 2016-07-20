package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class YarnAppResultDTO implements Serializable{
    
    private YarnAppResult yarnAppResult;
    private long executionDuration;

    public YarnAppResultDTO(){}
    
    public YarnAppResultDTO(YarnAppResult yarnAppResult){
        this.yarnAppResult = yarnAppResult;
    }
    
    public YarnAppResultDTO(YarnAppResult yarnAppResult, long executionDuration){
        this.yarnAppResult = yarnAppResult;
        this.executionDuration = executionDuration;
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
     * @return the executionDuration
     */
    public long getExecutionDuration() {
        return executionDuration;
    }

    /**
     * @param executionDuration the executionDuration to set
     */
    public void setExecutionDuration(long executionDuration) {
        this.executionDuration = executionDuration;
    }
    
    
    
}
