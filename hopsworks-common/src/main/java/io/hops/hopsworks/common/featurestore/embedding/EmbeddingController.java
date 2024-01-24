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

package io.hops.hopsworks.common.featurestore.embedding;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.hops.hopsworks.common.featurestore.featuregroup.EmbeddingDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Embedding;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.EmbeddingFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.vectordb.Index;
import io.hops.hopsworks.vectordb.VectorDatabaseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EmbeddingController {

  @EJB
  private Settings settings;
  @EJB
  private VectorDatabaseClient vectorDatabaseClient;
  @EJB
  private FeaturegroupController featuregroupController;

  public void createVectorDbIndex(Project project, Featuregroup featureGroup)
      throws FeaturestoreException {
    Index index = new Index(featureGroup.getEmbedding().getVectorDbIndexName());
    try {
      vectorDatabaseClient.getClient().createIndex(index, createIndex(featureGroup.getEmbedding().getColPrefix(),
          featureGroup.getEmbedding().getEmbeddingFeatures()), true);
      if (isDefaultVectorDbIndex(project, index.getName())) {
        vectorDatabaseClient.getClient().addFields(index, createMapping(featureGroup.getEmbedding().getColPrefix(),
            featureGroup.getEmbedding().getEmbeddingFeatures()));
      }
    } catch (VectorDatabaseException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATUREGROUP,
          Level.FINE, "Cannot create opensearch vectordb index: " + index.getName());
    }
  }

  public Embedding getEmbedding(Project project, EmbeddingDTO embeddingDTO, Featuregroup featuregroup)
      throws FeaturestoreException {
    Embedding embedding = new Embedding();
    embedding.setFeaturegroup(featuregroup);
    if (Strings.isNullOrEmpty(embeddingDTO.getIndexName())) {
      embedding.setVectorDbIndexName(getDefaultVectorDbIndex(project));
      embedding.setColPrefix(getVectorDbColPrefix(featuregroup));
    } else {
      String vectorDbIndexPrefix = getVectorDbIndexPrefix(project);
      // In hopsworks opensearch, users can only access indexes which start with specific prefix
      if (!embeddingDTO.getIndexName().startsWith(vectorDbIndexPrefix)) {
        embedding.setVectorDbIndexName(
            vectorDbIndexPrefix + "_" + embeddingDTO.getIndexName());
        embedding.setColPrefix("");
      }
      if (isDefaultVectorDbIndex(project, embeddingDTO.getIndexName())) {
        embedding.setColPrefix(getVectorDbColPrefix(featuregroup));
      }
    }
    embedding.setEmbeddingFeatures(
        embeddingDTO.getFeatures()
            .stream()
            .map(mapping -> new EmbeddingFeature(embedding, mapping.getName(), mapping.getDimension(),
                mapping.getSimilarityFunctionType()))
            .collect(Collectors.toList())
    );
    return embedding;
  }

  public void dropEmbeddingForProject(Project project)
      throws FeaturestoreException {
    try {
      for (Index index: vectorDatabaseClient.getClient().getAllIndices().stream()
          .filter(index -> index.getName().startsWith(getVectorDbIndexPrefix(project))).collect(Collectors.toSet())) {
        vectorDatabaseClient.getClient().deleteIndex(index);
      }
    } catch (VectorDatabaseException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_VECTOR_DB_INDEX,
          Level.FINE, "Cannot delete index from vectordb for project: " + project.getName());
    }
  }

  public void dropEmbedding(Project project, Featuregroup featureGroup)
      throws FeaturestoreException {
    Index index = new Index(featureGroup.getEmbedding().getVectorDbIndexName());
    try {
      // If it is a project index, remove only the documents and keep the index.
      if (isDefaultVectorDbIndex(project, featureGroup.getEmbedding().getVectorDbIndexName())) {
        removeDocuments(featureGroup);
      } else {
        // If it is a previous project index, remove only the documents and keep the index.
        if (isPreviousDefaultVectorDbIndex(featureGroup.getEmbedding())) {
          removeDocuments(featureGroup);
        } else {
          vectorDatabaseClient.getClient().deleteIndex(index);
        }
      }
    } catch (VectorDatabaseException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_FEATUREGROUP,
          Level.FINE, "Cannot delete documents from vectordb for feature group: " +
          featureGroup.getName(), e.getMessage(), e);
    }
  }

  private boolean isPreviousDefaultVectorDbIndex(Embedding embedding)
      throws FeaturestoreException, VectorDatabaseException {
    // if column prefix is null or empty OR if all column starts with the column prefix, then it is an individual index.
    return !(Strings.isNullOrEmpty(embedding.getColPrefix()) ||
        vectorDatabaseClient.getClient().getSchema(new Index(embedding.getVectorDbIndexName()))
            .stream().allMatch(field -> field.getName().startsWith(embedding.getColPrefix())));
  }

  private void removeDocuments(Featuregroup featureGroup) throws FeaturestoreException, VectorDatabaseException {
    EmbeddingFeature feature = featureGroup.getEmbedding().getEmbeddingFeatures().stream().findFirst().get();
    String matchQuery = "%s:*";

    String field = feature.getEmbedding().getColPrefix() == null
        ? feature.getName()
        : feature.getEmbedding().getColPrefix() + feature.getName();
    vectorDatabaseClient.getClient().deleteByQuery(
        new Index(featureGroup.getEmbedding().getVectorDbIndexName()),
        String.format(matchQuery, field)
    );
  }

  protected String createMapping(String prefix, Collection<EmbeddingFeature> features) {
    String mappingString = "{\n" +
        "    \"properties\": {\n" +
        "%s\n" +
        "    }\n" +
        "  }";
    String fieldString = "        \"%s\": {\n" +
        "          \"type\": \"knn_vector\",\n" +
        "          \"dimension\": %d\n" +
        "        }";
    List<String> fieldMapping = Lists.newArrayList();
    for (EmbeddingFeature feature : features) {
      fieldMapping.add(String.format(
          fieldString, prefix + feature.getName(), feature.getDimension()));
    }
    return String.format(mappingString, String.join(",\n", fieldMapping));
  }

  protected String createIndex(String prefix, Collection<EmbeddingFeature> features) {
    String jsonString = "{\n" +
        "  \"settings\": {\n" +
        "    \"index\": {\n" +
        "      \"knn\": \"true\",\n" +
        "      \"knn.algo_param.ef_search\": 512\n" +
        "    }\n" +
        "  },\n" +
        "  \"mappings\": %s\n" +
        "}";
    return String.format(jsonString, createMapping(prefix, features));

  }

  private String getDefaultVectorDbIndex(Project project) throws FeaturestoreException {
    Set<String> indexName = getAllDefaultVectorDbIndex(project);
    // randomly select an index
    return indexName.stream().sorted(Comparator.comparingInt(i -> new Random().nextInt())).findFirst().get();
  }

  private boolean isDefaultVectorDbIndex(Project project, String index) throws FeaturestoreException {
    return getAllDefaultVectorDbIndex(project).contains(index);
  }

  private Set<String> getAllDefaultVectorDbIndex(Project project) throws FeaturestoreException {
    Set<String> indices;
    if (!Strings.isNullOrEmpty(settings.getOpensearchDefaultEmbeddingIndexName())) {
      indices = Arrays.stream(settings.getOpensearchDefaultEmbeddingIndexName().split(","))
          .collect(Collectors.toSet());
    } else {
      indices = Sets.newHashSet();
      for (int i = 0; i < settings.getOpensearchNumDefaultEmbeddingIndex(); i++) {
        indices.add(getVectorDbIndexPrefix(project) + "_default_project_embedding_" + i);
      }
    }
    if (indices.size() == 0) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.OPENSEARCH_DEFAULT_EMBEDDING_INDEX_SUFFIX_NOT_DEFINED, Level.FINE,
          "Default vector db index is not defined.");
    }
    return indices;
  }

  private String getVectorDbIndexPrefix(Project project) {
    return project.getId() + "__embedding";
  }

  private String getVectorDbColPrefix(Featuregroup featuregroup) {
    return Utils.getFeaturegroupName(featuregroup) + "_";
  }

}
