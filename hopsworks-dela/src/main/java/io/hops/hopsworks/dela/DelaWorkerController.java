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

package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksTransferDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
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
import javax.ws.rs.core.Response;
import org.apache.hadoop.fs.Path;
import org.json.JSONException;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaWorkerController {

  private Logger LOG = Logger.getLogger(DelaWorkerController.class.getName());

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

  public String shareDatasetWithHops(Project project, Dataset dataset, Users user) throws ThirdPartyException {

    if (dataset.isPublicDs()) {
      return dataset.getPublicDsId();
    }
    if (dataset.isShared()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(),
        "dataset shared - only owner can publish", ThirdPartyException.Source.LOCAL, "bad request");
    }
    delaStateCtrl.checkDelaAvailable();
    delaHdfsCtrl.writeManifest(project, dataset, user);

    long datasetSize = delaHdfsCtrl.datasetSize(project, dataset, user);
    String publicDSId;
    try {
      publicDSId = hopsSiteCtrl.performAsUser(user, new HopsSite.UserFunc<String>() {
        @Override
        public String perform() throws ThirdPartyException {
          return hopsSiteCtrl.publish(dataset.getName(), dataset.getDescription(), getCategories(), 
            datasetSize, user.getEmail());
          
        }
      });
      delaCtrlUpload(project, dataset, user, publicDSId);
    } catch (ThirdPartyException tpe) {
      if (ThirdPartyException.Source.HOPS_SITE.equals(tpe.getSource())
        && ThirdPartyException.Error.DATASET_EXISTS.is(tpe.getMessage())) {
        //TODO ask dela to checksum it;
      }
      throw tpe;
    }
    delaDatasetCtrl.uploadToHops(dataset, publicDSId);
    LOG.log(Level.INFO, "{0} shared with hops", publicDSId);
    return publicDSId;
  }

  private void delaCtrlUpload(Project project, Dataset dataset, Users user, String publicDSId)
    throws ThirdPartyException {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    HDFSResource resource = new HDFSResource(datasetPath.toString(), Settings.MANIFEST_FILE);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath().toString(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(dataset.getName(), project.getId(), dataset.getId());
    delaCtrl.upload(publicDSId, details, resource, endpoint);
  }

  public void unshareFromHops(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    if (!dataset.isPublicDs()) {
      return;
    }
    delaStateCtrl.checkDelaAvailable();
    delaCtrl.cancel(dataset.getPublicDsId());
    hopsSiteCtrl.cancel(dataset.getPublicDsId());
    delaHdfsCtrl.deleteManifest(project, dataset, user);
    delaDatasetCtrl.unshareFromHops(dataset);
  }

  public void unshareFromHopsAndClean(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    unshareFromHops(project, dataset, user);
    delaDatasetCtrl.delete(project, dataset);
  }
  
  public ManifestJSON startDownload(Project project, Users user, HopsworksTransferDTO.Download downloadDTO)
    throws ThirdPartyException {
    delaStateCtrl.checkDelaAvailable();
    Dataset dataset = delaDatasetCtrl.download(project, user, downloadDTO.getPublicDSId(), downloadDTO.getName());

    try {
      delaCtrlStartDownload(project, dataset, user, downloadDTO);
    } catch (ThirdPartyException tpe) {
      delaDatasetCtrl.delete(project, dataset);
      throw tpe;
    }

    ManifestJSON manifest = delaHdfsCtrl.readManifest(project, dataset, user);
    delaDatasetCtrl.updateDescription(dataset, manifest.getDatasetDescription());
    return manifest;
  }

  private void delaCtrlStartDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO) throws ThirdPartyException {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    HDFSResource resource = new HDFSResource(datasetPath.toString(), Settings.MANIFEST_FILE);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    HDFSEndpoint endpoint = new HDFSEndpoint(getHDFSXmlPath().toString(), hdfsUser);
    HopsDatasetDetailsDTO details = new HopsDatasetDetailsDTO(downloadDTO.getName(), project.getId(), dataset.getId());
    delaCtrl.startDownload(downloadDTO.getPublicDSId(), details, resource, endpoint, downloadDTO.getBootstrap());
  }

  public void advanceDownload(Project project, Dataset dataset, Users user, HopsworksTransferDTO.Download downloadDTO,
    String sessionId, KafkaEndpoint kafkaEndpoint) throws ThirdPartyException {

    delaStateCtrl.checkDelaAvailable();
    delaCtrlAdvanceDownload(project, dataset, user, downloadDTO, sessionId, kafkaEndpoint);
    hopsSiteCtrl.download(downloadDTO.getPublicDSId());
  }

  private void delaCtrlAdvanceDownload(Project project, Dataset dataset, Users user,
    HopsworksTransferDTO.Download downloadDTO, String sessionId, KafkaEndpoint kafkaEndpoint)
    throws ThirdPartyException {
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
