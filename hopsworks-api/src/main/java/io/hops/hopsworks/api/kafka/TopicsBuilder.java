package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.kafka.KafkaController;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
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
}
