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
package io.hops.hopsworks.common.dao.remote.oauth;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthToken;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Optional;

@Stateless
public class OauthTokenFacade extends AbstractFacade<OauthToken> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public OauthTokenFacade() {
    super(OauthToken.class);
  }
  
  public Optional<OauthToken> findByUser(Users user) {
    try {
      TypedQuery<OauthToken> query =
        em.createNamedQuery("OauthToken.findByUser", OauthToken.class).setParameter("user", user);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  /**
   * Update the existing token of a user if it exists else create a new token.
   * @param user
   * @param idToken
   * @param accessToken
   * @param refreshToken
   */
  public void updateOrCreate(Users user, String idToken, String accessToken, String refreshToken) {
    Optional<OauthToken> oauthTokenOptional = findByUser(user);
    OauthToken oauthToken;
    if (!oauthTokenOptional.isPresent()) {
      oauthToken = new OauthToken(user, idToken, accessToken);
      if (!Strings.isNullOrEmpty(refreshToken)) {
        oauthToken.setRefreshToken(refreshToken);
      }
      save(oauthToken);
    } else {
      oauthToken = oauthTokenOptional.get();
      oauthToken.setIdToken(idToken);
      oauthToken.setAccessToken(accessToken);
      if (!Strings.isNullOrEmpty(refreshToken)) {
        oauthToken.setRefreshToken(refreshToken);
      }
      update(oauthToken);
    }
  }
  
}
