package io.hops.hopsworks.common.dao.workflow;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
public class WorkflowDTO {

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  private Integer id;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name;

  public Collection<Node> getNodes() {
    return nodes;
  }

  public void setNodes(Collection<Node> nodes) {
    this.nodes = nodes;
  }

  private Collection<Node> nodes;

  public WorkflowDTO() {

  }

  public WorkflowDTO(Workflow workflow) {
    this.id = workflow.getId();
    this.name = workflow.getName();
    this.nodes = workflow.getNodes();
  }

}
