/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt;

import io.hops.hopsworks.jwt.dao.JwtSigningKey;
import io.hops.hopsworks.jwt.dao.JwtSigningKeyFacade;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Singleton
public class SigningKeyHelper {
  private final static Logger LOGGER = Logger.getLogger(SigningKeyHelper.class.getName());
  
  private ConcurrentHashMap<Integer, DecodedSigningKey> signingKeys = new ConcurrentHashMap<>();
  @EJB
  private JwtSigningKeyFacade jwtSigningKeyFacade;
  
  @PostConstruct
  public void init() {
  }
  
  public void invalidateCache() {
    if (signingKeys != null && !signingKeys.isEmpty()) {
      signingKeys.clear();
    }
  }

  public DecodedSigningKey getSigningKey(Integer id, String masterPassword) {
    DecodedSigningKey decodedSigningKey = signingKeys.get(id);
    if (decodedSigningKey != null) {
      return decodedSigningKey;
    }
    JwtSigningKey jwtSigningKey = jwtSigningKeyFacade.find(id);
    decodedSigningKey = new DecodedSigningKey(jwtSigningKey, "");
    return decodedSigningKey;
  }
  
  
  
}
