package io.hops.hopsworks.common.dao.certificates;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class CertsFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public CertsFacade() throws Exception {
  }

  public UserCerts findUserCert(String projectName, String username) {
    TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findUserProjectCert", UserCerts.class);
    query.setParameter("projectname", projectName);
    query.setParameter("username", username);
    try {
      UserCerts res = query.getSingleResult();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new UserCerts();
  }

  public List<UserCerts> findAllUserCerts() {
    TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findAll", UserCerts.class);
    try {
      List<UserCerts> res = query.getResultList();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new ArrayList<>();
  }

  public List<UserCerts> findUserCertsByProjectId(String projectname) {
    TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findByProjectname", UserCerts.class);
    query.setParameter("projectname", projectname);
    try {
      List<UserCerts> res = query.getResultList();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new ArrayList<>();
  }

  public List<UserCerts> findUserCertsByUid(String username) {
    TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findByUsername", UserCerts.class);
    query.setParameter("username", username);
    try {
      List<UserCerts> res = query.getResultList();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new ArrayList<>();
  }

  public void persist(UserCerts uc) {
    em.persist(uc);
  }

  public List<ServiceCerts> findAllServiceCerts() {
    TypedQuery<ServiceCerts> query = em.createNamedQuery(
            "ServiceCerts.findAll", ServiceCerts.class);
    try {
      List<ServiceCerts> res = query.getResultList();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new ArrayList<>();
  }

  public List<ServiceCerts> findServiceCertsByName(String service) {
    TypedQuery<ServiceCerts> query = em.createNamedQuery(
            "ServiceCerts.findByServiceName", ServiceCerts.class);
    query.setParameter("serviceName", service);

    try {
      List<ServiceCerts> res = query.getResultList();
      return res;
    } catch (EntityNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    }
    return new ArrayList<>();
  }

  public void putUserCerts(String projectname, String username) throws IOException {
    FileInputStream kfin = new FileInputStream(new File("/tmp/"
            + projectname + "__" + username + "__kstore.jks"));
    FileInputStream tfin = new FileInputStream(new File("/tmp/"
            + projectname + "__" + username + "__tstore.jks"));

    byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
    byte[] tStoreBlob = ByteStreams.toByteArray(tfin);

    UserCerts uc = new UserCerts(projectname, username);
    uc.setUserKey(kStoreBlob);
    uc.setUserCert(tStoreBlob);
    em.persist(uc);
    em.flush();

  }

  public void putServiceCerts(String service) {
    try (FileInputStream kfin = new FileInputStream(new File("/tmp/"
            + service + "__kstore.jks"));
            FileInputStream tfin = new FileInputStream(new File("/tmp/"
                    + service + "__tstore.jks"))) {

      byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
      byte[] tStoreBlob = ByteStreams.toByteArray(tfin);

      ServiceCerts sc = new ServiceCerts(service);
      sc.setServiceKey(kStoreBlob);
      sc.setServiceCert(tStoreBlob);
      em.persist(sc);
      em.flush();

      // TODO - DO NOT SWALLOW EXCEPTIONS!!!
    } catch (FileNotFoundException e) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              e);
    } catch (IOException ex) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              ex);
    } catch (Throwable ex) {
      Logger.getLogger(CertsFacade.class.getName()).log(Level.SEVERE, null,
              ex);
    }
  }

  public void update(UserCerts uc) {
    em.merge(uc);
  }

  public void remove(UserCerts uc) {
    em.remove(uc);
  }

  public void removeUserProjectCerts(String projectname, String username) {
    UserCerts item = findUserCert(projectname, username);
    if (item != null) {
      UserCerts tmp = em.merge(item);
      remove(tmp);
    }
  }

  public void removeAllCertsOfAUser(String username) {
    List<UserCerts> items = findUserCertsByUid(username);
    if (items != null) {
      for (UserCerts uc : items) {
        UserCerts tmp = em.merge(uc);
        remove(tmp);
      }
    }
  }

  public void removeAllCertsOfAProject(String projectname) {
    List<UserCerts> items = findUserCertsByProjectId(projectname);
    if (items != null) {
      for (UserCerts uc : items) {
        UserCerts tmp = em.merge(uc);
        remove(tmp);
      }
    }
  }
}
