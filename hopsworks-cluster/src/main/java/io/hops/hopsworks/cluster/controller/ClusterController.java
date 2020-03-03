/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.cluster.controller;

import io.hops.hopsworks.cluster.ClusterDTO;
import io.hops.hopsworks.cluster.ClusterYmlDTO;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCertFacade;
import io.hops.hopsworks.common.dao.user.cluster.RegistrationStatusEnum;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.FormatUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterController {

  private static final Logger LOGGER = Logger.getLogger(ClusterController.class.getName());
  private static final String CLUSTER_NAME_PREFIX = "Agent";
  private static final String CLUSTER_GROUP = "CLUSTER_AGENT";
  public static final long VALIDATION_KEY_EXPIRY_DATE = 48l;//hours to validate request
  static final long VALIDATION_KEY_EXPIRY_DATE_MS = 48l * 36l * 100000l;//millisecond to validate request
  private static final int VALIDATION_KEY_LEN = 64;

  public enum OP_TYPE {

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
  private AuthController authController;
  @EJB
  private UsersController usersCtrl;
  @EJB
  private CertificatesController certificatesController;
  @EJB
  private SecurityUtils securityUtils;

  public void registerClusterNewUser(ClusterDTO cluster, HttpServletRequest req, boolean autoValidate)
    throws MessagingException, UserException {
    isValidNewCluster(cluster);
    Users clusterAgent = createClusterAgent(cluster, req);
    ClusterCert clusterCert = createClusterCert(cluster, clusterAgent);

    if (autoValidate) {
      activateClusterAgent(cluster);
      autoActivateCluster(clusterCert);
    } else {
      sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent,
        "REGISTRATION");
    }
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", new Object[]{clusterAgent.getEmail(),
      clusterAgent.getUsername()});
  }

  public void registerClusterWithUser(ClusterDTO cluster, HttpServletRequest req, boolean autoValidate)
    throws MessagingException, UserException {
    isValidCluster(cluster);
    Optional<Users> clusterAgent = verifyClusterAgent(cluster, req);
    if (!clusterAgent.isPresent()) {
      throw new IllegalArgumentException("User not registerd.");
    }
    ClusterCert clusterCert = createClusterCert(cluster, clusterAgent.get());
    if (autoValidate) {
      autoActivateCluster(clusterCert);
    } else {
      sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent.get(),
        "REGISTRATION");
    }
    LOGGER.log(Level.INFO, "New cluster added with email: {0}, and username: {1}", 
      new Object[]{clusterAgent.get().getEmail(), clusterAgent.get().getUsername()});
  }

  private Optional<Users> verifyClusterAgent(ClusterDTO cluster, HttpServletRequest req) throws UserException {
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      return Optional.empty();
    }
    checkUserPasswordAndStatus(cluster, clusterAgent, req);
    return Optional.of(clusterAgent);
  }

  private Users createClusterAgent(ClusterDTO cluster, HttpServletRequest req) throws UserException {
    Optional<Users> clusterAgentAux = verifyClusterAgent(cluster, req);
    if (clusterAgentAux.isPresent()) {
      return clusterAgentAux.get();
    }
    Users clusterAgent = usersCtrl.createNewAgent(cluster.getEmail(), CLUSTER_NAME_PREFIX, "007",
      cluster.getChosenPassword(), "Mrs");
    clusterAgent.setBbcGroupCollection(bbcGroups());
    userBean.persist(clusterAgent);
    return clusterAgent;
  }

  private void activateClusterAgent(ClusterDTO cluster) {
    Users clusterAgent = userBean.findByEmail(cluster.getEmail());
    if (clusterAgent == null) {
      throw new IllegalArgumentException("Cluster not registerd.");
    }
    if (clusterAgent.getStatus().equals(UserAccountStatus.ACTIVATED_ACCOUNT)) {
      return;
    }
    if (clusterAgent.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      clusterAgent.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);
      userBean.update(clusterAgent);
      return;
    }
    throw new IllegalStateException("Trying to activate account in state:" + clusterAgent.getStatus());
  }

  private List<BbcGroup> bbcGroups() {
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    Integer gid = groupFacade.lastGroupID() + 1;
    if (group == null) {
      group = new BbcGroup(gid, CLUSTER_GROUP);//do this in chef?
      group.setGroupDesc("Clusters outside the system");
      groupFacade.save(group);
    }
    List<BbcGroup> groups = new ArrayList<>();
    groups.add(group);
    return groups;
  }

  private ClusterCert createClusterCert(ClusterDTO cluster, Users clusterAgent) {
    ClusterCert clusterCert = clusterCertFacade.getByOrgUnitNameAndOrgName(cluster.getOrganizationName(), cluster.
      getOrganizationalUnitName());
    if (clusterCert != null) {
      throw new IllegalArgumentException(
        "Cluster with the same Organization and Organization unit name already registered.");
    }
    String commonName = cluster.getOrganizationName() + "_" + cluster.getOrganizationalUnitName();
    clusterCert = new ClusterCert(commonName, cluster.getOrganizationName(), cluster.getOrganizationalUnitName(),
      RegistrationStatusEnum.REGISTRATION_PENDING, clusterAgent);
    clusterCert.setValidationKey(securityUtils.generateSecureRandomString(VALIDATION_KEY_LEN));
    clusterCert.setValidationKeyDate(new Date());
    clusterCertFacade.save(clusterCert);
    return clusterCert;
  }

  private void autoActivateCluster(ClusterCert clusterCert) {
    clusterCert.setRegistrationStatus(RegistrationStatusEnum.REGISTERED);
    clusterCert.setValidationKey(null);
    clusterCert.setValidationKeyDate(null);
    clusterCertFacade.update(clusterCert);
  }

  public void unregister(ClusterDTO cluster, HttpServletRequest req) throws MessagingException, UserException {
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

    clusterCert.setValidationKey(securityUtils.generateSecureRandomString(VALIDATION_KEY_LEN));
    clusterCert.setRegistrationStatus(RegistrationStatusEnum.UNREGISTRATION_PENDING);
    clusterCert.setValidationKeyDate(new Date());
    clusterCertFacade.update(clusterCert);
    sendEmail(cluster, req, clusterCert.getId() + clusterCert.getValidationKey(), clusterAgent,
      "UNREGISTRATION");
    LOGGER.log(Level.INFO, "Unregistering cluster with email: {0}", clusterAgent.getEmail());
  }

  public void validateRequest(String key, HttpServletRequest req, OP_TYPE type)
      throws HopsSecurityException, GenericException {

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
      if (agent.getStatus() == UserAccountStatus.NEW_MOBILE_ACCOUNT) {
        agent.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);
        userBean.update(agent);
      }
      clusterCert.setValidationKey(null);
      clusterCert.setValidationKeyDate(null);
      clusterCert.setRegistrationStatus(RegistrationStatusEnum.REGISTERED);
      clusterCertFacade.update(clusterCert);
    } else if (clusterCert.getRegistrationStatus().equals(RegistrationStatusEnum.UNREGISTRATION_PENDING)) {
      revokeCert(clusterCert);
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

  public List<ClusterCert> getAllClusters(ClusterDTO cluster, HttpServletRequest req) throws UserException {
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

  public List<ClusterYmlDTO> getAllClusterYml(ClusterDTO cluster, HttpServletRequest req) throws UserException {
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
    List<ClusterCert> clusterCerts = clusterCertFacade.getByAgent(clusterAgent);
    List<ClusterYmlDTO> clusterYmlDTOs = new ArrayList<>();
    for (ClusterCert cCert : clusterCerts) {
      clusterYmlDTOs.add(new ClusterYmlDTO(cCert.getAgentId().getEmail(),
        cCert.getCommonName(),
        cCert.getOrganizationName(),
        cCert.getOrganizationalUnitName(),
        cCert.getRegistrationStatus(),
        cCert.getRegistrationDate(),
        cCert.getSerialNumber()));
    }
    return clusterYmlDTOs;
  }

  public ClusterCert getCluster(ClusterDTO cluster, HttpServletRequest req) throws UserException {

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

  private void checkUserPasswordAndStatus(ClusterDTO cluster, Users clusterAgent, HttpServletRequest req)
    throws UserException {
    authController.checkPasswordAndStatus(clusterAgent, cluster.getChosenPassword());
    BbcGroup group = groupFacade.findByGroupName(CLUSTER_GROUP);
    if (!clusterAgent.getBbcGroupCollection().contains(group)) {
      throw new SecurityException("User not allowed to register clusters.");
    }
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
      if (validationKeyDate == null) {
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
      if (type.equals("REGISTRATION")) {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
          buildClusterRegisterRequestMessage(FormatUtils.getUserURL(req), validationKey));
      } else {
        emailBean.sendEmail(cluster.getEmail(), Message.RecipientType.TO,
          UserAccountsEmailMessages.CLUSTER_REQUEST_SUBJECT, UserAccountsEmailMessages.
          buildClusterUnregisterRequestMessage(FormatUtils.getUserURL(req), validationKey));
      }
    } catch (MessagingException ex) {
      LOGGER.log(Level.SEVERE, "Could not send email to ", u.getEmail());
      throw new MessagingException(ex.getMessage());
    }
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

  private void revokeCert(ClusterCert clusterCert) throws HopsSecurityException, GenericException {
    if (clusterCert == null || clusterCert.getSerialNumber() == null) {
      return;
    }

    certificatesController.revokeDelaClusterCertificate(clusterCert.getCommonName());
  }
}
