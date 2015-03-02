/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class CustomAuthenticationSwitch implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isOtpEnabled() {

        Properties prop = new Properties();
        try {
            InputStream inputStream
                    = getClass().getClassLoader().getResourceAsStream("clientcauth.properties");
            prop.load(inputStream);
 
        } catch (IOException ex) {
            //Logger.getLogger(CustomAuthenticationSwitch.class.getName()).log(Level.SEVERE, "##Could not read the authentication configuration", ex);
        }

     
                   
        // check if custom realm is enabled if not disable the gui
        if (!"true".equals(prop.getProperty("cauth-realm-enabled"))) {
            //Logger.getLogger(CustomAuthenticationSwitch.class.getName()).log(Level.INFO, "## Custom authentication configuration disabled");
            return false;
        }
        
        return true;
    }

}
