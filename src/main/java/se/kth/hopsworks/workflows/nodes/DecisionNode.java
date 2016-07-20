package se.kth.hopsworks.workflows.nodes;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.w3c.dom.Element;
import se.kth.hopsworks.workflows.Edge;
import se.kth.hopsworks.workflows.NodePK;
import se.kth.hopsworks.workflows.OozieFacade;
import se.kth.hopsworks.workflows.Node;

import javax.persistence.Entity;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.UUID;

@Entity
@XmlRootElement
public class DecisionNode extends Node {
    public DecisionNode(){
        this.setId(UUID.randomUUID().toString());
        this.setType("decision-node");
        this.setData(new ObjectMapper().createObjectNode());
    }

    @JsonIgnore
    @XmlTransient
    public Node getDefaultNode(){
        Node defaultNode = null;
        for(Edge edge: this.getOutEdges()){
            if(edge.getType().equals("default-decision-edge")) defaultNode = edge.getTarget();
        }
        return defaultNode;
    }

    public void setTargetNodeId(String id){
        ((ObjectNode)this.getData()).put("targetNodeId", id);
    }

    public Element getWorkflowElement(OozieFacade execution, Element root) throws ProcessingException {
        if(execution.hasNodeId(this.getOozieId())) return null;
        Element action = execution.getDoc().createElement("decision");
        action.setAttribute("name", this.getOozieId());
        root.appendChild(action);

        Element decisions = execution.getDoc().createElement("switch");
        action.appendChild(decisions);
        Node defaultNode = this.getDefaultNode();
        Element decision;
        for(Node node: this.getChildren()){

            if(node.equals(defaultNode)) continue;
            if(node.getDecision() == null) throw new ProcessingException("Decision should not be empty");
            decision = execution.getDoc().createElement("case");
            decision.setTextContent(node.getDecision());
            decision.setAttribute("to", node.getOozieId());
            decisions.appendChild(decision);

            node.getWorkflowElement(execution, root);

        }
        decision = execution.getDoc().createElement("default");
        decision.setAttribute("to", defaultNode.getOozieId());
        defaultNode.getWorkflowElement(execution, root);
        decisions.appendChild(decision);

        execution.addNodeId(this.getOozieId());
        return action;
    }
}
