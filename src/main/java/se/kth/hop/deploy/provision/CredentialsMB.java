/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.provision;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 * Managed Bean that retrieves the PaaS credentials which are already registered for
 * this dashboard.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class CredentialsMB {
        private PaasCredentials credentials;
        @EJB
        private CredentialsFacade facade;
        
        public PaasCredentials getCredentials() {
        if (credentials == null) {
//            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            credentials = facade.find();
        }
        return credentials;
//            return facade.find();
    }
}
