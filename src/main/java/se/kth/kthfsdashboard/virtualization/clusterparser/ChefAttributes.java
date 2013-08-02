/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
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
public class ChefAttributes implements Serializable {

    private String role;
    private String chefJson;

    public ChefAttributes() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getChefJson() {
        return chefJson;
    }

    public void setChefJson(String chefJson) {
        this.chefJson = chefJson;
    }

    @Override
    public String toString() {
        return "ChefAttributes{" + "role=" + role + ", chefJson=" + chefJson + '}';
    }
}
