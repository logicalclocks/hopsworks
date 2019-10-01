package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.api.ResourceRequest;
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
public class TopicsBuilder {
  
  @EJB
  private KafkaController kafkaController;
  
  public List<TopicDTO> buildItems(Project project, ResourceRequest resourceRequest) {
    List<TopicDTO> allTopics = kafkaController.findTopicDtosByProject(project);
    allTopics.addAll(kafkaController.findSharedTopicsByProject(project.getId()));
    
    for (AbstractFacade.FilterBy tFilter : resourceRequest.getFilter()) {
      allTopics = filterTopics(tFilter, allTopics);
    }
    
    allTopics = sortTopics(resourceRequest.getSort(), allTopics);
    
    return allTopics;
  }
  
  private List<TopicDTO> filterTopics(AbstractFacade.FilterBy filterBy, List<TopicDTO> list) {
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
  
  private List<TopicDTO> sortTopics(Set<? extends AbstractFacade.SortBy> sortBySet, List<TopicDTO> list) {
    Iterator<? extends AbstractFacade.SortBy> it = sortBySet.iterator();
    Comparator<TopicDTO> comparator = null;
    while (it.hasNext()) {
      switch (KafkaFacade.TopicsSorts.valueOf(it.next().getValue())) {
        case NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getName);
          } else {
            comparator.thenComparing(TopicDTO::getName);
          }
          break;
        case SCHEMA_NAME:
          if (comparator == null) {
            comparator = Comparator.comparing(TopicDTO::getSchemaName);
          } else {
            comparator.thenComparing(TopicDTO::getSchemaName);
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
