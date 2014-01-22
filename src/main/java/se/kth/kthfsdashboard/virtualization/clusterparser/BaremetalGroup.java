/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class BaremetalGroup implements Serializable {

//    private String service;
    private int number;
    private List<String> hosts;
    private List<String> services;
    private String bittorrent;
    private String chefAttributes;

    public BaremetalGroup() {
    }

//    public String getService() {
//        return service;
//    }
//
//    public void setService(String service) {
//        this.service = service;
//    }

    
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public String getStringHosts() {
        String temp = hosts.toString().replaceAll("\\[", "");
        temp = temp.replaceAll(" ", "");
        return temp.toString().replaceAll("\\]", "");
    }

    public void setStringHosts(String hosts) {

        String[] splittedHosts = hosts.split(",");
        setHosts(Arrays.asList(splittedHosts));
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> recipes) {
        this.services = recipes;
    }

    public String getBittorrent() {
        return bittorrent;
    }

    public void setBittorrent(String bittorrent) {
        this.bittorrent = bittorrent;
    }

    public String getChefAttributes() {
        return chefAttributes;
    }

    public void setChefAttributes(String chefAttributes) {
        this.chefAttributes = chefAttributes;
    }
    
    public String getStringRecipes() {
        String temp = services.toString().replaceAll("\\[", "");
        temp = temp.replaceAll(" ", "");
        return temp.toString().replaceAll("\\]", "");
    }
    
    public void setStringRecipes(String recipes){
        String[] splittedRecipes = recipes.split(",");
        setServices(Arrays.asList(splittedRecipes));
    }
    
    @Override
    public String toString() {
        return "BaremetalGroup{" + "securityGroup=" + services.get(0) + ", number="
                + number + ", hosts=" + hosts + ", roles=" + services
                + '}';
    }
}
