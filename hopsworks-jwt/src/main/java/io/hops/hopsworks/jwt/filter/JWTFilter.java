/*
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
 */
package io.hops.hopsworks.jwt.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import static io.hops.hopsworks.jwt.Constants.BEARER;
import static io.hops.hopsworks.jwt.Constants.DEFAULT_EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.EXPIRY_LEEWAY;
import static io.hops.hopsworks.jwt.Constants.ROLES;
import static io.hops.hopsworks.jwt.Constants.WWW_AUTHENTICATE_VALUE;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;

public abstract class JWTFilter implements ContainerRequestFilter {

  private final static Logger LOGGER = Logger.getLogger(JWTFilter.class.getName());

  @Override
  public final void filter(ContainerRequestContext requestContext) throws IOException {
    preJWTFilter(requestContext);
    jwtFilter(requestContext);
  }

  public void jwtFilter(ContainerRequestContext requestContext) throws IOException {

    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null) {
      LOGGER.log(Level.INFO, "Token not provided.");
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity("Token not provided.").build());
      return;
    }
    if (!authorizationHeader.startsWith(BEARER)) {
      LOGGER.log(Level.INFO, "Invalid token. AuthorizationHeader : {0}", authorizationHeader);
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity("Invalid token.").build());
      return;
    }

    String token = authorizationHeader.substring(BEARER.length()).trim();
    DecodedJWT jwt = JWT.decode(token);
    Claim expLeewayClaim = jwt.getClaim(EXPIRY_LEEWAY);
    String issuer = getIssuer();
    int expLeeway = expLeewayClaim.asInt();
    try {
      Algorithm algorithm = getAlgorithm(jwt);
      JWTVerifier verifier = JWT.require(algorithm)
          .withIssuer(issuer == null || issuer.isEmpty() ? jwt.getIssuer() : issuer)
          .acceptExpiresAt(expLeeway == 0 ? DEFAULT_EXPIRY_LEEWAY : expLeeway)
          .build();
      jwt = verifier.verify(token);
    } catch (JWTVerificationException | SigningKeyNotFoundException exception) {
      LOGGER.log(Level.INFO, "JWT Verification Exception: {0}", exception.getMessage());
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity(exception.getMessage()).build());
      return;
    }

    if (!isTokenValid(jwt)) {
      LOGGER.log(Level.INFO, "JWT Verification Exception: Invalidated token.");
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity("Invalidated token.").build());
      return;
    }

    Claim rolesClaim = jwt.getClaim(ROLES);
    String[] userRoles = rolesClaim == null ? new String[0] : rolesClaim.asArray(String.class);
    Set<String> allowedRolesSet = allowedRoles();
    if (allowedRolesSet != null && !allowedRolesSet.isEmpty()) {
      if (!intersect(allowedRolesSet, Arrays.asList(userRoles))) {
        LOGGER.log(Level.INFO, "JWT Access Exception: Client not authorized for this invocation.");
        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity(
            "Client not authorized for this invocation.").build());
        return;
      }
    }

    List<String> audience = jwt.getAudience();
    Set<String> accepts = acceptedTokens();
    if (accepts != null && !accepts.isEmpty()) {
      if (!intersect(accepts, audience)) {
        LOGGER.log(Level.INFO, "JWT Access Exception: Token not issued for this recipient.");
        requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity(
            "Token not issued for this recipient.").build());
        return;
      }
    }

    postJWTFilter(requestContext, jwt);
  }

  private boolean intersect(Collection list1, Collection list2) {
    if (list1 == null || list1.isEmpty() || list2 == null || list2.isEmpty()) {
      return false;
    }
    Set<String> set1 = new HashSet<>(list1);
    Set<String> set2 = new HashSet<>(list2);
    set1.retainAll(set2);
    return !set1.isEmpty();
  }

  public abstract Algorithm getAlgorithm(DecodedJWT jwt) throws SigningKeyNotFoundException;

  public abstract Set<String> allowedRoles();

  public abstract Set<String> acceptedTokens();

  public abstract boolean isTokenValid(DecodedJWT jwt);

  public abstract void preJWTFilter(ContainerRequestContext requestContext) throws IOException;

  public abstract void postJWTFilter(ContainerRequestContext requestContext, DecodedJWT jwt) throws IOException;

  public abstract String getIssuer();
}
