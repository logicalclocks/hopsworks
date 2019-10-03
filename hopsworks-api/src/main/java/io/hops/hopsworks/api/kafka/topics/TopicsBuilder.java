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
package io.hops.hopsworks.api.kafka.topics;

import io.hops.hopsworks.common.api.CollectionsBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.kafka.KafkaController;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.hops.hopsworks.common.dao.kafka.KafkaFacade.TopicsFilters;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TopicsBuilder extends CollectionsBuilder<TopicDTO> {
  
  @EJB
  private KafkaController kafkaController;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  
  @Override
  protected List<TopicDTO> getAll(Project project) {
    List<TopicDTO> allTopics = kafkaController.findTopicsByProject(project);
    allTopics.addAll(kafkaController.findSharedTopicsByProject(project.getId()));
    return allTopics;
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
      switch (KafkaFacade.TopicsSorts.valueOf(sort.getValue())) {
        case NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getName);
          } else {
            comparator.thenComparing(TopicDTO::getName);
          }
          if (sort.getParam().getValue().equals("DESC")) {
            comparator = comparator.reversed();
          }
          break;
        case SCHEMA_NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getSchemaName);
          } else {
            comparator.thenComparing(TopicDTO::getSchemaName);
          }
          if (sort.getParam().getValue().equals("DESC")) {
            comparator = comparator.reversed();
          }
          break;
      }
    }
  
    if (comparator == null) {
      return list;
    } else {
      return list.stream()
        .sorted(comparator)
        .collect(Collectors.toList());
    }
  }
  
  //TODO: SQL here is not working
  public List<TopicDTO> build(Project project, ResourceRequest resourceRequest) {
    List<ProjectTopics> ptList  = projectTopicsFacade.findTopicsByProject(project,
      resourceRequest.getOffset(),
      resourceRequest.getLimit(),
      resourceRequest.getFilter(),
      resourceRequest.getSort());
  
    List<TopicDTO> topics = new ArrayList<>();
    for (ProjectTopics pt : ptList) {
      topics.add(new TopicDTO(pt.getTopicName(),
        pt.getSchemaTopics().getSchemaTopicsPK().getName(),
        pt.getSchemaTopics().getSchemaTopicsPK().getVersion(),
        false));
    }
    return topics;
  }
}
