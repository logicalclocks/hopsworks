/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.virtualization.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a group of nodes in a cluster which have the same software deployed
 * on them. It has information of the number of nodes in this group, recipes to be run, ports to be opened
 * by the user and any override attributes for the recipes.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class NodeGroup implements Serializable {

//    private String service;
    private int number;
    private List<String> services;
    private List<Integer> authorizePorts;
    private String chefAttributes;
    private String bittorrent;

    public NodeGroup() {
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> recipes) {
        this.services = recipes;
    }

    public List<Integer> getAuthorizePorts() {
        return authorizePorts;
    }

    public void setAuthorizePorts(List<Integer> authorizePorts) {
        this.authorizePorts = authorizePorts;
    }

    public String getChefAttributes() {
        return chefAttributes;
    }

    public void setChefAttributes(String chefAttributes) {
        this.chefAttributes = chefAttributes;
    }

    public String getBittorrent() {
        return bittorrent;
    }

    public void setBittorrent(String bittorrent) {
        this.bittorrent = bittorrent;
    }

    public String getStringPorts() {
        String temp = authorizePorts.toString().replaceAll("\\[", "");
        temp = temp.replaceAll(" ", "");
        return temp.toString().replaceAll("\\]", "");
    }

    public void setStringPorts(String ports) {
        List<Integer> result = new ArrayList<Integer>();
        for(String port: ports.split(",")){
            result.add(new Integer(port));
        }
        setAuthorizePorts(result);
    }
    
    public String getStringRecipes() {
        String temp = services.toString().replaceAll("\\[", "");
        temp = temp.replaceAll(" ", "");
        return temp.toString().replaceAll("\\]", "");
    }

    public void setStringRecipes(String recipes) {
        String[] splittedRecipes = recipes.split(",");
        setServices(Arrays.asList(splittedRecipes));
    }

    @Override
    public String toString() {
        return "NodeGroup{" + "securityGroup=" + services.get(0) + ", number=" + number + ", roles=" + services + ", authorizePorts=" + authorizePorts + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + this.number;
        hash = 73 * hash + (this.services != null ? this.services.hashCode() : 0);
        hash = 73 * hash + (this.authorizePorts != null ? this.authorizePorts.hashCode() : 0);
        hash = 73 * hash + (this.chefAttributes != null ? this.chefAttributes.hashCode() : 0);
        hash = 73 * hash + (this.bittorrent != null ? this.bittorrent.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeGroup other = (NodeGroup) obj;
        if (this.number != other.number) {
            return false;
        }
        if (this.services != other.services && (this.services == null || !this.services.equals(other.services))) {
            return false;
        }
        if (this.authorizePorts != other.authorizePorts && (this.authorizePorts == null || !this.authorizePorts.equals(other.authorizePorts))) {
            return false;
        }
        if ((this.chefAttributes == null) ? (other.chefAttributes != null) : !this.chefAttributes.equals(other.chefAttributes)) {
            return false;
        }
        if ((this.bittorrent == null) ? (other.bittorrent != null) : !this.bittorrent.equals(other.bittorrent)) {
            return false;
        }
        return true;
    }
    
    
}
