/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Entity
@Table(name="NodeProgress")
@NamedQueries({
    @NamedQuery(name = "NodeProgression.findAll", query = "SELECT c FROM NodeProgression c"),
    @NamedQuery(name ="NodeProgression.findAllInGroup", query = "SELECT c FROM NodeProgression"
        + " c WHERE c.nodeId = :nodeIdREGEX"),
        @NamedQuery(name="NodeProgression.findNodeByNodeID", query= 
        "SELECT c FROM NodeProgression c WHERE c.nodeId = :id")
       
})
public class NodeProgression implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String cluster;
    private String nodeId;
    private String nodeRole;
    private String phase;
    private String previousPhase;
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRole() {
        return nodeRole;
    }

    public void setRole(String role) {
        this.nodeRole = role;
    }

    public String getPhase() {
        return phase;
    }

    public String getPreviousPhase() {
        return previousPhase;
    }

    public void setPreviousPhase(String previousPhase) {
        this.previousPhase = previousPhase;
    }
    
     

    public void setPhase(String phase) {
        this.phase = phase;
    }   

    @Override
    public int hashCode() {
        int hash = 17;
        hash += (id != null ? id.hashCode() : 0);
        hash += (cluster != null ? cluster.hashCode() : 0);
        hash += (nodeRole != null ? nodeRole.hashCode() : 0);
        hash += (phase != null ? phase.hashCode() : 0);
        hash += (nodeId != null ? nodeId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof NodeProgression)) {
            return false;
        }
        NodeProgression other = (NodeProgression) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.virtualization.nodeProgression[ id=" + id + " ]";
    }
    
}
