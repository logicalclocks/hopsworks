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
package io.hops.hopsworks.api.kafka.topics;

import io.hops.hopsworks.common.api.CollectionsBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.PartitionDetailsDTO;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.SharedProjectDTO;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade.TopicsFilters;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TopicsBuilder extends CollectionsBuilder<TopicDTO> {
  
  @EJB
  private KafkaController kafkaController;
  
  @Override
  protected List<TopicDTO> getAll(Project project) {
    List<TopicDTO> allTopics = kafkaController.findTopicsByProject(project);
    allTopics.addAll(kafkaController.findSharedTopicsByProject(project.getId()));
    return allTopics;
  }
  
  public TopicDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    URI href = uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.TOPICS.toString().toLowerCase())
      .build();
    TopicDTO dto = new TopicDTO();
    dto.setHref(href);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<TopicDTO> list = buildItems(project, resourceRequest);
      dto.setCount(Integer.toUnsignedLong(list.size()));
      list.forEach(topic -> dto.addItem(build(uriInfo, resourceRequest, topic, project)));
    }
    return dto;
  }
  
  public UriBuilder topicUri(UriInfo uriInfo, Project project, String topicName) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.TOPICS.toString().toLowerCase())
      .path(topicName);
  }
  
  public UriBuilder topicUri(UriInfo uriInfo, Integer projectId, String topicName) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(projectId))
      .path(ResourceRequest.Name.KAFKA.toString().toLowerCase())
      .path(ResourceRequest.Name.TOPICS.toString().toLowerCase())
      .path(topicName);
  }
  
  public UriBuilder sharedProjectUri(UriInfo uriInfo, Project project, String topicName) {
    return topicUri(uriInfo, project, topicName)
      .path(ResourceRequest.Name.SHARED.toString().toLowerCase());
  }
  
  private TopicDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, TopicDTO topic, Project project) {
    if (topic.isShared()) {
      topic.setHref(topicUri(uriInfo, topic.getOwnerProjectId(), topic.getName()).build());
    } else {
      topic.setHref(topicUri(uriInfo, project, topic.getName()).build());
    }
    expand(topic, resourceRequest);
    return topic;
  }
  
  private void expand(TopicDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.KAFKA)) {
      dto.setExpand(true);
    }
  }
  
  public PartitionDetailsDTO buildTopicDetails(UriInfo uriInfo, Project project, String topicName)
    throws KafkaException, InterruptedException, ExecutionException {
    
    PartitionDetailsDTO dto = new PartitionDetailsDTO();
    dto.setHref(topicUri(uriInfo, project, topicName).build());
    List<PartitionDetailsDTO> list = kafkaController.getTopicDetails(project, topicName).get();
    dto.setCount(Integer.toUnsignedLong(list.size()));
    list.forEach(dto::addItem);
    return dto;
  }
  
  public SharedProjectDTO buildSharedProject(UriInfo uriInfo, Project project, String topicName) {
    SharedProjectDTO dto = new SharedProjectDTO();
    dto.setHref(sharedProjectUri(uriInfo, project, topicName).build());
    List<SharedProjectDTO> list = kafkaController.getTopicSharedProjects(topicName, project.getId());
    dto.setCount(Integer.toUnsignedLong(list.size()));
    list.forEach(dto::addItem);
    return dto;
  }
  
  @Override
  protected List<TopicDTO> filterTopics(List<TopicDTO> list, AbstractFacade.FilterBy filterBy) {
    switch (TopicsFilters.valueOf(filterBy.getValue())) {
      case SHARED:
        boolean isShared = Boolean.valueOf(filterBy.getParam());
        return list.stream()
          .filter(t -> t.isShared() == isShared)
          .collect(Collectors.toList());
      default:
        return list;
    }
  }
  
  @Override
  protected List<TopicDTO> sortTopics(List<TopicDTO> list, Set<? extends AbstractFacade.SortBy> sortBySet) {
    Iterator<? extends AbstractFacade.SortBy> it = sortBySet.iterator();
    Comparator<TopicDTO> comparator = null;
    while (it.hasNext()) {
      AbstractFacade.SortBy sort = it.next();
      Comparator order =
        sort.getParam().getValue().equals("DESC") ? Comparator.reverseOrder() : Comparator.naturalOrder();
      switch (ProjectTopicsFacade.TopicsSorts.valueOf(sort.getValue())) {
        case NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getName, order);
          } else {
            comparator = comparator.thenComparing(TopicDTO::getName, order);
          }
          break;
        case SCHEMA_NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getSchemaName, order);
          } else {
            comparator = comparator.thenComparing(TopicDTO::getSchemaName, order);
          }
          break;
      }
    }
  
    if (comparator != null) {
      list.sort(comparator);
    }
    return list;
  }
}
