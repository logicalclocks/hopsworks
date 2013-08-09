package se.kth.kthfsdashboard.struct;

import java.io.Serializable;
import se.kth.kthfsdashboard.utils.FormatUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */


public class NodesTableItem implements Serializable {

    private Integer nodeId;
    private String status;
    private Long upTime;
    private Integer startPhase;
    private Integer configGeneration;

    public NodesTableItem(Integer nodeId, String status, Long uptime, Integer startPhase, Integer configGeneration) {

        this.nodeId = nodeId;
        this.status = status;
        this.upTime = uptime;
        this.startPhase = startPhase;
        this.configGeneration = configGeneration;
    }

   public Integer getNodeId() {
      return nodeId;
   }

   public void setNodeId(Integer nodeId) {
      this.nodeId = nodeId;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getUpTime() {
      return FormatUtils.timeInSec(upTime);
   }

   public void setUpTime(Long upTime) {
      this.upTime = upTime;
   }

   public Integer getStartPhase() {
      return startPhase;
   }

   public void setStartPhase(Integer startPhase) {
      this.startPhase = startPhase;
   }

   public Integer getConfigGeneration() {
      return configGeneration;
   }

   public void setConfigGeneration(Integer configGeneration) {
      this.configGeneration = configGeneration;
   }

}
