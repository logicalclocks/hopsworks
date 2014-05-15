/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import se.kth.kthfsdashboard.user.UserFacade;

import se.kth.kthfsdashboard.user.Username;
/**
 *
 * @author roshan
 */
@FacesConverter("name")
public class InputConverter implements Converter {
    
    @EJB
    private UserFacade userFacade;
    private List<Username> username = new ArrayList<Username>();
    
    @PostConstruct
    protected void init(){
        username = getAllUsers();
    }
    
    public List<Username> getAllUsers() {
        return userFacade.findAll();
    }
        
    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String name) {
        
        if(username == null)
            init();
        
        if (name.trim().equals("")) {
            return null;
        } else {
            try {
                //int number = Integer.parseInt(submittedValue);
 
                for (Username un : username) {
                    if (un.getName().equalsIgnoreCase(name)) {
                        return un;
                    }
                }
                 
            } catch(Exception exception) {
                throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Not a valid player"));
            }
        }
                return null;
    
}
 
    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return String.valueOf(((Username) value).getName());
        }
    }
    
    
}
