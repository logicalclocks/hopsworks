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
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.kafka.KafkaController;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
      Comparator order =
        sort.getParam().getValue().equals("DESC") ? Comparator.reverseOrder() : Comparator.naturalOrder();
      switch (KafkaFacade.TopicsSorts.valueOf(sort.getValue())) {
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
