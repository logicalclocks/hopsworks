package se.kth.hopsworks.rest;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SparkService {
  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;
  
  SparkService setProjectId(Integer id){
    this.projectId = id;
    return this;
  }
}
