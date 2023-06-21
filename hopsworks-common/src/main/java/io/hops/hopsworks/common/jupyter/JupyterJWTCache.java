/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.jupyter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterJWTCache {
  private static final String MAP_NAME = "jupyterJWTMap";
  @Inject
  private HazelcastInstance hazelcastInstance;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  
  private final TreeSet<JupyterJWTDTO> jupyterJWTs = new TreeSet<>((t0, t1) -> {
    if (t0.equals(t1)) {
      return 0;
    } else {
      if (t0.getExpiration().isBefore(t1.getExpiration())) {
        return -1;
      } else if (t0.getExpiration().isAfter(t1.getExpiration())) {
        return 1;
      }
      return 0;
    }
  });
  
  private final HashMap<CidAndPort, JupyterJWT> pidAndPortToJWT = new HashMap<>();
  
  public void add(JupyterJWT jupyterJWT) {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      pidAndPortToJWTMap.put(jupyterJWT.pidAndPort, new JupyterJWTDTO(jupyterJWT));
    } else {
      jupyterJWTs.add(new JupyterJWTDTO(jupyterJWT));
      pidAndPortToJWT.put(jupyterJWT.pidAndPort, jupyterJWT);
    }
  }
  
  public Optional<JupyterJWT> get(CidAndPort pidAndPort) {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      JupyterJWTDTO jupyterJWTDTO = pidAndPortToJWTMap.get(pidAndPort);
      if (jupyterJWTDTO != null) {
        Project project = projectFacade.find(jupyterJWTDTO.getProjectId());
        Users user = userFacade.find(jupyterJWTDTO.getUserId());
        return Optional.of(
          new JupyterJWT(project, user, jupyterJWTDTO.getExpiration(), pidAndPort, jupyterJWTDTO.getToken(),
            Paths.get(jupyterJWTDTO.getTokenFile())));
      }
      return Optional.empty();
    } else {
      return Optional.ofNullable(pidAndPortToJWT.get(pidAndPort));
    }
  }
  
  public void remove(CidAndPort pidAndPort) {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      pidAndPortToJWTMap.remove(pidAndPort);
    } else {
      JupyterJWT jupyterJWT = pidAndPortToJWT.remove(pidAndPort);
      jupyterJWTs.remove(new JupyterJWTDTO(jupyterJWT));
    }
  }
  
  public void replaceAll(Set<JupyterJWT> renewedJWTs) {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      renewedJWTs.forEach(t -> pidAndPortToJWTMap.replace(t.pidAndPort, new JupyterJWTDTO(t)));
    } else {
      renewedJWTs.forEach(t -> {
        //remove old token
        JupyterJWT jupyterJWT = pidAndPortToJWT.remove(t.pidAndPort);
        jupyterJWTs.remove(new JupyterJWTDTO(jupyterJWT));
        //Add the new token
        jupyterJWTs.add(new JupyterJWTDTO(t));
        pidAndPortToJWT.put(t.pidAndPort, t);
      });
    }
  }
  
  public int getSize() {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      return pidAndPortToJWTMap.size();
    } else {
      return jupyterJWTs.size();
    }
  }
  
  public Iterator<JupyterJWTDTO> getMaybeExpired() {
    if (hazelcastInstance != null) {
      IMap<CidAndPort, JupyterJWTDTO> pidAndPortToJWTMap = hazelcastInstance.getMap(MAP_NAME);
      Predicate<CidAndPort, JupyterJWTDTO> expirationPredicate = Predicates.lessEqual("expiration", DateUtils.getNow());
      Collection<JupyterJWTDTO> jupyterJWTDTOS = pidAndPortToJWTMap.values(expirationPredicate);
      return jupyterJWTDTOS.iterator();
    } else {
      return jupyterJWTs.iterator();
    }
  }
}
