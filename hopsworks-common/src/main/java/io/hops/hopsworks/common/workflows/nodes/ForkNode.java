package io.hops.hopsworks.common.workflows.nodes;

import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.UUID;

@Entity
@XmlRootElement
public class ForkNode extends Node {

  public ForkNode() {
    this.setId(UUID.randomUUID().toString());
    this.setType("fork-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  @JsonIgnore
  @XmlTransient
  public String getJoinNodeId() {
    if (this.getData().get("joinNodeId") == null) {
      return null;
    }
    return this.getData().get("joinNodeId").getValueAsText();
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          ProcessingException {
    if (execution.hasNodeId(this.getOozieId())) {
      return null;
    }
    Element fork = execution.getDoc().createElement("fork");
    fork.setAttribute("name", this.getOozieId());
    root.appendChild(fork);
    for (Node node : this.getChildren()) {
      Element path = execution.getDoc().createElement("path");
      path.setAttribute("start", node.getOozieId());
      fork.appendChild(path);

      node.getWorkflowElement(execution, root);
    }
//        JoinNode joinNode = execution.getEm().find(JoinNode.class, new NodePK(this.getJoinNodeId(), this.getWorkflowId()));
//        joinNode.getWorkflowElement(execution, root);

    execution.addNodeId(this.getOozieId());
    return fork;
  }
}
