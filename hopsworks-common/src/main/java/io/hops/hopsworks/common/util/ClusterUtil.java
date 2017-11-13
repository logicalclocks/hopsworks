package io.hops.hopsworks.common.util;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.metadata.yarn.entity.quota.PriceMultiplicator;

/**
 *
 * Cluster Utilisation
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterUtil {

  private final static int CACHE_MAX_AGE = 10000;
  private YarnPriceMultiplicator multiplicator;
  private long lastUpdated = 0l;

  @EJB
  private ProjectController projectController;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;

  /**
   * Gets the yarn price multiplicator from cache if it is not older than
   * CACHE_MAX_AGE, from the database otherwise.
   *
   * @return YarnPriceMultiplicator
   */
  public YarnPriceMultiplicator getMultiplicator() {
    long timeNow = System.currentTimeMillis();
    if (timeNow - lastUpdated > CACHE_MAX_AGE || multiplicator == null) {
      lastUpdated = System.currentTimeMillis();
      multiplicator = yarnProjectsQuotaFacade.getMultiplicator(PriceMultiplicator.MultiplicatorType.GENERAL);
    }
    return multiplicator;
  }
}
