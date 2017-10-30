package io.hops.hopsworks.cluster.controller;

import io.hops.hopsworks.cluster.ClusterDTO;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterController {

  private final static Logger LOGGER = Logger.getLogger(ClusterController.class.getName());
  private final static String DATE_FORMAT = "yyMMddHHmmssZ";//010704120856-0700
  private final static String CLUSTER_NAME_PREFIX = "Agent";
  private final static String CLUSTER_GROUP = "CLUSTER_AGENT";
  public final static long VALIDATION_KEY_EXPIRY_DATE = 48l;//hours to validate request
  public final static long VALIDATION_KEY_EXPIRY_DATE_MS = 48l * 36l * 100000l;//milisecond to validate request
  private final static int REG_RANDOM_KEY_LEN = 32;
  private final static int UNREG_RANDOM_KEY_LEN = 64;

  public static enum OP_TYPE {
    REGISTER,
    UNREGISTER
  }
  @EJB
  private UserFacade userBean;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;

  public void register(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidNewCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent != null) {
      throw new IllegalArgumentException("Cluster alrady registerd.");
    }
    String agentName = getAgentName();
    String validationKey = getNewKey(REG_RANDOM_KEY_LEN);
    Users agentUser = new Users();
    agentUser.setUsername(agentName);
    agentUser.setEmail(cluster.getEmail());
    agentUser.setFname(cluster.getFirstName());
    agentUser.setLname(CLUSTER_NAME_PREFIX);
    agentUser.setTitle("Mrs");
    agentUser.setStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT);
    agentUser.setMode(PeopleAccountType.M_ACCOUNT_TYPE);
    agentUser.setPassword(DigestUtils.sha256Hex(cluster.getChosenPassword()));
    agentUser.setValidationKey(validationKey);
    agentUser.setMaxNumProjects(0);

    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    Integer gid = groupFacade.lastGroupID() + 1;
    if (group == null) {
      group = new BbcGroup(gid, CLUSTER_GROUP);//do this in chef?
      group.setGroupDesc("Clusters outside the system");
      groupFacade.save(group);
    }

    List<BbcGroup> groups = new ArrayList<>();
    groups.add(group);
    agentUser.setBbcGroupCollection(groups);
    sendEmail(cluster, req, agentName + agentUser.getValidationKey(), agentUser, AccountsAuditActions.REGISTRATION.
        name());
    userBean.persist(agentUser);
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", new Object[]{agentUser.getEmail(),
      agentUser.getUsername()});

  }

  public void unregister(ClusterDTO cluster, HttpServletRequest req) throws MessagingException {
    isValidCluster(cluster);
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    if (!isOnlyClusterAgent(clusterAgent)) {
      throw new IllegalArgumentException("Not a cluster agent.");
    }
    String password = DigestUtils.sha256Hex(cluster.getChosenPassword());
    if (!password.equals(clusterAgent.getPassword())) {
      throw new SecurityException("Incorrect password.");
    }
    String validationKey = getNewKey(UNREG_RANDOM_KEY_LEN);
    clusterAgent.setValidationKey(validationKey);
    sendEmail(cluster, req, clusterAgent.getUsername() + validationKey, clusterAgent,
        AccountsAuditActions.UNREGISTRATION.name());
    userBean.update(clusterAgent);
    LOGGER.log(Level.INFO, "Unregistering cluster with email: {0}", clusterAgent.getEmail());
  }

  public void validateRequest(String key, HttpServletRequest req, OP_TYPE type) throws ParseException, IOException,
      FileNotFoundException, InterruptedException, CertificateException {
    String agentName = extractUsername(key);
    int keyLen = (type.equals(OP_TYPE.REGISTER) ? REG_RANDOM_KEY_LEN : UNREG_RANDOM_KEY_LEN);
    Date date = extractDate(key, keyLen);
    long diff = getDateDiffHours(date);
    String validationKey = extractValidationKey(key);
    Users agent = userBean.findByUsername(agentName);
    if (agent == null) {
      throw new IllegalStateException("Agent not found.");
    }
    if (!validationKey.equals(agent.getValidationKey())) {
      throw new IllegalStateException("Validation key not found.");
    }
    if (diff > VALIDATION_KEY_EXPIRY_DATE) {
      removeUserIfNotValidated(agent);
      throw new IllegalStateException("Expired valdation key.");
    }
    if (type.equals(OP_TYPE.REGISTER)) {
      agent.setValidationKey(null);
      agent.setStatus(PeopleAccountStatus.ACTIVATED_ACCOUNT);
      userBean.update(agent);
    } else {
      revokeCert(agent);
      userBean.removeByEmail(agent.getEmail());
    }
  }

  public void cleanupUnverifiedUsers() {
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    List<Integer> usersInGroup = userBean.findAllInGroup(group.getGid());
    Users u;
    for (Integer uid : usersInGroup) {
      u = userBean.find(uid);
      removeUserIfNotValidated(u);
    }
  }

  private void removeUserIfNotValidated(Users u) {
    if (u == null || u.getValidationKey() == null) {
      return;
    }
    if (u.getStatus() != PeopleAccountStatus.NEW_MOBILE_ACCOUNT) {
      return;
    }
    if (!isOnlyClusterAgent(u)) {
      return;
    }
    Date date;
    long diff;
    try {
      date = extractDate(u.getUsername() + u.getValidationKey(), REG_RANDOM_KEY_LEN);
      diff = getDateDiffHours(date);
    } catch (ParseException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return;
    }
    if (diff < VALIDATION_KEY_EXPIRY_DATE) {
      return;
    }
    
    userBean.removeByEmail(u.getEmail());
  }
  
  private boolean isOnlyClusterAgent(Users u) {
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    boolean isInClusterAgent = u.getBbcGroupCollection().contains(group);
    return u.getBbcGroupCollection().size() == 1 && isInClusterAgent;
  }

  private void isValidNewCluster(ClusterDTO cluster) {
    isValidCluster(cluster);
    if (cluster.getFirstName() == null || cluster.getFirstName().isEmpty()) {
      throw new IllegalArgumentException("Cluster name not set.");
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
      //am.registerAccountChange(u, type, AccountsAuditActions.SUCCESS.name(), "", u, req); prevents user deletion
    } catch (MessagingException ex) {
      LOGGER.log(Level.SEVERE, "Could not send email to ", u.getEmail());
      //am.registerAccountChange(u, type, AccountsAuditActions.FAILED.name(), "", u, req);
      throw new MessagingException(ex.getMessage());
    }
  }

  private String getNewKey(int len) {
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date();
    return SecurityUtils.getRandomPassword(len) + dateFormat.format(date);
  }

  private String getAgentName() {
    String sufix = "" + (userBean.lastUserID() + 1);
    int end = Settings.USERNAME_LEN - sufix.length();
    String name = CLUSTER_NAME_PREFIX.toLowerCase().substring(0, end) + (userBean.lastUserID() + 1);
    return name;
  }

  private String extractUsername(String key) {
    return key.substring(0, Settings.USERNAME_LEN);
  }

  private String extractValidationKey(String key) {
    return key.substring(Settings.USERNAME_LEN);
  }

  private Date extractDate(String key, int keyLen) throws ParseException {
    int start = Settings.USERNAME_LEN + keyLen;
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
    if (start >= key.length()) {
      throw new IllegalArgumentException("Invalid validation key.");
    }
    String date = key.substring(start);
    return format.parse(date);
  }

  private long getDateDiffHours(Date start) {
    Date now = new Date();
    long diff = now.getTime() - start.getTime();
    return TimeUnit.MILLISECONDS.toHours(diff);
  }

  private void revokeCert(Users agent) throws FileNotFoundException, IOException, InterruptedException,
      CertificateException {
    if (agent == null || agent.getEmail() == null || agent.getEmail().isEmpty()) {
      throw new IllegalArgumentException("User email required.");
    }
    File indexFile = new File(settings.getCertsDir() + "/index.txt");
    if (!indexFile.exists()) {
      throw new IllegalStateException(indexFile + " not found.");
    }
    String serialPem = getSerialNumberFromFile(indexFile, agent.getEmail());
    if (serialPem == null || serialPem.isEmpty()) {
      return; //No cert signed for agent
    }
    File agentPem = new File(settings.getCertsDir() + "/newcerts/" + serialPem);
    PKIUtils.revokeCert(agentPem.getPath(), settings.getCaDir(), settings.getHopsworksMasterPasswordSsl(), false);
  }
 
  private String getSerialNumberFromFile(File indexFile, String email) throws IOException {
//    openssl Index.txt certificate database
//    1.Certificate status flag (V=valid, R=revoked, E=expired).
//    2.Certificate expiration date in YYMMDDHHMMSSZ format.
//    3.Certificate revocation date in YYMMDDHHMMSSZ[,reason] format. Empty if not revoked.
//    4.Certificate serial number in hex.
//    5.Certificate filename or literal string ‘unknown’.
//    6.Certificate distinguished name.

    List<String> lines = Files.readAllLines(indexFile.toPath(), StandardCharsets.UTF_8);
    String[] cols;
    for (String line : lines) {
      cols = line.split("\\s+");// split by whitespace
      if (cols == null || cols.length < 5 || "R".equals(cols[0]) || "E".equals(cols[0])) {
        continue;
      }
      if (email.equals(getEmailFromLine(cols[cols.length - 1]))) {
        return cols[2] + ".pem";
      }
    }
    return null;
  }

  private boolean checkCertOwner(File pem, String email) throws FileNotFoundException, IOException,
      InterruptedException, CertificateException {
    X509Certificate cert;
    try (FileInputStream is = new FileInputStream(pem)) {
      cert = X509Certificate.getInstance(is);
    }
    return getEmailFromLine(cert.getSubjectDN().getName()).equals(email);
  }

  private String getEmailFromLine(String line) {
    String email = "emailAddress=";
    int start = line.indexOf(email);
    String tmpName, name = "";
    if (start > -1) {
      tmpName = line.substring(start + email.length());
      int end = tmpName.indexOf("/");
      if (end > 0) {
        name = tmpName.substring(0, end);
      } else {
        name = tmpName;
      }
    }
    return name;
  }

}
