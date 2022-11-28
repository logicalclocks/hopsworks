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

package com.predic8.membrane.servlet.embedded;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.interceptor.HTTPClientInterceptor;
import com.predic8.membrane.core.transport.http.client.HttpClientConfiguration;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HopsHTTPClientInterceptor extends HTTPClientInterceptor {
  
  private static final Logger LOGGER = Logger.getLogger(HopsHTTPClientInterceptor.class.getName());
  
  @Override
  public void init(Router router) throws Exception {
    ManagedExecutorService managedExecutorService;
    try {
      managedExecutorService = InitialContext.doLookup("concurrent/jupyterExecutorService");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error looking up for the jupyterExecutorService", e);
      throw e;
    }
    super.init(router, new HopsHttpClient(new HttpClientConfiguration(), managedExecutorService));
  }
}
