package io.hops.hopsworks.common.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class RootNode extends Node {

  public RootNode() {
    this.setId("root");
    this.setType("root-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          ProcessingException {

    if (this.getChildren().size() != 1) {
      throw new ProcessingException("Node should only contain one descendant");
    }

    Element element = execution.getDoc().createElement("start");
    root.appendChild(element);

    Node child = this.getChildren().iterator().next();

    element.setAttribute("to", child.getOozieId());
    child.getWorkflowElement(execution, root);

    return element;
  }

  public String getOozieId() {
    return ":start:";
  }

}
