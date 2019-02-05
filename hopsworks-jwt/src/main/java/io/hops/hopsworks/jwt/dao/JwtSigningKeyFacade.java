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
package io.hops.hopsworks.jwt.dao;

import io.hops.hopsworks.jwt.SignatureAlgorithm;
import io.hops.hopsworks.jwt.SigningKeyGenerator;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class JwtSigningKeyFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @EJB
  private SigningKeyGenerator signingKeyGenerator;

  public JwtSigningKey find(Integer id) {
    return em.find(JwtSigningKey.class, id);
  }

  public List<JwtSigningKey> findAll() {
    TypedQuery<JwtSigningKey> query = em.createNamedQuery("JwtSigningKey.findAll", JwtSigningKey.class);
    return query.getResultList();
  }

  public JwtSigningKey findByName(String keyName) {
    TypedQuery<JwtSigningKey> query = em.createNamedQuery("JwtSigningKey.findByName", JwtSigningKey.class).
        setParameter("name", keyName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  public void renameSigningKey (String keyName, String newName) {
    JwtSigningKey signingKey = findByName(keyName);
    renameSigningKey(signingKey, newName);
  }
  
  public void renameSigningKey (JwtSigningKey signingKey, String newName) {
    signingKey.setName(newName);
    em.merge(signingKey);
  }
  
  public JwtSigningKey getOrCreateSigningKey(String keyName, SignatureAlgorithm alg) throws NoSuchAlgorithmException {
    JwtSigningKey signingKey = this.findByName(keyName);
    if (signingKey == null) {
      signingKey = this.createSigningKey(keyName, alg);
    }
    return signingKey;
  }
  
  public JwtSigningKey createNewSigningKey(String keyName, SignatureAlgorithm alg) throws NoSuchAlgorithmException, 
      DuplicateSigningKeyException {
    JwtSigningKey signingKey = this.findByName(keyName);
    if (signingKey != null) {
      // throwing DuplicateSigningKeyException to catch parent exception (JWTException) and
      throw new DuplicateSigningKeyException("A signing key with the same name already exists.");
    }
    return this.createSigningKey(keyName, alg);
  }
  
  private JwtSigningKey createSigningKey(String keyName, SignatureAlgorithm alg) throws NoSuchAlgorithmException {
    JwtSigningKey signingKey;
    String base64Encoded = signingKeyGenerator.getSigningKey(alg.getJcaName());
    signingKey = new JwtSigningKey(base64Encoded, keyName);
    persist(signingKey);
    JwtSigningKey newSigningKey = findByName(keyName);
    return newSigningKey;
  }

  public void persist(JwtSigningKey invalidJwt) {
    em.persist(invalidJwt);
  }

  public JwtSigningKey merge(JwtSigningKey invalidJwt) {
    return em.merge(invalidJwt);
  }

  public void remove(JwtSigningKey jwtSigningKey) {
    JwtSigningKey jsk = find(jwtSigningKey.getId());
    if (jsk == null) {
      return;
    }
    em.remove(jsk);
  }

  public void remove(String name) {
    JwtSigningKey jsk = findByName(name);
    if (jsk == null) {
      return;
    }
    em.remove(jsk);
  }
}
