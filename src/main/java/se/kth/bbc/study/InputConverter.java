/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import javax.faces.bean.ManagedProperty;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
/**
 *
 * @author roshan
 */
@FacesConverter("nameConverter")
public class InputConverter implements Converter {
    
    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        if(value != null && value.trim().length() > 0) {
            StudyMB service = (StudyMB) facesContext.getExternalContext().getSessionMap().get("studyManagedBean");
            return service.getThemes().get(Integer.parseInt(value));
        }
        else {
            return null;
        }
        
}
 
    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return String.valueOf(((Theme) value).getId());
        }
    }
    
    
    
}
