/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.NoneScoped;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author roshan
 */

@ManagedBean(name="autoCompleteBean")
@NoneScoped
public class AutocompleteMB implements Serializable {
    
    private List<Username> usernames;

    @EJB
    private UserFacade userFacade;

    public AutocompleteMB(){}
    
    @PostConstruct
    public void init(){
        usernames = getUsersNameList();
    }
    
    public List<Username> getUsersNameList() {
        return userFacade.findAllUsers();
    }
       
    public List<Username> getUsersname() {
        return usernames;
    }
       
}
