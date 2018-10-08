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

package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.ClientWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.common.exception.DelaException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class RemoteDelaController {

  private static final Logger LOGGER = Logger.getLogger(RemoteDelaController.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtrl;

  private void checkReady() throws DelaException {
    delaStateCtrl.checkHopsworksDelaSetup();
  }

  //********************************************************************************************************************
  public FilePreviewDTO readme(String publicDSId, ClusterAddressDTO source) throws DelaException {
    checkReady();
    try {
      ClientWrapper client = getClient(source.getDelaClusterAddress(), Path.readme(publicDSId), FilePreviewDTO.class);
      LOGGER.log(Settings.DELA_DEBUG, "dela:cross:readme {0}", client.getFullPath());
      FilePreviewDTO result = (FilePreviewDTO) client.doGet();
      LOGGER.log(Settings.DELA_DEBUG, "dela:cross:readme:done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ex) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.REMOTE_DELA, null, ex.getMessage(), ex);
    }
  }

  private ClientWrapper getClient(String delaClusterAddress, String path, Class resultClass)
    throws DelaException {
    return ClientWrapper.httpsInstance(resultClass).setTarget(delaClusterAddress).setPath(path);
  }

  public static class Path {

    public static String readme(String publicDSId) {
      return "/remote/dela/datasets/" + publicDSId + "/readme";
    }
  }
}
