/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;

/**
 * Represents a cloud provider with the required configuration parameters to configure
 * all the nodes in the cloud.
 * 
 * This includes the name of the provider, the instance type for the VMs, the user to login in them, the 
 * OS image to request and the region to be deployed in the data center.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class Provider implements Serializable {

    private String name;
    private String instanceType;
    private String loginUser;
    private String image;
    private String region;

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public String toString() {
        return "Provider{" + "name=" + name + ", instanceType=" + instanceType + ", image=" + image + " '}'";
    }
}
