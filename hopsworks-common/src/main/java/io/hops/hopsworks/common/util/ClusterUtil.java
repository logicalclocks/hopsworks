/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
