/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.dao.hdfs;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.hdfs.HdfsLeDescriptors;

import java.util.Random;

@Stateless
public class HdfsLeDescriptorsFacade extends AbstractFacade<HdfsLeDescriptors> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsLeDescriptorsFacade() {
    super(HdfsLeDescriptors.class);
  }

  /**
   * Get the hostname:port to a random NN, randomness is needed to ensure workload spread
   * From a client perspective it does not matter if it bootstraps on a lead NN or not
   *
   * @return
   */
  public String getRPCEndpoint() {
    try {
      List<HdfsLeDescriptors> hdfsLeDescriptorsList = em.createNamedQuery(
              "HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getResultList();
      if (hdfsLeDescriptorsList.isEmpty()) {
        return null;
      } else {
        // Get a random NN Address
        int randomNNIndex = new Random().nextInt(hdfsLeDescriptorsList.size());
        HdfsLeDescriptors randomNN = hdfsLeDescriptorsList.get(randomNNIndex);
        String rpcAddresses = randomNN.getRpcAddresses();
        rpcAddresses = rpcAddresses.trim();

        if(rpcAddresses.contains(",")) {
          String[] rpcAddressArr = rpcAddresses.split(",");
          return rpcAddressArr[0];
        } else {
          return rpcAddresses;
        }
      }
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the web UI address for the leader NN
   *
   * @return
   */
  public String getLeaderWebEndpoint() {
    try {
      List<HdfsLeDescriptors> hdfsLeDescriptorsList = em.createNamedQuery(
          "HdfsLeDescriptors.findLeader", HdfsLeDescriptors.class).getResultList();
      if (hdfsLeDescriptorsList.isEmpty()) {
        return null;
      } else {
        return hdfsLeDescriptorsList.get(0).getHttpAddress();
      }
    } catch (NoResultException e) {
      return null;
    }
  }


}
