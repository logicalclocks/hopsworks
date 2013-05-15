/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
//@Named(value = "provider")
@ManagedBean
@SessionScoped
//@RequestScoped
public class ProviderMB implements Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    private String providerName;
    private String groupName;
    private String publicKey;
    private String privateKey;
    private String dashboardUsername;
    private String dashboardPassword;
    private boolean showProvider = false;

    public ProviderMB() {
    }

    public String updateName() {
        showProvider = true;
        return "mainPage";
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean getShowProvider() {
        return showProvider;
    }


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
    
    public String getDashboardUsername() {
        return dashboardUsername;
    }

    public void setDashboardUsername(String dashboardUsername) {
        this.dashboardUsername = dashboardUsername;
    }

    public String getDashboardPassword() {
        return dashboardPassword;
    }

    public void setDashboardPassword(String dashboardPassword) {
        this.dashboardPassword = dashboardPassword;
    }

    public boolean notImplemented(){
        if(providerName.equals("aws-ec2")){
            return false;
        }
        return true;
    }
    
}
