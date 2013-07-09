/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
//@Entity
public class NodeGroup implements Serializable{
//    @Id
//    @GeneratedValue(strategy= GenerationType.AUTO)
//    private Long id;
    private String securityGroup;
    private int number;
    private List<String> roles;
    private List<String> authorizePorts;
//    @ManyToOne
//    @JoinColumn(name="CLUSTER_ID")
//    private Cluster cluster;

    public NodeGroup(){
        
    }
    
    public NodeGroup(String name){
        this.securityGroup=name;
        
    }
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Cluster getCluster() {
//        return cluster;
//    }

//    public void setCluster(Cluster cluster) {
//        this.cluster = cluster;
//    }
    
    

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getAuthorizePorts() {
        return authorizePorts;
    }

    public void setAuthorizePorts(List<String> authorizePorts) {
        this.authorizePorts = authorizePorts;
    }

    @Override
    public String toString() {
        return "NodeGroup{" + "securityGroup=" + securityGroup + ", number=" + number + ", roles=" + roles + ", authorizePorts=" + authorizePorts + '}';
    }
         
    
}
