package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KafkaBuilder {
  
  @EJB
  private KafkaFacade kafkaFacade;
  
  private TopicDTO expand(TopicDTO dto, Optional<ResourceRequest> resourceRequest) {
    if (resourceRequest.isPresent() && resourceRequest.get().contains(ResourceRequest.Name.KAFKA)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public TopicDTO buildItemsTopic(UriInfo uriInfo, Optional<ResourceRequest> resourceRequest, Project project) {
//    TopicDTO dto = new TopicDTO(uriInfo);
//    dto = expand(dto, resourceRequest);
//    if (dto.isExpand()) {
//      AbstractFacade.CollectionInfo collectionInfo = kafkaFacade.findTopicsByProject(project, resourceRequest);
//    }
    return null;
  }
}
