package io.hops.hopsworks.common.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;

import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;
import javax.persistence.Entity;

@Entity
@XmlRootElement
public class EmailNode extends Node {

  public EmailNode() {
    this.setId(UUID.randomUUID().toString());
    this.setType("email-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  @XmlElement(name = "to")
  public String getTo() {
    if (this.getData().get("to") == null) {
      return null;
    }
    return this.getData().get("to").getValueAsText();
  }

  @XmlElement(name = "body")
  public String getBody() {
    if (this.getData().get("body") == null) {
      return null;
    }
    return this.getData().get("body").getValueAsText();
  }

  @XmlElement(name = "subject")
  public String getSubject() {
    if (this.getData().get("subject") == null) {
      return null;
    }
    return this.getData().get("subject").getValueAsText();
  }

  @XmlElement(name = "cc")
  public String getCC() {
    if (this.getData().get("cc") == null) {
      return null;
    }
    return this.getData().get("cc").getValueAsText();
  }

  @XmlElement(name = "name")
  public String getName() {
    if (this.getData().get("name") == null) {
      return this.getId();
    }
    return this.getData().get("name").getValueAsText();
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          ProcessingException {
    /*
     * Add attachment content_type
     */

    if (this.getTo().isEmpty() || this.getBody().isEmpty() || this.getSubject().
            isEmpty()) {
      throw new ProcessingException("Missing arguments for Email");
    }
    if (this.getChildren().size() != 1) {
      throw new ProcessingException("Node should only contain one descendant");
    }

    if (execution.hasNodeId(this.getOozieId())) {
      return null;
    }

    Element action = execution.getDoc().createElement("action");
    action.setAttribute("name", this.getOozieId());

    Element email = execution.getDoc().createElement("email");
    Node child = this.getChildren().iterator().next();
    email.setAttribute("xmlns", "uri:oozie:email-action:0.1");

    Element to = execution.getDoc().createElement("to");
    to.setTextContent(this.getTo());
    email.appendChild(to);

    Element subject = execution.getDoc().createElement("subject");
    subject.setTextContent(this.getSubject());
    email.appendChild(subject);

    Element body = execution.getDoc().createElement("body");
    body.setTextContent(this.getBody());
    email.appendChild(body);

    if (this.getCC() != null && !this.getCC().isEmpty()) {
      Element cc = execution.getDoc().createElement("cc");
      cc.setTextContent(this.getCC());
      email.appendChild(cc);
    }

    action.appendChild(email);

    Element ok = execution.getDoc().createElement("ok");
    ok.setAttribute("to", child.getOozieId());
    action.appendChild(ok);

    Element error = execution.getDoc().createElement("error");
    error.setAttribute("to", "end");
    action.appendChild(error);

    root.appendChild(action);
    if (child.getClass() != JoinNode.class) {
      child.getWorkflowElement(execution, root);
    }
    execution.addNodeId(this.getOozieId());
    return action;
  }
}
