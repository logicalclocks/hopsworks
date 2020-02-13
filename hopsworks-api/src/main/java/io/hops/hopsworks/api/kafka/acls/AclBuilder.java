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

package io.hops.hopsworks.api.kafka.acls;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.AclUser;
import io.hops.hopsworks.common.dao.kafka.AclUserDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.kafka.TopicAclsFacade;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.persistence.entity.kafka.TopicAcls;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AclBuilder {
  @EJB
  private KafkaController kafkaController;
  @EJB
  private TopicAclsFacade topicAclsFacade;
  
  public AclDTO build(UriInfo uriInfo, TopicAcls acl) {
    AclDTO dto = new AclDTO(acl.getId(),
      KafkaConst.getProjectNameFromPrincipal(acl.getPrincipal()),
      acl.getUser().getEmail(),
      acl.getPermissionType(),
      acl.getOperationType(),
      acl.getHost(),
      acl.getRole());
    dto.setHref(uriInfo.getAbsolutePathBuilder().path(Integer.toString(acl.getId())).build());
    return dto;
  }
  
  public AclDTO build(UriInfo uriInfo, Project project, String topicName, ResourceRequest resourceRequest) {
    AclDTO dto = new AclDTO();
    aclUri(dto, uriInfo, project, topicName);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = topicAclsFacade.findByTopicName(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(),
        topicName);
      dto.setCount(collectionInfo.getCount());
      UriBuilder uriBuilder = getAclUri(uriInfo, project, topicName);
      collectionInfo.getItems().forEach((acl) ->
        dto.addItem(build((TopicAcls) acl, uriBuilder)));
    }
    return dto;
  }
  
  private AclDTO build(TopicAcls acl, UriBuilder uriBuilder) {
    AclDTO dto = new AclDTO();
    dto.setHost(acl.getHost());
    dto.setId(acl.getId());
    dto.setOperationType(acl.getOperationType());
    dto.setPermissionType(acl.getPermissionType());
    dto.setProjectName(KafkaConst.getProjectNameFromPrincipal(acl.getPrincipal()));
    dto.setRole(acl.getRole());
    dto.setUserEmail(acl.getUser().getEmail());
    dto.setHref(uriBuilder.clone().path(Integer.toString(acl.getId())).build());
    return dto;
  }
  
  private void expand(AclDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.KAFKA)) {
      dto.setExpand(true);
    }
  }
  
  private void aclUri(AclDTO dto, UriInfo uriInfo, Project project, String topicName) {
    URI uri = getAclUri(uriInfo, project, topicName).build();
    dto.setHref(uri);
  }
  
  public UriBuilder getAclUri(UriInfo uriInfo, Project project, String topicName) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.TOPICS.toString().toLowerCase())
      .path(topicName)
      .path(ResourceRequest.Name.ACL.toString().toLowerCase());
  }
  
  private UriBuilder getAclUri(UriInfo uriInfo, Project project, String topicName, Integer aclId) {
    return getAclUri(uriInfo, project, topicName)
      .path(Integer.toString(aclId));
  }
  
  public AclDTO getAclByTopicAndId(UriInfo uriInfo, Project project, String topicName, Integer aclId)
    throws KafkaException {
    
    AclDTO dto = new AclDTO();
    URI uri = getAclUri(uriInfo, project, topicName, aclId).build();
    dto.setHref(uri);
    Optional<TopicAcls> aclsOptional = kafkaController.findAclByIdAndTopic(topicName, aclId);
    if (aclsOptional.isPresent()) {
      TopicAcls acl = aclsOptional.get();
      dto.setId(acl.getId());
      dto.setProjectName(KafkaConst.getProjectNameFromPrincipal(acl.getPrincipal()));
      dto.setUserEmail(acl.getUser().getEmail());
      dto.setPermissionType(acl.getPermissionType());
      dto.setOperationType(acl.getOperationType());
      dto.setHost(acl.getHost());
      dto.setRole(acl.getRole());
    }
    return dto;
  }
  
  public AclUserDTO buildAclUser(UriInfo uriInfo, Project project, String topicName) {
    AclUserDTO dto = new AclUserDTO();
    URI uri = getAclUserUri(uriInfo, project, topicName).build();
    dto.setHref(uri);
    List<AclUser> list = kafkaController.getTopicAclUsers(project, topicName);
    dto.setCount(Integer.toUnsignedLong(list.size()));
    list.forEach((aclUser) -> dto.addItem(build((AclUser) aclUser)));
    return dto;
  }
  
  private AclUserDTO build(AclUser aclUser) {
    AclUserDTO dto = new AclUserDTO();
    dto.setProjectName(aclUser.getProjectName());
    dto.setUserEmails(aclUser.getUserEmails());
    return dto;
  }
  
  private UriBuilder getAclUserUri(UriInfo uriInfo, Project project, String topicName) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.TOPICS.toString().toLowerCase())
      .path(topicName)
      .path(ResourceRequest.Name.USERS.toString().toLowerCase())
      .path(ResourceRequest.Name.ACL.toString().toLowerCase());
  }
}
