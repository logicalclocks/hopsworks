package io.hops.hopsworks.cluster.controller;

import io.hops.hopsworks.cluster.ClusterDTO;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;
import io.hops.hopsworks.common.dao.user.cluster.RegistrationStatusEnum;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.UserStatusValidator;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.security.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.security.cert.CertificateException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterController {

  private final static Logger LOGGER = Logger.getLogger(ClusterController.class.getName());
  private final static String CLUSTER_NAME_PREFIX = "Agent";
  private final static String CLUSTER_GROUP = "CLUSTER_AGENT";
  public final static long VALIDATION_KEY_EXPIRY_DATE = 48l;//hours to validate request
  public final static long VALIDATION_KEY_EXPIRY_DATE_MS = 48l * 36l * 100000l;//milisecond to validate request
  private final static int VALIDATION_KEY_LEN = 64;

  public static enum OP_TYPE {
    REGISTER,
    UNREGISTER
  }
  @EJB
  private UserFacade userBean;
  @EJB
  private ClusterCertFacade clusterCertFacade;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;
  @EJB
  private AuditManager am;
  @EJB
  private UsersController userController;
  @EJB
  private UserStatusValidator statusValidator;

  public void register(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidNewCluster(cluster);
    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName());
    if (clusterCert != null) {
      throw new IllegalArgumentException(
          "Cluster with the same Organization and Organization unit name alrady registerd.");
    }
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent != null) {
      throw new IllegalArgumentException("User email already registerd.");
    }
    String agentName = getAgentName();
    clusterAgent = new Users();
    clusterAgent.setUsername(agentName);
    clusterAgent.setEmail(cluster.getEmail());
    clusterAgent.setFname(cluster.getCommonName());
    clusterAgent.setLname(CLUSTER_NAME_PREFIX);
    clusterAgent.setTitle("Mrs");
    clusterAgent.setStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT);
    clusterAgent.setMode(PeopleAccountType.M_ACCOUNT_TYPE);
    clusterAgent.setPassword(DigestUtils.sha256Hex(cluster.getChosenPassword()));
    clusterAgent.setMaxNumProjects(0);

    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    Integer gid = groupFacade.lastGroupID() + 1;
    if (group == null) {
      group = new BbcGroup(gid, CLUSTER_GROUP);//do this in chef?
      group.setGroupDesc("Clusters outside the system");
      groupFacade.save(group);
    }

    List<BbcGroup> groups = new ArrayList<>();
    groups.add(group);
    clusterAgent.setBbcGroupCollection(groups);
    userBean.persist(clusterAgent);

    clusterCert = new ClusterCert(cluster.getCommonName(), cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName(), RegistrationStatusEnum.REGISTRATION_PENDING, clusterAgent);
    clusterCert.setValidationKey(SecurityUtils.getRandomPassword(VALIDATION_KEY_LEN));
    clusterCert.setValidationKeyDate(new Date());
    clusterCertFacade.save(clusterCert);
    sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent,
        AccountsAuditActions.REGISTRATION.name());
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", new Object[]{clusterAgent.getEmail(),
      clusterAgent.getUsername()});
  }

  public void registerCluster(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidCluster(cluster);
    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName());
    if (clusterCert != null) {
      throw new IllegalArgumentException(
          "Cluster with the same Organization and Organization unit name alrady registerd.");
    }
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("User not registerd.");
    }
    checkUserPasswordAndStatus(cluster, clusterAgent, req);
    clusterCert = new ClusterCert(cluster.getCommonName(), cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName(), RegistrationStatusEnum.REGISTRATION_PENDING, clusterAgent);
    clusterCert.setValidationKey(SecurityUtils.getRandomPassword(VALIDATION_KEY_LEN));
    clusterCert.setValidationKeyDate(new Date());
    clusterCertFacade.save(clusterCert);
    sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent,
        AccountsAuditActions.REGISTRATION.name());
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", new Object[]{clusterAgent.getEmail(),
      clusterAgent.getUsername()});
  }

  public void unregister(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName());
    if (clusterCert == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    if (clusterCert.getRegistrationStatus().equals(RegistrationStatusEnum.UNREGISTRATION_PENDING) && getDateDiffHours(
        clusterCert.getValidationKeyDate()) < VALIDATION_KEY_EXPIRY_DATE) {
      throw new IllegalArgumentException(
          "Cluster unregisterd use the validation key sent to you via email to complete unregistration.");
    }
    if (!isOnlyClusterAgent(clusterAgent)) {
      throw new IllegalArgumentException("Not a cluster agent.");
    }
    checkUserPasswordAndStatus(cluster, clusterAgent, req);

    clusterCert.setValidationKey(SecurityUtils.getRandomPassword(VALIDATION_KEY_LEN));
    clusterCert.setRegistrationStatus(RegistrationStatusEnum.UNREGISTRATION_PENDING);
    clusterCert.setValidationKeyDate(new Date());
    clusterCertFacade.update(clusterCert);
    sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent,
        AccountsAuditActions.UNREGISTRATION.name());
    LOGGER.log(Level.INFO, "Unregistering cluster with email: {0}", clusterAgent.getEmail());
  }

  public void validateRequest(String key, HttpServletRequest req, OP_TYPE type) throws IOException,
      FileNotFoundException, InterruptedException, CertificateException {
    Integer clusterCertId = extractClusterCertId(key);
    ClusterCert clusterCert = clusterCertFacade.find(clusterCertId);
    if (clusterCert == null) {
      throw new IllegalStateException("Agent not found.");
    }
    long diff = getDateDiffHours(clusterCert.getValidationKeyDate());
    String validationKey = extractValidationKey(key);
    Users agent = clusterCert.getAgentId();
    if (agent == null) {
      throw new IllegalStateException("Agent not found.");
    }
    if (!validationKey.equals(clusterCert.getValidationKey())) {
      throw new IllegalStateException("Validation key not found.");
    }
    if (diff > VALIDATION_KEY_EXPIRY_DATE) {
      removeUserIfNotValidated(agent);
      throw new IllegalStateException("Expired valdation key.");
    }
    if (type.equals(OP_TYPE.REGISTER) && clusterCert.getRegistrationStatus().equals(
        RegistrationStatusEnum.REGISTRATION_PENDING)) {
      if (agent.getStatus() == PeopleAccountStatus.NEW_MOBILE_ACCOUNT) {
        agent.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT);
        userBean.update(agent);
      }
      clusterCert.setValidationKey(null);
      clusterCert.setValidationKeyDate(null);
      clusterCert.setRegistrationStatus(RegistrationStatusEnum.REGISTERED);
      clusterCertFacade.update(clusterCert);
    } else if (clusterCert.getRegistrationStatus().equals(RegistrationStatusEnum.UNREGISTRATION_PENDING)) {
      revokeCert(clusterCert, true);
      removeClusterCert(clusterCert);
    }
  }

  private void removeClusterCert(ClusterCert clusterCert) {
    List<ClusterCert> clusterCerts = clusterCertFacade.getByAgent(clusterCert.getAgentId());
    if (clusterCerts.size() > 1) {
      clusterCertFacade.remove(clusterCert);
      LOGGER.log(Level.INFO, "Removed cluster {0} for user: {1}", new Object[]{clusterCert.getCommonName(), clusterCert.
        getAgentId().getEmail()});
      return;
    }
    LOGGER.log(Level.INFO, "Removing user: {0}", clusterCert.getAgentId().getEmail());
    userBean.removeByEmail(clusterCert.getAgentId().getEmail());
  }

  public void cleanupUnverifiedUsers() {
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    if (group == null) {
      return;
    }
    List<Integer> usersInGroup = userBean.findAllInGroup(group.getGid());
    Users u;
    for (Integer uid : usersInGroup) {
      u = userBean.find(uid);
      removeUserIfNotValidated(u);
    }
  }

  public List<ClusterCert> getAllClusters(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    if (cluster == null) {
      throw new NullPointerException("Cluster not assigned.");
    }
    if (cluster.getEmail() == null || cluster.getEmail().isEmpty()) {
      throw new IllegalArgumentException("Cluster email not set.");
    }
    if (cluster.getChosenPassword() == null || cluster.getChosenPassword().isEmpty()) {
      throw new IllegalArgumentException("Cluster password not set.");
    }
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("No registerd cluster found for user.");
    }
    checkUserPasswordAndStatus(cluster, clusterAgent, req);
    return clusterCertFacade.getByAgent(clusterAgent);
  }

  public ClusterCert getCluster(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    checkUserPasswordAndStatus(cluster, clusterAgent, req);
    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(cluster.getOrganizationName(), cluster.
        getOrganizationalUnitName());
    if (clusterCert == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    return clusterCert;
  }

  private void checkUserPasswordAndStatus(ClusterDTO cluster, Users clusterAgent, HttpServletRequest req) throws
      MessagingException {
    String password = DigestUtils.sha256Hex(cluster.getChosenPassword());
    if (!password.equals(clusterAgent.getPassword())) {
      userController.registerFalseLogin(clusterAgent);//will set status if false login > allowed
      am.registerLoginInfo(clusterAgent, UserAuditActions.LOGIN.name(), UserAuditActions.FAILED.name(), req);
      throw new SecurityException("Incorrect password.");
    }
    try {
      statusValidator.checkStatus(clusterAgent.getStatus());
    } catch (AppException ex) {
      LOGGER.log(Level.WARNING, "User {0} with state {1} trying to login.", new Object[]{clusterAgent.getEmail(),
        clusterAgent.getStatus()});
      throw new SecurityException(ex.getMessage());
    }
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    if (!clusterAgent.getBbcGroupCollection().contains(group)) {
      throw new SecurityException("User not allowed to register clusters.");
    }
    userController.resetFalseLogin(clusterAgent);
  }

  private void removeUserIfNotValidated(Users u) {
    if (u == null) {
      return;
    }
    if (!isOnlyClusterAgent(u)) {
      return;
    }
    List<ClusterCert> clusterCerts = clusterCertFacade.getByAgent(u);
    long diff;
    int countExpired = 0;
    for (ClusterCert clusterCert : clusterCerts) {
      Date validationKeyDate = clusterCert.getValidationKeyDate();
      if(validationKeyDate == null) {
        continue;
      }
      diff = getDateDiffHours(validationKeyDate);
      if (diff > VALIDATION_KEY_EXPIRY_DATE && clusterCert.getRegistrationStatus().equals(
          RegistrationStatusEnum.REGISTRATION_PENDING)) {
        countExpired++;
        clusterCertFacade.remove(clusterCert);
      } else if (diff > VALIDATION_KEY_EXPIRY_DATE && clusterCert.getRegistrationStatus().equals(
          RegistrationStatusEnum.UNREGISTRATION_PENDING)) {
        clusterCert.setRegistrationStatus(RegistrationStatusEnum.REGISTERED);
        clusterCert.setValidationKeyDate(null);
        clusterCertFacade.update(clusterCert);
      }
    }
    if (countExpired == clusterCerts.size()) {
      userBean.removeByEmail(u.getEmail());
    }
  }

  private boolean isOnlyClusterAgent(Users u) {
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    boolean isInClusterAgent = u.getBbcGroupCollection().contains(group);
    return u.getBbcGroupCollection().size() == 1 && isInClusterAgent;
  }

  private void isValidNewCluster(ClusterDTO cluster) {
    isValidCluster(cluster);
    if (cluster.getCommonName() == null || cluster.getCommonName().isEmpty()) {
      throw new IllegalArgumentException("Cluster Common Name not set.");
    }
    if (cluster.getCommonName().contains(Settings.DOUBLE_UNDERSCORE)) {
      throw new IllegalArgumentException("Cluster Common Name can not contain " + Settings.DOUBLE_UNDERSCORE);
    }
    if (!cluster.getChosenPassword().equals(cluster.getRepeatedPassword())) {
      throw new IllegalArgumentException("Cluster password does not match.");
    }
    if (!cluster.isTos()) {
      throw new IllegalStateException("You should agree with the terms and conditions.");
    }
  }

  private void isValidCluster(ClusterDTO cluster) {
    if (cluster == null) {
      throw new NullPointerException("Cluster not assigned.");
    }
    if (cluster.getEmail() == null || cluster.getEmail().isEmpty()) {
      throw new IllegalArgumentException("Cluster email not set.");
    }
    if (cluster.getChosenPassword() == null || cluster.getChosenPassword().isEmpty()) {
      throw new IllegalArgumentException("Cluster password not set.");
    }
    if (cluster.getOrganizationName() == null || cluster.getOrganizationName().isEmpty()) {
      throw new IllegalArgumentException("Cluster Organization Name not set.");
    }
    if (cluster.getOrganizationalUnitName() == null || cluster.getOrganizationalUnitName().isEmpty()) {
      throw new IllegalArgumentException("Cluster Organizational Unit Name not set.");
    }
  }

  private void sendEmail(ClusterDTO cluster, HttpServletRequest req, String validationKey, Users u, String type) throws
      MessagingException {
    if (type == null || type.isEmpty()) {
      throw new IllegalArgumentException("No type set.");
    }
    try {
      if (type.equals(AccountsAuditActions.REGISTRATION.name())) {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
                buildClusterRegisterRequestMessage(AuditUtil.getUserURL(req), validationKey));
      } else {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
            UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
                buildClusterUnregisterRequestMessage(AuditUtil.getUserURL(req), validationKey));
      }
    } catch (MessagingException ex) {
      LOGGER.log(Level.SEVERE, "Could not send email to ", u.getEmail());
      throw new MessagingException(ex.getMessage());
    }
  }

  private String getAgentName() {
    String sufix = "" + (userBean.lastUserID() + 1);
    int end = Settings.USERNAME_LEN - sufix.length();
    String name = CLUSTER_NAME_PREFIX.toLowerCase().substring(0, end) + (userBean.lastUserID() + 1);
    return name;
  }

  private Integer extractClusterCertId(String key) {
    if (key == null || key.isEmpty() || key.length() <= VALIDATION_KEY_LEN) {
      throw new IllegalArgumentException("Key not valid.");
    }
    int idLen = key.length() - VALIDATION_KEY_LEN;
    Integer id;
    try {
      id = Integer.parseInt(key.substring(0, idLen));
    } catch (NumberFormatException e) {
      return null;
    }
    return id;
  }

  private String extractValidationKey(String key) {
    if (key == null || key.isEmpty() || key.length() <= VALIDATION_KEY_LEN) {
      throw new IllegalArgumentException("Key too short.");
    }
    int idLen = key.length() - VALIDATION_KEY_LEN;
    return key.substring(idLen);
  }

  private long getDateDiffHours(Date start) {
    Date now = new Date();
    long diff = now.getTime() - start.getTime();
    return TimeUnit.MILLISECONDS.toHours(diff);
  }

  private void revokeCert(ClusterCert clusterCert, boolean intermediate) throws FileNotFoundException, IOException,
      InterruptedException, CertificateException {
    if (clusterCert == null || clusterCert.getSerialNumber() == null) {
      return;
    }
    String agentP = intermediate ? settings.getIntermediateCaDir() : settings.getCertsDir();
    File agentPem = new File(agentP + "/newcerts/" + clusterCert.getSerialNumber() + ".pem");
    if (!agentPem.exists()) {
      LOGGER.log(Level.WARNING, "Could not find cert to be revoked at path: {0}", agentPem.getPath());
    }
    PKIUtils.revokeCert(settings, agentPem.getPath(), intermediate);
  }

}
