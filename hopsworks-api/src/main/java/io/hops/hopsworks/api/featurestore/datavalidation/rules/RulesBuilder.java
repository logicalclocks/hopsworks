/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidation.rules;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.RuleDTO;
import io.hops.hopsworks.common.featurestore.datavalidation.RuleFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.Name;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.ValidationRule;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RulesBuilder {

  @EJB
  private RuleFacade ruleDefinitionFacade;

  public RuleDTO uri(RuleDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }

  public RuleDTO uri(RuleDTO dto, UriInfo uriInfo, ValidationRule validationRule) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.RULES.toString())
        .path(validationRule.getName().toString())
        .build());
    return dto;
  }

  public RuleDTO expand(RuleDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.RULES)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public RuleDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Name name) throws FeaturestoreException {
    return build(uriInfo, resourceRequest,
            ruleDefinitionFacade.findByName(name).orElseThrow(() ->
                    new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.RULE_NOT_FOUND,
                            Level.FINE, "Name: " + name)));
  }

  public RuleDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, ValidationRule validationRule) {
    RuleDTO dto = new RuleDTO();
    uri(dto, uriInfo, validationRule);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setName(validationRule.getName());
      dto.setDescription(validationRule.getDescription());
      dto.setPredicate(validationRule.getPredicate());
      dto.setAcceptedType(validationRule.getAcceptedType());
    }
    return dto;
  }

  public RuleDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    RuleDTO dto = new RuleDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if(dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = ruleDefinitionFacade.findAll(resourceRequest.getOffset(),
          resourceRequest.getLimit(),
          resourceRequest.getFilter(),
          resourceRequest.getSort());
      //set the count
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((validationRule) ->
          dto.addItem(build(uriInfo, resourceRequest, (ValidationRule) validationRule)));
    }
    return dto;
  }

}
