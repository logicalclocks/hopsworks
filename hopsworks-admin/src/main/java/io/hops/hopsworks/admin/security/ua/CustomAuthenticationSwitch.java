package io.hops.hopsworks.admin.security.ua;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.util.Settings;

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
