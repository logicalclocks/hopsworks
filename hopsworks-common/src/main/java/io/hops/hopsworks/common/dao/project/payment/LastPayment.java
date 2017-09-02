package io.hops.hopsworks.common.dao.project.payment;

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "hopsworks.projects_last_payment")
@NamedQueries({
  @NamedQuery(name = "LastPayment.findByProjectname",
      query
      = "SELECT p FROM LastPayment p "
      + "WHERE p.projectname = :projectname")})
public class LastPayment {

  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 100)
  private String projectname;
  @Basic(optional = false)
  @NotNull
  @Column(name = "transaction_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date transactionDate;

  public String getProjectname() {
    return projectname;
  }

  public void setProjectname(String projectname) {
    this.projectname = projectname;
  }

  public Date getTransactionDate() {
    return transactionDate;
  }

  public void setTransactionDate(Date transactionDate) {
    this.transactionDate = transactionDate;
  }

}
