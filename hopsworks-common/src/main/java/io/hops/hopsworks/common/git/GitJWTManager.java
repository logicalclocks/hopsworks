/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.git;

import io.hops.hopsworks.api.auth.UserUtilities;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.jwt.Constants;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;


import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitJWTManager {
  @EJB
  private JWTController jwtController;
  @EJB
  private Settings settings;
  @EJB
  private UserUtilities userUtilities;

  private final String TOKEN_FILE_NAME = "token.jwt";

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void materializeJWT(Users user, String tokenPath) throws GitOpException {
    LocalDateTime expirationDate = LocalDateTime.now().plus(settings.getGitJwtExpMs(), ChronoUnit.MILLIS);
    String token = createTokenForGitContainer(user, expirationDate);
    try {
      FileUtils.writeStringToFile(getTokenFullPath(tokenPath).toFile(), token);
    } catch (IOException e) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.JWT_MATERIALIZATION_ERROR, Level.SEVERE, "Failed to " +
          "materialize jwt", e.getMessage(), e);
    }
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String createGitContainerJWT(Users user) throws GitOpException {
    LocalDateTime expirationDate = LocalDateTime.now().plus(settings.getGitJwtExpMs(), ChronoUnit.MILLIS);
    return createTokenForGitContainer(user, expirationDate);
  }

  private String createTokenForGitContainer(Users user, LocalDateTime expirationDate)
      throws GitOpException {
    String[] userRoles = userUtilities.getUserRoles(user).toArray(new String[1]);
    return createTokenForGitContainer(user.getUsername(), userRoles, expirationDate);
  }

  private String createTokenForGitContainer(String username, String[] userRoles,
                                            LocalDateTime expirationDate) throws GitOpException {
    try {
      Map<String, Object> claims = new HashMap<>();
      claims.put(Constants.ROLES, userRoles);
      claims.put(Constants.RENEWABLE, false);
      return jwtController.createToken(settings.getJWTSigningKeyName(), false, settings.getJWTIssuer(),
          new String[]{"api", "git"}, DateUtils.localDateTime2Date(expirationDate),
          DateUtils.localDateTime2Date(DateUtils.getNow()), username, claims,
          SignatureAlgorithm.valueOf(settings.getJWTSignatureAlg()));
    } catch (DuplicateSigningKeyException | NoSuchAlgorithmException | SigningKeyNotFoundException e) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.JWT_NOT_CREATED, Level.SEVERE, "Failed to create jwt token " +
          "for git", e.getMessage(), e);
    }
  }

  public Path getTokenFullPath(String tokenDir) {
    return Paths.get(tokenDir, TOKEN_FILE_NAME);
  }
}
