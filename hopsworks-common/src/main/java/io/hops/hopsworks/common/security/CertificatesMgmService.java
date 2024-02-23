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
package io.hops.hopsworks.common.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;
import io.hops.hopsworks.kube.client.KubeSecretManager;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class CertificatesMgmService {
  private final Logger LOG = Logger.getLogger(CertificatesMgmService.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private UserFacade userFacade;
  @EJB
  private MessageController messageController;
  @Inject
  @Any
  private Instance<MasterPasswordHandler> handlers;
  @Inject
  private HazelcastInstance hazelcastInstance;
  @EJB
  private CertificateMasterPwdMgm certificateMasterPwdMgm;
  @EJB
  private KubeSecretManager kubeSecretManager;
  
  public enum UPDATE_STATUS {
    OK,
    WORKING,
    FAILED,
    NOT_FOUND
  }
  private final Map<Class, MasterPasswordChangeResult> handlersResult = new HashMap<>();
  // should be shared
  private Cache<Integer, UPDATE_STATUS> updateStatus;
  private Random rand;

  public CertificatesMgmService() {
  
  }
  
  private static final String MAP_NAME = "certificatesMgmUpdateStatus";
  
  private void initHazelcast() {
    if (hazelcastInstance != null) {
      if (hazelcastInstance.getConfig().getMapConfigOrNull(MAP_NAME) == null) {
        MapConfig mapConfig = new MapConfig(MAP_NAME);
        mapConfig.setMaxIdleSeconds((int) TimeUnit.HOURS.toSeconds(12L));
        hazelcastInstance.getConfig().addMapConfig(mapConfig);
      }
    } else {
      updateStatus = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build();
    }
  }
  
  private UPDATE_STATUS getIfPresentUpdateStatusCache(Integer key) {
    if (hazelcastInstance != null) {
      IMap<Integer, UPDATE_STATUS> map = hazelcastInstance.getMap(MAP_NAME);
      return map.get(key);
    } else {
      return updateStatus.getIfPresent(key);
    }
  }
  
  private void putUpdateStatusCache(Integer key, UPDATE_STATUS val) {
    if (hazelcastInstance != null) {
      IMap<Integer, UPDATE_STATUS> map = hazelcastInstance.getMap(MAP_NAME);
      map.put(key, val);
    } else {
      updateStatus.put(key, val);
    }
  }
  
  @PostConstruct
  public void init() {
    initHazelcast();
    rand = new Random();
  }

  @Lock(LockType.READ)
  public String getMasterEncryptionPassword() {
    return certificateMasterPwdMgm.getMasterEncryptionPassword(kubeSecretManager);
  }
  
  /**
   * Validates the provided password against the configured one
   * @param providedPassword Password to validate
   * @param userRequestedEmail User requested the password check
   * @throws IOException
   * @throws EncryptionMasterPasswordException
   */
  @Lock(LockType.READ)
  public void checkPassword(String providedPassword, String userRequestedEmail)
      throws EncryptionMasterPasswordException {
    certificateMasterPwdMgm.checkPassword(providedPassword, userRequestedEmail, kubeSecretManager, userFacade);
  }
  
  // A put operation with the same operationId can only be initiated from the same node that created the operationId.
  // Because initUpdateOperation is called before resetMasterEncryptionPassword which will update the state.
  // So locking this locally in this singleton should be enough.
  @Lock(LockType.WRITE)
  public Integer initUpdateOperation() {
    Integer operationId = rand.nextInt();
    putUpdateStatusCache(operationId, UPDATE_STATUS.WORKING);
    return operationId;
  }
  
  @Lock(LockType.READ)
  public UPDATE_STATUS getOperationStatus(Integer operationId) {
    UPDATE_STATUS status = getIfPresentUpdateStatusCache(operationId);
    return status != null ? status : UPDATE_STATUS.NOT_FOUND;
  }
  
  /**
   * Decrypt secrets using the old master password and encrypt them with the new
   * Both for project specific and project generic certificates
   * @param newMasterPasswd new master encryption password
   * @param userRequested User requested password change
   */
  @Asynchronous
  @Lock(LockType.WRITE)
  public void resetMasterEncryptionPassword(Integer operationId, String newMasterPasswd, String userRequested)
    throws ExecutionException, InterruptedException {
    Future<MasterPasswordResetResult> futureResetResult =
      certificateMasterPwdMgm.resetMasterEncryptionPassword(newMasterPasswd, kubeSecretManager, handlers,
        handlersResult);
    MasterPasswordResetResult resetResult;
    try {
      resetResult = futureResetResult.get();
    } catch (InterruptedException | ExecutionException e) {
      sendUnsuccessfulMessage(e.getMessage(), userRequested);
      putUpdateStatusCache(operationId, UPDATE_STATUS.FAILED);
      throw e;
    }
    if (UPDATE_STATUS.OK.equals(resetResult.getUpdateStatus())) {
      sendSuccessfulMessage(resetResult.getSuccessLog(), userRequested);
      putUpdateStatusCache(operationId, UPDATE_STATUS.OK);
    } else {
      sendUnsuccessfulMessage(resetResult.getErrorLog(), userRequested);
      putUpdateStatusCache(operationId, UPDATE_STATUS.FAILED);
    }
  }

  private void sendSuccessfulMessage(String successLog, String userRequested) {
    sendInbox(successLog, "Changed successfully", userRequested);
  }
  
  private void sendUnsuccessfulMessage(String message, String userRequested) {
    sendInbox(message, "Change failed!", userRequested);
  }
  
  private void sendInbox(String message, String preview, String userRequested) {
    Users to = userFacade.findByEmail(userRequested);
    Users from = userFacade.findByEmail(settings.getAdminEmail());
    messageController.send(to, from, "Master encryption password changed", preview, message, "");
  }
}
