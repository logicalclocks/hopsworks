/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

/**
 *
 * @author roshan
 */

public class ChangeListener implements ValueChangeListener {
    
    @Override
    public void processValueChange(ValueChangeEvent event) throws AbortProcessingException {
        ValueChangeMB teamRole = (ValueChangeMB) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("valueChangeMB"); 
        teamRole.setNewTeamRole(event.getNewValue().toString());
        System.out.println(" new value from Listener ===== "+ event.getNewValue().toString());
        System.out.println(" new value from Listener ===== "+ event.getOldValue().toString());
        
   }
}
