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
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import java.util.ArrayList;
import java.util.List;

public class Bootstrap extends HttpServlet {

  @Override
  public void init(ServletConfig config) {
    ApiKeyAuthDefinition apiKeyAuth = new ApiKeyAuthDefinition();
    apiKeyAuth.setName("Authorization");
    apiKeyAuth.setIn(In.HEADER);
    apiKeyAuth.setDescription("Authorization Token JWT or ApiKey. \n To use jwt add the token with prefix Bearer and " +
        "for ApiKey add prefix ApiKey. Not all end points accept apikey.");
    Swagger swagger = new Swagger();
    swagger.securityDefinition("Authorization", apiKeyAuth);
    List<SecurityRequirement> securityRequirements = new ArrayList<>();
    SecurityRequirement securityRequirement = new SecurityRequirement();
    securityRequirements.add(securityRequirement.requirement("Authorization"));
    swagger.setSecurity(securityRequirements);
    swagger.addScheme(Scheme.HTTPS);
    swagger.addScheme(Scheme.HTTP);
    new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
  }
}
