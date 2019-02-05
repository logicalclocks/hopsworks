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

import com.google.gson.Gson;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.ClientWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.common.exception.DelaException;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.ExtendedDetails;
import io.hops.hopsworks.dela.old_dto.HDFSEndpoint;
import io.hops.hopsworks.dela.old_dto.HDFSResource;
import io.hops.hopsworks.dela.old_dto.HopsContentsReqJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.dela.old_dto.HopsDatasetDetailsDTO;
import io.hops.hopsworks.dela.old_dto.HopsTorrentAdvanceDownload;
import io.hops.hopsworks.dela.old_dto.HopsTorrentStartDownload;
import io.hops.hopsworks.dela.old_dto.HopsTorrentUpload;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.SuccessJSON;
import io.hops.hopsworks.dela.old_dto.TorrentExtendedStatusJSON;
import io.hops.hopsworks.dela.old_dto.TorrentId;
import io.hops.hopsworks.util.SettingsHelper;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TransferDelaController {

  private Logger logger = Logger.getLogger(TransferDelaController.class.getName());
  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateController;

  public AddressJSON getDelaPublicEndpoint(String delaVersion) throws DelaException {
    String delaTransferHttpEndpoint = SettingsHelper.delaTransferHttpEndpoint(settings);
    try {
      ClientWrapper<AddressJSON> rc = ClientWrapper
        .httpInstance(AddressJSON.class)
        .setTarget(delaTransferHttpEndpoint)
        .setPath(TransferDela.CONTACT)
        .setPayload(delaVersion);
      logger.log(Settings.DELA_DEBUG, "dela:contact {0}", rc.getFullPath());
      AddressJSON result = rc.doPost();
      logger.log(Settings.DELA_DEBUG, "dela:contact - done {0} {1}", new Object[]{rc.getFullPath(), result.getIp()});
      return result;
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela:contact - communication fail{0}", ise.getMessage());
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public void upload(String publicDSId, HopsDatasetDetailsDTO datasetDetails, HDFSResource resource,
    HDFSEndpoint endpoint) throws DelaException {
    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    logger.log(Settings.DELA_DEBUG, "{0} upload - transfer");
    HopsTorrentUpload reqContent = new HopsTorrentUpload(new TorrentId(publicDSId), datasetDetails.getDatasetName(),
      datasetDetails.getProjectId(), datasetDetails.getDatasetId(), resource, endpoint);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/upload/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public void startDownload(String publicDSId, HopsDatasetDetailsDTO datasetDetails, HDFSResource resource,
    HDFSEndpoint endpoint, List<ClusterAddressDTO> bootstrap)
    throws DelaException {

    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    List<AddressJSON> bootstrapAdr = new LinkedList<>();
    Gson gson = new Gson();
    for(ClusterAddressDTO b : bootstrap) {
      bootstrapAdr.add(gson.fromJson(b.getDelaTransferAddress(), AddressJSON.class));
    }
    HopsTorrentStartDownload reqContent = new HopsTorrentStartDownload(new TorrentId(publicDSId), datasetDetails.
      getDatasetName(), datasetDetails.getProjectId(), datasetDetails.getDatasetId(), resource, bootstrapAdr, endpoint);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/download/start/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public void advanceDownload(String publicDSId, HDFSEndpoint hdfsEndpoint, KafkaEndpoint kafkaEndpoint,
    ExtendedDetails details)
    throws DelaException {

    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    
    HopsTorrentAdvanceDownload reqContent = new HopsTorrentAdvanceDownload(new TorrentId(publicDSId),
      kafkaEndpoint, hdfsEndpoint, details);
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/download/advance/xml")
        .setPayload(reqContent);
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      logger.log(Level.WARNING, "dela communication fail:{0}", ise.getMessage());
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public void cancel(String publicDSId) throws DelaException {
    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    
    try {
      ClientWrapper<SuccessJSON> rc = ClientWrapper
        .httpInstance(SuccessJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("torrent/hops/stop")
        .setPayload(new TorrentId(publicDSId));
      SuccessJSON result = rc.doPost();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public HopsContentsSummaryJSON.Contents getContents(List<Integer> projectIds) throws DelaException {
    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.WARNING,
        DelaException.Source.LOCAL);
    }
    HopsContentsReqJSON reqContent = new HopsContentsReqJSON(projectIds);
    try {
      ClientWrapper<HopsContentsSummaryJSON.JsonWrapper> rc = ClientWrapper
        .httpInstance(HopsContentsSummaryJSON.JsonWrapper.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("library/hopscontents")
        .setPayload(reqContent);
      HopsContentsSummaryJSON.Contents result = rc.doPost().resolve();
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  public TorrentExtendedStatusJSON details(TorrentId torrentId) throws DelaException {
    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    try {
      ClientWrapper<TorrentExtendedStatusJSON> rc = ClientWrapper
        .httpInstance(TorrentExtendedStatusJSON.class)
        .setTarget(settings.getDELA_TRANSFER_HTTP_ENDPOINT())
        .setPath("/library/extended")
        .setPayload(torrentId);
      TorrentExtendedStatusJSON result = rc.doPost();
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.DELA, null, ise.getMessage(), ise);
    }
  }

  /**
   * @return <upldDS, dwnlDS>
   */
  public Pair<List<String>, List<String>> getContents() throws DelaException {
    if(!delaStateController.transferDelaAvailable()) {
      throw new DelaException(RESTCodes.DelaErrorCode.DELA_TRANSFER_NOT_AVAILABLE, Level.SEVERE,
        DelaException.Source.LOCAL);
    }
    HopsContentsSummaryJSON.Contents contents = TransferDelaController.this.getContents(new LinkedList<>());
    List<String> upldDSIds = new LinkedList<>();
    List<String> dwnlDSIds = new LinkedList<>();
    for (ElementSummaryJSON[] ea : contents.getContents().values()) {
      for (ElementSummaryJSON e : ea) {
        if (e.getTorrentStatus().toLowerCase().equals("uploading")) {
          upldDSIds.add(e.getTorrentId().getVal());
        } else if (e.getTorrentStatus().toLowerCase().equals("downloading")) {
          dwnlDSIds.add(e.getTorrentId().getVal());
        }
      }
    }
    return Pair.with(upldDSIds, dwnlDSIds);
  }
}
