package io.hops.hopsworks.common.dao.workflow;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class EdgePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "id",
          nullable = false,
          length = 255)
  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Basic(optional = false)
  @NotNull
  @Column(name = "workflow_id",
          nullable = false)
  private Integer workflowId;

  public Integer getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Integer workflowId) {
    this.workflowId = workflowId;
  }

  public EdgePK() {
  }

  public EdgePK(String id, int workflowId) {
    this.id = id;
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EdgePK edgePK = (EdgePK) o;

    if (id != null ? !id.equals(edgePK.id) : edgePK.id != null) {
      return false;
    }
    if (workflowId != null ? !workflowId.equals(edgePK.workflowId)
            : edgePK.workflowId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (workflowId != null ? workflowId.hashCode() : 0);
    return result;
  }

}
