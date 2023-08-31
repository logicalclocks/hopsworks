/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.tag;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Optional;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupTagResource extends FeatureStoreTagResource {

  private Featuregroup featureGroup;

  /**
   * Sets the feature group of the tag resource
   *
   * @param featureGroup
   */
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  @Override
  protected Optional<FeatureStoreTag> getTag(String name) throws FeatureStoreMetadataException {
    return tagController.getTag(featureGroup, name);
  }
  
  @Override
  protected Map<String, FeatureStoreTag> getTags() {
    return tagController.getTags(featureGroup);
  }
  
  @Override
  protected AttachMetadataResult<FeatureStoreTag> putTag(String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    return tagController.upsertTag(featureGroup, name, value);
  }
  
  @Override
  protected AttachMetadataResult<FeatureStoreTag> putTags(Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException {
    return tagController.upsertTags(featureGroup, tags);
  }
  
  @Override
  protected void deleteTag(String name) throws FeatureStoreMetadataException, FeaturestoreException {
    tagController.deleteTag(featureGroup, name);
  }
  
  @Override
  protected void deleteTags() throws FeaturestoreException {
    tagController.deleteTags(featureGroup);
  }
  
  @Override
  protected TagsDTO buildPutTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException {
    return tagBuilder.build(uriInfo, request, project.getId(), featureStore.getId(),
      ResourceRequest.Name.FEATUREGROUPS, featureGroup.getId(), tags);
  }
  
  @Override
  protected TagsDTO buildGetTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException {
    return tagBuilder.build(uriInfo, request, project.getId(), featureStore.getId(),
      ResourceRequest.Name.FEATUREGROUPS, featureGroup.getId(), tags);
  }
}
