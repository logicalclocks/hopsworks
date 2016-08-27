package se.kth.hopsworks.workflows;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.w3c.dom.Element;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.workflows.nodes.BlankNode;
import se.kth.hopsworks.workflows.nodes.EndNode;
import se.kth.hopsworks.workflows.nodes.RootNode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.ParserConfigurationException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "hopsworks.workflows")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Workflow.findAll",
                query
                        = "SELECT w FROM Workflow w"),
        @NamedQuery(name = "Workflow.find",
                query
                        = "SELECT w FROM Workflow w WHERE w.id = :id AND w.projectId = :projectId"),
        @NamedQuery(name = "Workflow.findByName",
                query
                        = "SELECT w FROM Workflow w WHERE w.name = :name")})
public class Workflow implements Serializable {

    public Workflow(){

    }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "name", nullable = false, length = 20)
    private String name;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic(optional = false)
    @NotNull
    @Column(name = "project_id", nullable = false)
    private Integer projectId;
    public Integer getProjectId() {
        return projectId;
    }
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    @Basic(optional = false)
    @Column(name = "created_at")
    private Date createdAt;
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Basic(optional = false)
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Workflow workflows = (Workflow) o;

        if (id != null ? !id.equals(workflows.id) : workflows.id != null) return false;
        if (name != null ? !name.equals(workflows.name) : workflows.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    @OrderBy("workflowTimestamp DESC")
    private List<WorkflowExecution> workflowExecutions;
    @JsonIgnore
    @XmlTransient
    public List<WorkflowExecution> getWorkflowExecutions() {
        return workflowExecutions;
    }


    public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
        this.workflowExecutions = workflowExecutions;
    }

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<Edge> edges;
    public Collection<Edge> getEdges() {
        return edges;
    }

    public void setEdges(Collection<Edge> edges) {
        this.edges = edges;
    }


    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<Node> nodes;
    public Collection<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private Collection<BlankNode> blankNodes;

    @JsonIgnore
    public Collection<BlankNode> getBlankNodes() {
        return blankNodes;
    }

    @OneToOne(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private RootNode rootNode;

    @JsonIgnore
    public RootNode getRootNode() {
        return rootNode;
    }

    @OneToOne(cascade = CascadeType.REMOVE, mappedBy = "workflow")
    private EndNode endNode;

    @JsonIgnore
    public EndNode getEndNode() {
        return endNode;
    }

    public Boolean isComplete(){
        if(this.getBlankNodes().size() > 0) return false;
        return true;
    }


    public void makeWorkflowFile(OozieFacade execution) throws ParserConfigurationException, ProcessingException, UnsupportedOperationException{

        if(!this.isComplete()) throw new ProcessingException("Workflow is not in a complete state");

        Element workflow = execution.getDoc().createElement("workflow-app");
        workflow.setAttribute("name", this.getName());
        workflow.setAttribute("xmlns", "uri:oozie:workflow:0.5");
        execution.addNodeId(rootNode.getOozieId());
        execution.addNodeId(endNode.getOozieId());
        rootNode.getWorkflowElement(execution, workflow);
        execution.removeNodeId(endNode.getOozieId());
        endNode.getWorkflowElement(execution, workflow);
        execution.getDoc().appendChild(workflow);
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @PrimaryKeyJoinColumn(name="project_id")
    private Project project;
    @JsonIgnore
    @XmlTransient
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

}
