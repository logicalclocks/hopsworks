package se.kth.hopsworks.workflows;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.workflows.nodes.*;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Stateless
public class NodeFacade extends AbstractFacade<Node> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Node> findAll() {
        TypedQuery<Node> query = em.createNamedQuery("Node.findAll",
                Node.class);
        return query.getResultList();
    }

    public Node findById(NodePK id) {
        TypedQuery<Node> query =  em.createNamedQuery("Node.findById", Node.class);
        query.setParameter("nodePK", id);
        return query.getSingleResult();
    }

    public NodeFacade() {
        super(Node.class);
    }

    public void persist(Node node){
        Date date = new Date();
        Workflow workflow = node.getWorkflow();
        workflow.setUpdatedAt(date);
        node.setCreatedAt(date);
        em.persist(node);
//        em.merge(workflow);
    }

    public void flush() {
        em.flush();
    }

    public Node merge(Node node) {
        Date date = new Date();
        Workflow workflow = node.getWorkflow();
        node.setUpdatedAt(date);
        workflow.setUpdatedAt(date);
//        em.merge(workflow);
        return em.merge(node);

    }

    public void remove(Node node) {
        em.remove(em.merge(node));
    }

    public Node refresh(Node node) {
        Node n = findById(node.getNodePK());
        em.refresh(n);
        return n;
    }
    public void buildXml(String xml, Workflow workflow) throws IOException, SAXException, ParserConfigurationException, XPathException{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        Element root = doc.getDocumentElement();
        workflow.setName(root.getAttribute("name"));
        em.persist(workflow);
        em.flush();

        NodeList nodes = root.getChildNodes();

        Collection<Edge> edges = new HashSet<Edge>();

        for(int i = 0; i < nodes.getLength(); i++){
            buildXmlNode((Element)nodes.item(i), edges, workflow);
        }

        for(Edge e: edges){
            e.setWorkflow(workflow);
            em.persist(e);
        }
        em.flush();
    }
    public Node buildXmlNode(Element elem, Collection<Edge> edges, Workflow workflow) throws XPathException {
        Node node = null, inNode;
        Edge edge;
        NodeList nodes;
        Element child, child2;
        XPath xPath = XPathFactory.newInstance().newXPath();
        if(!elem.getNodeName().equals("start")){
            try{
                node = findById(new NodePK(elem.getAttribute("name"), workflow.getId()));
                return node;
            }catch(NoResultException e) {
                node = null;
            }
        }
        switch (elem.getNodeName()){
            case "start":
                node = new RootNode();
                node.setWorkflow(workflow);
                persist(node);
                node = refresh(node);

                edge = new Edge();
                edge.setSourceId(node.getId());
                edge.setTargetId(elem.getAttribute("to"));
                edges.add(edge);
                break;
            case "end":
                node = new EndNode();
                node.setId(elem.getAttribute("name"));
                node.setWorkflow(workflow);
                persist(node);
                break;
            case "decision":
                node = new DecisionNode();
                node.setWorkflow(workflow);
                node.setId(elem.getAttribute("name"));
                persist(node);
                node = refresh(node);

                child = (Element)elem.getChildNodes().item(0);
                nodes = child.getElementsByTagName("case");

                for(int j = 0; j < nodes.getLength(); j++){

                    child = (Element) nodes.item(j);

                    try{
                        inNode = findById(new NodePK(child.getAttribute("to"), workflow.getId()));
                    }catch(NoResultException e) {
                        child2 = (Element) xPath.compile("//*[@name='" +child.getAttribute("to")+ "']").evaluate(elem, XPathConstants.NODE);
                        inNode = buildXmlNode(child2, edges, workflow);
                    }

                    inNode.setDecision(child.getTextContent());
                    merge(inNode);
                    edge = new Edge();
                    edge.setSourceId(node.getId());
                    edge.setTargetId(child.getAttribute("to"));
                    edges.add(edge);
                }

                child = (Element)elem.getChildNodes().item(0);
                child = (Element)child.getElementsByTagName("default").item(0);

                try{
                    inNode = findById(new NodePK(child.getAttribute("to"), workflow.getId()));
                }catch(NoResultException e) {
                    child2 = (Element) xPath.compile("//*[@name='" +child.getAttribute("to")+ "']").evaluate(elem, XPathConstants.NODE);
                    inNode = buildXmlNode(child2, edges, workflow);
                }

                ((DecisionNode) node).setTargetNodeId(inNode.getId());
                edge = new Edge();
                edge.setType("default-decision-edge");
                edge.setSourceId(node.getId());
                edge.setTargetId(child.getAttribute("to"));
                edges.add(edge);
                break;
            case "fork":
                node = new ForkNode();
                node.setWorkflow(workflow);
                node.setId(elem.getAttribute("name"));
                persist(node);
                node = refresh(node);

                for(int j = 0; j < elem.getChildNodes().getLength(); j++){
                    child = (Element)elem.getChildNodes().item(j);
                    edge = new Edge();
                    edge.setSourceId(node.getId());
                    edge.setTargetId(child.getAttribute("start"));
                    edges.add(edge);
                }

                break;
            case "join":
                node = new JoinNode();
                node.setWorkflow(workflow);
                node.setId(elem.getAttribute("name"));
                persist(node);
                node = refresh(node);

                edge = new Edge();
                edge.setSourceId(node.getId());
                edge.setTargetId(elem.getAttribute("to"));
                edges.add(edge);
                break;
            case "action":
                if(elem.getElementsByTagName("spark").getLength() == 1) node = new SparkCustomNode(elem);
                if(elem.getElementsByTagName("email").getLength() == 1) node = new EmailNode();
                if(node == null) throw new ProcessingException("Unsuported Action");

                node.setId(elem.getAttribute("name"));
                node.setWorkflow(workflow);
                persist(node);
                node = refresh(node);

                child = (Element) elem.getElementsByTagName("ok").item(0);
                edge = new Edge();
                edge.setSourceId(node.getId());
                edge.setTargetId(child.getAttribute("to"));
                edges.add(edge);
                break;
            case "kill":
                break;
            default:
                throw new ProcessingException("Unsuported Action");
        }
        return node;
    }
}
