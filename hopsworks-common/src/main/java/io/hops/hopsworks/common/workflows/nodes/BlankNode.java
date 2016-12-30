package io.hops.hopsworks.common.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.UUID;

@Entity
@XmlRootElement
public class BlankNode extends Node {

  public BlankNode() {
    this.setId(UUID.randomUUID().toString());
    this.setType("blank-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          UnsupportedOperationException {
    throw new UnsupportedOperationException(
            "Blank node is not part of the Workflow");
  }
//    public String getOozieId(){
//        return null;
//    }
}
