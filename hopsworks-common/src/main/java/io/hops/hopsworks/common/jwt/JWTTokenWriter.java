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

package io.hops.hopsworks.common.jwt;

import io.hops.hopsworks.common.jobs.ExecutionJWT;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.Set;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public interface JWTTokenWriter {
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  void writeToken(ServiceJWT serviceJWT);
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  void writeToken(ExecutionJWT executionJWT);
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  void deleteToken(ServiceJWT serviceJWT);
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  void deleteToken(ExecutionJWT executionJWT);
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  Set<ExecutionJWT> getJWTs(Map<String, String> labels) throws VerificationException, SigningKeyNotFoundException;
  
}
