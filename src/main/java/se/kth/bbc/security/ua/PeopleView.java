/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.security.ua.model.People;


@ManagedBean(name="dtPeopleView")
@ViewScoped
public class PeopleView implements Serializable {
     
    private List<People> people;
     
    private UserManager service;
 
    @PostConstruct
    public void init() {
        people = service.findInactivateUsers();
    }
     
    public List<People> getPeople() {
        return people;
    }
 
}
