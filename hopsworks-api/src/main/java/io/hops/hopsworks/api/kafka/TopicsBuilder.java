package io.hops.hopsworks.api.kafka;

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
  protected List<TopicDTO> getItems(Project project) {
    List<TopicDTO> allTopics = kafkaController.findTopicDtosByProject(project);
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
}
