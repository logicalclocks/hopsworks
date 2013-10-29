/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class Baremetal implements Serializable{
        private String name;
        private boolean installPhase;
        private String loginUser;
        private int totalHosts;
        private GlobalProperties global=new GlobalProperties();
        
        private List<BaremetalGroup> nodes = new ArrayList<BaremetalGroup>();
        

    public Baremetal() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInstallPhase() {
        return installPhase;
    }

    public void setInstallPhase(boolean installPhase) {
        this.installPhase = installPhase;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public List<BaremetalGroup> getNodes() {
        return nodes;
    }

    public void setNodes(List<BaremetalGroup> nodes) {
        this.nodes = nodes;
    }
   
    public int getTotalHosts() {
        return totalHosts;
    }

    public void setTotalHosts(int totalHosts) {
        this.totalHosts = totalHosts;
    }

    public GlobalProperties getGlobal() {
        return global;
    }

    public void setGlobal(GlobalProperties global) {
        this.global = global;
    }

    
    @Override
    public String toString() {
        return "Baremetal{" + "name=" + name +  ", installPhase=" 
                + installPhase + ", loginUser=" + loginUser + ", nodes=" + nodes + ", chefAttributes=" 
                + '}';
    }
        
        
}
