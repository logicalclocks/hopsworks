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

package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SecretsBuilder {
  
  public SecretDTO build(List<SecretPlaintext> secrets, boolean withSecret) {
    SecretDTO dto = new SecretDTO();
    secrets.stream()
        .map(s -> buildDTO(s, withSecret))
        .forEach(k -> dto.addItem(k));
    return dto;
  }
  
  private SecretDTO buildDTO(SecretPlaintext secret, boolean withSecret) {
    SecretDTO dto = new SecretDTO(secret.getKeyName(), secret.getAddedOn());
    if (withSecret) {
      dto.setSecret(secret.getPlaintext());
    }
    dto.setVisibility(secret.getVisibilityType());
    return dto;
  }
}
