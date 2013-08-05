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
        private String environment;
        private boolean installPhase;
        private String loginUser;
        private int totalHosts;
        
        private List<BaremetalGroup> nodes = new ArrayList<BaremetalGroup>();
        private List<ChefAttributes>chefAttributes =new ArrayList<ChefAttributes>();

    public Baremetal() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
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

    public List<ChefAttributes> getChefAttributes() {
        return chefAttributes;
    }

    public void setChefAttributes(List<ChefAttributes> chefAttributes) {
        this.chefAttributes = chefAttributes;
    }

    public int getTotalHosts() {
        return totalHosts;
    }

    public void setTotalHosts(int totalHosts) {
        this.totalHosts = totalHosts;
    }

    
    @Override
    public String toString() {
        return "Baremetal{" + "name=" + name + ", environment=" + environment + ", installPhase=" 
                + installPhase + ", loginUser=" + loginUser + ", nodes=" + nodes + ", chefAttributes=" 
                + chefAttributes + '}';
    }
        
        
}
