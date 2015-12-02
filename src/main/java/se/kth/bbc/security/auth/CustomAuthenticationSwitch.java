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
import se.kth.hopsworks.util.Settings;

@ManagedBean
@RequestScoped
public class CustomAuthenticationSwitch implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private Settings settings;

  public boolean isOtpEnabled() {
    return settings.findById("twofactor_auth").getValue().equals("true");
  }

}
