package io.hops.hopsworks.common.dao.workflow;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.w3c.dom.Element;
import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

@Entity
@XmlRootElement
@Table(name = "hopsworks.nodes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "classname")
@NamedQueries({
  @NamedQuery(name = "Node.findAll",
          query
          = "SELECT n FROM Node n"),
  @NamedQuery(name = "Node.findById",
          query
          = "SELECT n FROM Node n WHERE n.nodePK = :nodePK")})
public abstract class Node implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected NodePK nodePK;

  private String type;

  @JoinColumn(name = "workflow_id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Workflow workflow;

  public Node() {
    this.nodePK = new NodePK();
  }

  public Node(NodePK nodePK, String type) {
    this.nodePK = nodePK;
    this.type = type;
  }

  public Node(String id, Workflow workflow, String type) {
    this.nodePK = new NodePK(id, workflow.getId());
    this.workflow = workflow;
    this.type = type;
  }

  @JsonIgnore
  @XmlTransient
  public NodePK getNodePK() {
    return nodePK;
  }

  public void setNodePK(NodePK nodePK) {
    this.nodePK = nodePK;
  }

  public String getId() {
    return nodePK.getId();
  }

  public Integer getWorkflowId() {
    return nodePK.getWorkflowId();
  }

  public void setWorkflowId(Integer workflowId) {
    this.nodePK.setWorkflowId(workflowId);
  }

  public void setId(String id) {
    this.nodePK.setId(id);
  }

  @JsonIgnore
  @XmlTransient
  public Workflow getWorkflow() {
    return workflow;
  }

  public void setWorkflow(Workflow workflow) {
    this.workflow = workflow;
    this.setWorkflowId(workflow.getId());
  }

  @Basic(optional = false)
  @Column(name = "external_id")
  private String externalId;

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

  @Basic
  @Column(name = "type",
          nullable = false,
          length = 255)
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Column(name = "data")
  @Convert(converter = NodeDataConverter.class)
  private JsonNode data;

  public JsonNode getData() {
    return this.data;
  }

  public void setData(JsonNode data) {
    this.data = data;
  }

  public void setData(String data) throws IOException {
    this.data = new ObjectMapper().readTree(data);
  }

  @Basic(optional = false)
  @Column(name = "classname")
  private String classname;

  @JsonIgnore
  public String getClassname() {
    return classname;
  }

  public void setClassname(String classname) {
    this.classname = classname;
  }

  @JsonIgnore
  @XmlTransient
  public String getDecision() {
    if (this.getData().get("decision") == null) {
      return null;
    }
    return this.getData().get("decision").getValueAsText();
  }

  public void setDecision(String decision) {
    ((ObjectNode) this.getData()).put("decision", decision);
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (nodePK != null ? nodePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Node)) {
      return false;
    }
    Node other = (Node) object;
    if ((this.nodePK == null && other.nodePK != null)
            || (this.nodePK != null && !this.nodePK.equals(
                    other.nodePK))) {
      return false;
    }
    return true;
  }

  @OneToMany(cascade = CascadeType.ALL,
          fetch = FetchType.LAZY)
  @JoinTable(name = "hopsworks.edges",
          joinColumns = {
            @JoinColumn(table = "hopsworks.nodes",
                    name = "source_id",
                    referencedColumnName = "id"),
            @JoinColumn(table = "hopsworks.nodes",
                    name = "workflow_id",
                    referencedColumnName = "workflow_id")
          },
          inverseJoinColumns = {
            @JoinColumn(table = "hopsworks.nodes",
                    name = "target_id",
                    referencedColumnName = "id"),
            @JoinColumn(table = "hopsworks.nodes",
                    name = "workflow_id",
                    referencedColumnName = "workflow_id")
          }
  )
  private Set<Node> children;

  @JsonIgnore
  @XmlTransient
  public Set<Node> getChildren() {
    return this.children;
  }

  @XmlElement(name = "childrenIds")
  public Set<String> getChildrenIds() {
    Set<String> ids = new HashSet();
    for (Node child : this.children) {
      ids.add(child.getId());
    }
    return ids;
  }

  @OneToMany
  @JoinColumns({
    @JoinColumn(name = "source_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false),
    @JoinColumn(name = "workflow_id",
            referencedColumnName = "workflow_id",
            insertable = false,
            updatable = false)})
  private List<Edge> outEdges;

  @JsonIgnore
  @XmlTransient
  public List<Edge> getOutEdges() {
    return outEdges;
  }

  public void setChildren(Set<Node> children) {
    this.children = children;
  }

  public abstract Element getWorkflowElement(OozieFacade execution, Element root)
          throws UnsupportedOperationException, ProcessingException;

  @JsonIgnore
  @XmlTransient
  public String getPrefix() {
    String type = LOWER_HYPHEN.to(UPPER_CAMEL, this.type.toString());
    if (type.length() < 13) {
      return type;
    }
    return type.substring(0, 12);
  }

  @JsonIgnore
  @XmlTransient
  public String getOozieId() throws UnsupportedOperationException {
    if (Character.isDigit(this.getId().charAt(0))) {
      return this.getPrefix() + "_" + this.getId();
    }
    return this.getId();
  }

  @PrePersist
  @PreUpdate
  public void prePersist() {
    this.externalId = this.getOozieId();
  }

  public String getExternalId() {
    return this.externalId;
  }
}
