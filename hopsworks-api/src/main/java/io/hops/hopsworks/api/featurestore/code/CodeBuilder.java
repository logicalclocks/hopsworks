/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.api.featurestore.code;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.code.CodeController;
import io.hops.hopsworks.common.featurestore.code.FeaturestoreCodeFacade;
import io.hops.hopsworks.common.jupyter.NotebookConversion;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CodeBuilder {

  @EJB
  private FeaturestoreCodeFacade codeFacade;
  @EJB
  private CodeController codeController;

  private UriBuilder uri(UriInfo uriInfo, Project project, Featurestore featurestore) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()));
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, Featuregroup featuregroup) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.CODE.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore,
                  Featuregroup featuregroup, FeaturestoreCode featurestoreCode) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.CODE.toString().toLowerCase())
        .path(Integer.toString(featurestoreCode.getId()))
        .queryParam("fields", "content")
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project,
                  Featurestore featurestore, TrainingDataset trainingDataset) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.CODE.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore,
                  TrainingDataset trainingDataset, FeaturestoreCode featurestoreCode) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.CODE.toString().toLowerCase())
        .path(Integer.toString(featurestoreCode.getId()))
        .queryParam("fields", "content")
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.CODE);
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user,
                       Featuregroup featuregroup,
                       FeaturestoreCode featurestoreCode,
                       NotebookConversion format) throws FeaturestoreException, ServiceException {
    Path fullCodePath = new Path(codeController.getCodeDirFullPath(project, featuregroup),
        featurestoreCode.getFileName());
    CodeDTO dto = new CodeDTO();
    dto.setCodeId(featurestoreCode.getId());
    dto.setApplicationId(featurestoreCode.getApplicationID());
    dto.setHref(uri(uriInfo, project, featuregroup.getFeaturestore(), featuregroup, featurestoreCode));
    dto.setPath(fullCodePath.toString());
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setCommitTime(featurestoreCode.getCommitTime().getTime());
      if (featurestoreCode.getFeatureGroupCommit() != null) {
        dto.setFeatureGroupCommitId(
            featurestoreCode.getFeatureGroupCommit().getFeatureGroupCommitPK().getCommitId());
      }
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        dto.setContentFormat(codeController.getContentFormat(fullCodePath.toString()));
        dto.setContent(codeController.readContent(project, user, fullCodePath.toString(), dto.getContentFormat(),
            format));
      }
    }

    return dto;
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user,
                       TrainingDataset trainingDataset,
                       FeaturestoreCode featurestoreCode,
                       NotebookConversion format) throws FeaturestoreException, ServiceException {
    Path fullCodePath = new Path(codeController.getCodeDirFullPath(project, trainingDataset),
        featurestoreCode.getFileName());
    CodeDTO dto = new CodeDTO();
    dto.setCodeId(featurestoreCode.getId());
    dto.setApplicationId(featurestoreCode.getApplicationID());
    dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset, featurestoreCode));
    dto.setPath(fullCodePath.toString());
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setCommitTime(featurestoreCode.getCommitTime().getTime());
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        dto.setContentFormat(codeController.getContentFormat(fullCodePath.toString()));
        dto.setContent(codeController.readContent(project, user, fullCodePath.toString(), dto.getContentFormat(),
            format));
      }
    }

    return dto;
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user, Featurestore featurestore, Featuregroup featuregroup,
                       NotebookConversion format)
      throws ServiceException, FeaturestoreException {
    CodeDTO dto = new CodeDTO();
    dto.setHref(uri(uriInfo, project, featurestore, featuregroup));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo<FeaturestoreCode> collectionInfo = codeFacade.findByFeaturegroup(
          resourceRequest.getOffset(),
          resourceRequest.getLimit(),
          resourceRequest.getSort(),
          resourceRequest.getFilter(),
          featuregroup);
      dto.setCount(collectionInfo.getCount());

      for (FeaturestoreCode s : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, featuregroup, s, format));
      }
    }

    return dto;
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user, Featurestore featurestore, TrainingDataset trainingDataset,
                       NotebookConversion format)
      throws ServiceException, FeaturestoreException {
    CodeDTO dto = new CodeDTO();
    dto.setHref(uri(uriInfo, project, featurestore, trainingDataset));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo<FeaturestoreCode> collectionInfo = codeFacade.findByTrainingDataset(
          resourceRequest.getOffset(),
          resourceRequest.getLimit(),
          resourceRequest.getSort(),
          resourceRequest.getFilter(),
          trainingDataset);
      dto.setCount(collectionInfo.getCount());

      for (FeaturestoreCode s : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, trainingDataset, s, format));
      }
    }

    return dto;
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user, Featuregroup featuregroup,
                       Integer codeId,
                       NotebookConversion format)
      throws FeaturestoreException, ServiceException {
    FeaturestoreCode featurestoreCode = codeFacade.findFeaturestoreCodeById(featuregroup, codeId)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_NOT_FOUND, Level.FINE));

    return build(uriInfo, resourceRequest, project, user, featuregroup, featurestoreCode, format);
  }

  public CodeDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Project project, Users user, TrainingDataset trainingDataset,
                       Integer codeId,
                       NotebookConversion format)
      throws FeaturestoreException, ServiceException {
    FeaturestoreCode featurestoreCode = codeFacade.findFeaturestoreCodeById(trainingDataset, codeId)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_NOT_FOUND, Level.FINE));

    return build(uriInfo, resourceRequest, project, user, trainingDataset, featurestoreCode, format);
  }
}
