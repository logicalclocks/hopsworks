/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.opensearch;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.util.OpenSearchSettings;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchJWTController {
  @EJB
  private JWTController jwtController;
  @EJB
  private Settings settings;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  
  public String getSigningKeyForELK() throws OpenSearchException {
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    try {
      return jwtController.getSigningKeyForELK(alg);
    } catch (NoSuchAlgorithmException e) {
      throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.SIGNING_KEY_ERROR,
          Level.SEVERE, "Failed to get elk signing key", e.getMessage(),
          e);
    }
  }
  
  public String createTokenForELK(Users user, Project project)
      throws OpenSearchException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    return createTokenForELK(project, userRole);
  }
  
  public String createTokenForELKAsDataOwner(Project project)
      throws OpenSearchException {
    return createTokenForELK(project,
        ProjectRoleTypes.DATA_OWNER.getRole());
  }
  
  public String createTokenForELKAsAdmin() throws OpenSearchException {
    return createTokenForELK(settings.getOpenSearchAdminUser(), Optional.empty(),
      OpenSearchSettings.OPENSEARCH_ADMIN_ROLE);
  }

  /**
   * Create a JWT token that can access the service index-pattern in Kibana
   * The service index-pattern in Kibana gives access to the logs of the
   * services
   * @return the JWT token
   * @throws OpenSearchException
   */
  public String createTokenForELKServices() throws OpenSearchException {
    return createTokenForELK(
        settings.getOpenSearchServiceLogUser(), Optional.empty(), OpenSearchSettings.OPENSEARCH_SERVICE_LOG_ROLE);
  }
  
  private String createTokenForELK(Project project, String role)
      throws OpenSearchException {
    String userRole = OpenSearchUtils.getValidRole(role);
    return createTokenForELK(project.getName(), Optional.of(project.getInode().getId()), userRole);
  }
  
  private String createTokenForELK(String project, Optional<Long> projectInodeId, String userRole)
      throws OpenSearchException {
    SignatureAlgorithm alg = SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg());
    Date expiresAt =
        new Date(System.currentTimeMillis() + settings.getOpenSearchJwtExpMs());
    try {
      Map<String, Object> claims = new HashMap<>();
      claims.put(Constants.ROLES, userRole);
      claims.put(Constants.ELK_VALID_PROJECT_NAME,
          OpenSearchUtils.getProjectNameWithNoSpecialCharacters(project));
      if(projectInodeId.isPresent()) {
        claims.put(Constants.ELK_PROJECT_INODE_ID, projectInodeId.get());
      }
      return jwtController.createTokenForELK(project, settings.getJWTIssuer()
          , claims, expiresAt, alg);
    } catch (DuplicateSigningKeyException | NoSuchAlgorithmException | SigningKeyNotFoundException e) {
      throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.JWT_NOT_CREATED,
          Level.SEVERE, "Failed to create jwt token for elk", e.getMessage(), e);
    }
  }
}
