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
package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.filter.JWTRenewFilter;
import java.util.Date;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.ext.Provider;

@Provider
@JWTRequired
public class JWTAutoRenewFilter extends JWTRenewFilter{

  private final static Logger LOGGER = Logger.getLogger(JWTAutoRenewFilter.class.getName());
  @EJB
  private JWTController jwtController;
  @EJB
  private Settings settings;

  @Override
  public String renewToken(String token) throws JWTException {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + settings.getJWTLifetimeMs());
    return jwtController.autoRenewToken(token, expiresAt, now);
  }

}
