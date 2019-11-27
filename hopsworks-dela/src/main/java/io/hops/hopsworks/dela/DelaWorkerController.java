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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksTransferDTO;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.dela.old_dto.ExtendedDetails;
import io.hops.hopsworks.dela.old_dto.HDFSEndpoint;
import io.hops.hopsworks.dela.old_dto.HDFSResource;
import io.hops.hopsworks.dela.old_dto.HdfsDetails;
import io.hops.hopsworks.dela.old_dto.HopsDatasetDetailsDTO;
import io.hops.hopsworks.dela.old_dto.KafkaDetails;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.KafkaResource;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.hadoop.fs.Path;
import org.json.JSONException;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaWorkerController {

  private final Logger LOGGER = Logger.getLogger(DelaWorkerController.class.getName());

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtrl;
  @EJB
  private TransferDelaController delaCtrl;
  @EJB
  private HopssiteController hopsSiteCtrl;
  @EJB
  private DelaDatasetController delaDatasetCtrl;
  @EJB
  private DelaHdfsController delaHdfsCtrl;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  public String shareDatasetWithHops(Project project, Dataset dataset, Users user) throws DelaException {

    if (dataset.isPublicDs()) {
      return dataset.getPublicDsId();
    }
    if (dataset.isShared(project)) {
      throw new DelaException(RESTCodes.DelaErrorCode.DATASET_PUBLISH_PERMISSION_ERROR, Level.WARNING,
        DelaException.Source.LOCAL);
    }
    delaStateCtrl.checkDelaAvailable();
    delaHdfsCtrl.writeManifest(project, dataset, user);

    long datasetSize = delaHdfsCtrl.datasetSize(project, dataset, user);
    String publicDSId;
    try {
      publicDSId = hopsSiteCtrl.performAsUser(user, new HopsSite.UserFunc<String>() {
        @Override
        public String perform() throws DelaException {
          return hopsSiteCtrl.publish(dataset.getName(), dataset.getDescription(), getCategories(), 
            datasetSize, user.getEmail());
          
        }
      });
      delaCtrlUpload(project, dataset, user, publicDSId);
    } catch (DelaException tpe) {
      if (DelaException.Source.HOPS_SITE.equals(tpe.getSource())
        && RESTCodes.DelaErrorCode.DATASET_EXISTS.equals(tpe.getMessage())) {
        //TODO ask dela to checksum it;
      }
      throw tpe;
    }
    delaDatasetCtrl.uploadToHops(project, dataset, publicDSId);
    LOGGER.log(Level.INFO, "{0} shared with hops", publicDSId);
    return publicDSId;
  }

  private void delaCtrlUpload(Project project, Dataset dataset, Users user, String publicDSId)
    throws DelaException {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    HDFSResource resource = new HDFSResource(datasetPath.toString(), Settings.MANIFEST_FILE);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath().toString(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(dataset.getName(), project.getId(), dataset.getId());
    delaCtrl.upload(publicDSId, details, resource, endpoint);
  }

  public void unshareFromHops(Project project, Dataset dataset, Users user) throws DelaException {
    if (!dataset.isPublicDs()) {
      return;
    }
    delaStateCtrl.checkDelaAvailable();
    delaCtrl.cancel(dataset.getPublicDsId());
    hopsSiteCtrl.cancel(dataset.getPublicDsId());
    delaHdfsCtrl.deleteManifest(project, dataset, user);
    delaDatasetCtrl.unshareFromHops(project, dataset);
  }

  public void unshareFromHopsAndClean(Project project, Dataset dataset, Users user) throws DelaException,
      DatasetException {
    unshareFromHops(project, dataset, user);
    delaDatasetCtrl.delete(project, dataset);
  }
  
  public ManifestJSON startDownload(Project project, Users user, HopsworksTransferDTO.Download downloadDTO)
    throws DelaException, DatasetException {
    delaStateCtrl.checkDelaAvailable();
    Dataset dataset = delaDatasetCtrl.download(project, user, downloadDTO.getPublicDSId(), downloadDTO.getName());

    try {
      delaCtrlStartDownload(project, dataset, user, downloadDTO);
    } catch (DelaException tpe) {
      delaDatasetCtrl.delete(project, dataset);
      throw tpe;
    }

    ManifestJSON manifest = delaHdfsCtrl.readManifest(project, dataset, user);
    delaDatasetCtrl.updateDescription(project, dataset, manifest.getDatasetDescription());
    return manifest;
  }

  private void delaCtrlStartDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO) throws DelaException {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    HDFSResource resource = new HDFSResource(datasetPath.toString(), Settings.MANIFEST_FILE);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath().toString(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(downloadDTO.getName(), project.getId(), dataset.getId());
    delaCtrl.startDownload(downloadDTO.getPublicDSId(), details, resource, endpoint, downloadDTO.getBootstrap());
  }

  public void advanceDownload(Project project, Dataset dataset, Users user, HopsworksTransferDTO.Download downloadDTO,
    String sessionId, KafkaEndpoint kafkaEndpoint) throws DelaException {

    delaStateCtrl.checkDelaAvailable();
    delaCtrlAdvanceDownload(project, dataset, user, downloadDTO, sessionId, kafkaEndpoint);
    hopsSiteCtrl.download(downloadDTO.getPublicDSId());
  }

  private void delaCtrlAdvanceDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO, String sessionId, KafkaEndpoint kafkaEndpoint)
    throws DelaException {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    JSONObject fileTopics = new JSONObject(downloadDTO.getTopics());
    LinkedList<HdfsDetails> hdfsResources = new LinkedList<>();
    LinkedList<KafkaDetails> kafkaResources = new LinkedList<>();

    Iterator<String> iter = fileTopics.keys();
    while (iter.hasNext()) {
      String key = iter.next();
      try {
        String value = (String) fileTopics.get(key);
        if (!value.equals("") && kafkaEndpoint != null) {
          kafkaResources.add(new KafkaDetails(key, new KafkaResource(sessionId, value)));
        }
        hdfsResources.add(new HdfsDetails(key, new HDFSResource(datasetPath.toString(), key)));
      } catch (JSONException e) {
        // Something went wrong!
      }
    }
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint hdfsEndpoint = new HDFSEndpoint(getHDFSXmlPath().toString(), hdfsUser);
    ExtendedDetails details = new ExtendedDetails(hdfsResources, kafkaResources);
    delaCtrl.advanceDownload(downloadDTO.getPublicDSId(), hdfsEndpoint, kafkaEndpoint, details);
  }

  //********************************************************************************************************************

  private Collection<String> getCategories() {
    Set<String> categories = new HashSet<>();
    return categories;
  }

  private Path getHDFSXmlPath() {
    return new Path(settings.getHadoopConfDir(), Settings.DEFAULT_HADOOP_CONFFILE_NAME);
  }
}
