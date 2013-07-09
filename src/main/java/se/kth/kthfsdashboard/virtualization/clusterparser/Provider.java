/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
//@Embeddable
//@Access(AccessType.FIELD)
public class Provider implements Serializable{
//    @Column(name="PROVIDER_NAME")
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
    private List<String> zones;
   
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

    public List<String> getZones() {
        return zones;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
    }     

    @Override
    public String toString() {
        return "Provider{" + "name=" + name + ", instanceType=" + instanceType + ", image=" + image + ", zones=" + zones + '}';
    }

}
