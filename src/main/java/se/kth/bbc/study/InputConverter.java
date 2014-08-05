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
import se.kth.kthfsdashboard.user.AutocompleteMB;
import se.kth.kthfsdashboard.user.UserController;
import se.kth.kthfsdashboard.user.UserFacade;

import se.kth.kthfsdashboard.user.Username;
/**
 *
 * @author roshan
 */
@FacesConverter("nameConverter")
public class InputConverter implements Converter {
    
    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
    
         if(value != null && value.trim().length() > 0) {
            StudyMB aComplete = (StudyMB) facesContext.getExternalContext().getApplicationMap().get("studyManagedBean");
            List<Username> list= aComplete.getUsersname();
            String convertedVal = null;
            
            for(Username u: list){
                if(u.getName().startsWith(value))
                    convertedVal = value;
            }
                    return convertedVal;
        }
        else {
            return null;
        }
    }
 
    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
        if (object != null) {
            return String.valueOf(object);
            //String inputVal = (String)object;
//            char[] email = inputVal.toCharArray();
            //return inputVal;
            
        } else {
            return null;
        }
    }
    
    
}
