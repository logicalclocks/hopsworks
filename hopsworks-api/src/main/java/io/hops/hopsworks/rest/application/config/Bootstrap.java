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
package io.hops.hopsworks.rest.application.config;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class Bootstrap extends HttpServlet {

  @Override
  public void init(ServletConfig config) throws ServletException {
    ApiKeyAuthDefinition apiKeyAuth = new ApiKeyAuthDefinition();
    apiKeyAuth.setName("Authorization");
    apiKeyAuth.setIn(In.HEADER);
    Swagger swagger = new Swagger();
    swagger.securityDefinition("Cauth-Realm", apiKeyAuth);
    swagger.addScheme(Scheme.HTTP);
    swagger.addScheme(Scheme.HTTPS);
    new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
  }
}
