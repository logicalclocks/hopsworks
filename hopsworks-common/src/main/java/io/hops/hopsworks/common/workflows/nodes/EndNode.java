package io.hops.hopsworks.common.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class EndNode extends Node {

  public EndNode() {
    this.setId("end");
    this.setType("end-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          ProcessingException {

    if (this.getChildren().size() != 0) {
      throw new ProcessingException("End node should have no descendants");
    }
    if (execution.hasNodeId(this.getOozieId())) {
      return null;
    }

    Element end = execution.getDoc().createElement("end");
    end.setAttribute("name", this.getOozieId());

    Element kill = execution.getDoc().createElement("kill");
    Element message = execution.getDoc().createElement("message");
    kill.setAttribute("name", "kill");
    message.setTextContent(
            "Workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]");
    kill.appendChild(message);

    root.appendChild(kill);
    root.appendChild(end);
    execution.addNodeId(this.getOozieId());
    return end;
  }

  public String getOozieId() {
    return this.getId();
  }
}
