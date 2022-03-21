/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.ldap;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.persistence.entity.util.VariablesVisibility;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LdapConfigBuilder {
  @EJB
  private Settings settings;
  
  public LdapConfigDTO uri(LdapConfigDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  public LdapConfigDTO build(UriInfo uriInfo) {
    LdapConfigDTO dto = new LdapConfigDTO(settings);
    uri(dto, uriInfo);
    return dto;
  }
  
  public LdapConfigDTO update(LdapConfigDTO ldapConfigDTO, DefaultConfig defaultConfig, UriInfo uriInfo) {
    List<Variables> variablesList;
    if (defaultConfig != null) {
      variablesList = ldapConfigDTO.variables(settings, defaultConfig).entrySet().stream()
        .map(v -> new Variables(v.getKey(), v.getValue(), VariablesVisibility.ADMIN))
        .collect(Collectors.toList());
    } else {
      variablesList = ldapConfigDTO.variables(settings).entrySet().stream()
        .map(v -> new Variables(v.getKey(), v.getValue(), VariablesVisibility.ADMIN))
        .collect(Collectors.toList());
    }
    settings.updateVariables(variablesList);
    LdapConfigDTO dto = new LdapConfigDTO(settings);
    uri(dto, uriInfo);
    return dto;
  }
}
