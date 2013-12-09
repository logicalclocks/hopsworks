/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class BbcViewController implements Serializable{
    private Username user;
    
    public BbcViewController(){
        
    }
    public String loadLims(){
        if(renderLims()){
            return "lims";
        }
        else{
            return "";
        }
    }
    
    public String loadWorkflows(){
        if(renderWorfkflows()){
            return "workflows";
        }
        else{
            return "";
        }
    }
    public String loadClusters(){
        if(renderClusters()){
            return "clusters";
        }
        else{
            return "";
        }
    }
    public boolean renderLims(){
        return user.getGroups().contains(Group.BBC_ADMIN);
    }
    
    public boolean renderWorfkflows(){
        return user.getGroups().contains(Group.ADMIN) 
                ||user.getGroups().contains(Group.BBC_RESEARCHER)
                ||user.getGroups().contains(Group.BBC_ADMIN);
    }
    
    public boolean renderClusters(){
        return user.getGroups().contains(Group.ADMIN);
    }

    public Username getUser() {
        return user;
    }

    public void setUser(Username user) {
        this.user = user;
    }
    
}
