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
