/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * Managed Bean that retrieves the PaaS credentials which are already registered for
 * this dashboard.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class CredentialsMB {
        private PaaSCredentials credentials;
        @EJB
        private CredentialsFacade facade;
        
        public PaaSCredentials getCredentials() {
        if (credentials == null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            credentials = facade.find();
        }
        return credentials;
    }
}
