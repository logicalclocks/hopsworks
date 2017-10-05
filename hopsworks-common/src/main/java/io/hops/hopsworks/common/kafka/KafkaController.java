package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileOutputStream;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class KafkaController {

  @EJB
  private CertsFacade userCerts;
  @EJB
  private Settings settings;

  public String getKafkaCertPaths(Project project) {
    UserCerts userCert = userCerts.findUserCert(project.getName(), project.
            getOwner().getUsername());
    //Check if the user certificate was actually retrieved
    if (userCert.getUserCert() != null
            && userCert.getUserCert().length > 0
            && userCert.getUserKey() != null
            && userCert.getUserKey().length > 0) {

      File certDir = new File(settings.getHopsworksTrueTempCertDir() + "/" + project.getName());

      if (!certDir.exists()) {
        try {
          certDir.mkdirs();
        } catch (Exception ex) {

        }
      }
      try {
        FileOutputStream fos;
        fos = new FileOutputStream(certDir.getAbsolutePath() + "/keystore.jks");
        fos.write(userCert.getUserKey());
        fos.close();

        fos = new FileOutputStream(certDir.getAbsolutePath() + "/truststore.jks");
        fos.write(userCert.getUserCert());
        fos.close();

      } catch (Exception e) {

      }
      return certDir.getAbsolutePath();
    } else {
      return null;
    }
  }

}
