/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.transformationFunction;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionAttachedDTO;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionDTO;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TransformationFunctionBuilder {
  @EJB
  private TransformationFunctionController transformationFunctionController;
  @EJB
  private TransformationFunctionFacade transformationFunctionFacade;
  @EJB
  private TrainingDatasetController trainingDatasetController;

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.TRANSFORMATIONFUNCTIONS.toString().toLowerCase()).build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, TrainingDataset trainingDataset) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.TRANSFORMATIONFUNCTIONS.toString().toLowerCase()).build();
  }

  private URI uri(UriInfo uriInfo, Project project, FeatureView featureView) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featureView.getFeaturestore().getId()))
        .path(ResourceRequest.Name.FEATUREVIEW.toString().toLowerCase())
        .path(featureView.getName())
        .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
        .path(String.valueOf(featureView.getVersion()))
        .path(ResourceRequest.Name.TRANSFORMATION.toString().toLowerCase())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TRANSFORMATIONFUNCTIONS);
  }

  public TransformationFunctionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
                                         Project project, Featurestore featurestore,
                                         TransformationFunction transformationFunction) throws FeaturestoreException {
    TransformationFunctionDTO transformationFunctionDTO =
        new TransformationFunctionDTO();
    transformationFunctionDTO.setHref(uri(uriInfo, project, featurestore));
    transformationFunctionDTO.setExpand(expand(resourceRequest));
    if (transformationFunctionDTO.isExpand()) {
      transformationFunctionDTO.setId(transformationFunction.getId());
      transformationFunctionDTO.setName(transformationFunction.getName());
      transformationFunctionDTO.setOutputType(transformationFunction.getOutputType());
      transformationFunctionDTO.setVersion(transformationFunction.getVersion());
      transformationFunctionDTO.setFeaturestoreId(transformationFunction.getFeaturestore().getId());
      transformationFunctionDTO.setSourceCodeContent(transformationFunctionController.readContent(
          user, project, transformationFunction));
    }
    return transformationFunctionDTO;
  }

  public TransformationFunctionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project project,
                                         Featurestore featurestore, String name, Integer version)
      throws FeaturestoreException {

    Long counts;
    List<TransformationFunction> transformationFunctions;

    if (name != null){
      TransformationFunction transformationFunction;
      if (version != null){
        transformationFunction =
            transformationFunctionFacade.findByNameVersionAndFeaturestore(name, version, featurestore)
                .orElseThrow(() ->
                    new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_DOES_NOT_EXIST,
                Level.FINE, "Could not find transformation function with name " + name + " and version" + version));
      } else {
        transformationFunction =
            transformationFunctionFacade.findByNameVersionAndFeaturestore(name, 1, featurestore)
                .orElseThrow(() ->
                    new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_DOES_NOT_EXIST,
                Level.FINE, "Could not find transformation function with name " + name + " and version" + 1));
      }
      transformationFunctions = Arrays.asList(transformationFunction);
      counts = 1L;
    } else {
      AbstractFacade.CollectionInfo transformationFunction =
          transformationFunctionFacade.findByFeaturestore(
              resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(),
              resourceRequest.getSort(), featurestore);

      transformationFunctions = transformationFunction.getItems();
      counts = transformationFunction.getCount();
    }

    TransformationFunctionDTO transformationFunctionDTO = new TransformationFunctionDTO();
    transformationFunctionDTO.setHref(uri(uriInfo, project, featurestore));
    transformationFunctionDTO.setExpand(expand(resourceRequest));
    if (transformationFunctionDTO.isExpand()) {
      List<TransformationFunctionDTO> list = new ArrayList<>();
      for (TransformationFunction t : transformationFunctions) {
        TransformationFunctionDTO build = build(uriInfo, resourceRequest, user, project, featurestore, t);
        list.add(build);
      }
      transformationFunctionDTO.setItems(list);
      transformationFunctionDTO.setCount(counts);
    }
    return transformationFunctionDTO;
  }

  public TransformationFunctionAttachedDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
                                                  Project project, TrainingDataset trainingDataset,
                                                  TrainingDatasetFeature tdFeature) throws FeaturestoreException {
    return build(resourceRequest, user, project,
        uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset), tdFeature);
  }

  public TransformationFunctionAttachedDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
      Project project, FeatureView featureView,
      TrainingDatasetFeature tdFeature) throws FeaturestoreException {
    return build(resourceRequest, user, project,
        uri(uriInfo, project, featureView.getFeaturestore()), tdFeature);
  }

  private TransformationFunctionAttachedDTO build(ResourceRequest resourceRequest, Users user,
      Project project, URI uri, TrainingDatasetFeature tdFeature) throws FeaturestoreException {
    TransformationFunctionAttachedDTO  transformationFunctionAttachedDTO = new TransformationFunctionAttachedDTO();
    TransformationFunctionDTO transformationFunctionDTO =  new TransformationFunctionDTO(
        tdFeature.getTransformationFunction().getId(),
        tdFeature.getTransformationFunction().getName(),
        tdFeature.getTransformationFunction().getOutputType(),
        tdFeature.getTransformationFunction().getVersion(),
        transformationFunctionController.readContent(user, project, tdFeature.getTransformationFunction()),
        tdFeature.getTransformationFunction().getFeaturestore().getId());

    transformationFunctionAttachedDTO.setHref(uri);
    transformationFunctionAttachedDTO.setExpand(expand(resourceRequest));
    if (transformationFunctionAttachedDTO.isExpand()) {
      transformationFunctionAttachedDTO.setName(trainingDatasetController.checkPrefix(tdFeature));
      transformationFunctionAttachedDTO.setTransformationFunction(transformationFunctionDTO);
    }

    return transformationFunctionAttachedDTO;
  }

  public TransformationFunctionAttachedDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
                                                  Project project, TrainingDataset trainingDataset)
      throws FeaturestoreException {

    TransformationFunctionAttachedDTO  transformationFunctionAttachedDTO = new TransformationFunctionAttachedDTO();
    transformationFunctionAttachedDTO.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(),
        trainingDataset));
    transformationFunctionAttachedDTO.setExpand(expand(resourceRequest));
    if (transformationFunctionAttachedDTO.isExpand()) {
      List<TransformationFunctionAttachedDTO> list = new ArrayList<>();
      for (TrainingDatasetFeature tdFeature: trainingDataset.getFeatures()){
        if (tdFeature.getTransformationFunction() != null){
          TransformationFunctionAttachedDTO build = build(uriInfo, resourceRequest, user, project, trainingDataset,
              tdFeature);
          list.add(build);
        }
      }
      transformationFunctionAttachedDTO.setItems(list);
      transformationFunctionAttachedDTO.setCount((long)list.size());
    }

    return transformationFunctionAttachedDTO;
  }

  public TransformationFunctionAttachedDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
      Project project, FeatureView featureView)
      throws FeaturestoreException {

    TransformationFunctionAttachedDTO  transformationFunctionAttachedDTO = new TransformationFunctionAttachedDTO();
    transformationFunctionAttachedDTO.setHref(uri(uriInfo, project, featureView));
    transformationFunctionAttachedDTO.setExpand(expand(resourceRequest));
    if (transformationFunctionAttachedDTO.isExpand()) {
      List<TransformationFunctionAttachedDTO> list = new ArrayList<>();
      for (TrainingDatasetFeature tdFeature: featureView.getFeatures()){
        if (tdFeature.getTransformationFunction() != null){
          TransformationFunctionAttachedDTO build = build(uriInfo, resourceRequest, user, project, featureView,
              tdFeature);
          list.add(build);
        }
      }
      transformationFunctionAttachedDTO.setItems(list);
      transformationFunctionAttachedDTO.setCount((long)list.size());
    }

    return transformationFunctionAttachedDTO;
  }
}
