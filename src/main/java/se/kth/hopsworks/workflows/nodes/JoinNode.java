package se.kth.hopsworks.workflows.nodes;

import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@Entity
@XmlRootElement
public class JoinNode extends Node{
    public JoinNode(){
        this.setId(UUID.randomUUID().toString());
        this.setType("join-node");
        this.setData(new ObjectMapper().createObjectNode());
    }

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException {
        if(this.getChildren().size() != 1) throw new ProcessingException("Node should only contain one descendant");
        if(execution.hasNodeId(this.getOozieId())) return null;

        Element element = execution.getDoc().createElement("join");
        root.appendChild(element);

        Node child = this.getChildren().iterator().next();

        element.setAttribute("to", child.getOozieId());
        element.setAttribute("name", this.getOozieId());
        child.getWorkflowElement(execution, root);

        execution.addNodeId(this.getOozieId());
        return element;
    }
}
