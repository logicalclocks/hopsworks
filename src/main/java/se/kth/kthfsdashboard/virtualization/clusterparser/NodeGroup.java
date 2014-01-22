/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
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
}
