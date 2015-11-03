package se.kth.bbc.security.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import se.kth.rest.application.config.VariablesFacade;

/**
 * This class is used to switch on/off the two factor authentication.
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class CustomAuthenticationSwitch implements Serializable {

  private static final long serialVersionUID = 1L;
  
  @EJB 
  private VariablesFacade vf;
  
  public boolean isOtpEnabled() {
    return vf.findById("twofactor_auth").getValue().equals("true");
  }

  
  
  public boolean isOtpEnabledFromPropFile() {

    Properties prop = new Properties();
    try {
      // Load the two factor authentication property file
      InputStream inputStream
              = getClass().getClassLoader().getResourceAsStream(
                      "clientcauth.properties");
      prop.load(inputStream);

    } catch (IOException ex) {

    }

    // Check if custom realm is enabled if not disable the gui
    if (!"true".equals(prop.getProperty("cauth-realm-enabled"))) {
      Logger.getLogger(CustomAuthenticationSwitch.class.getName()).log(
              Level.INFO, "## Custom authentication configuration disabled");
      return false;
    }

    return true;
  }

}
