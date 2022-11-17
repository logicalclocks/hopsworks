/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.TagStatus;
import software.amazon.awssdk.services.ecr.paginators.ListImagesIterable;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ECRClientService {
  
  private static final Logger LOG =
      Logger.getLogger(ECRClientService.class.getName());
  
  private EcrClient client = null;
  
  @PostConstruct
  private void initClient() {
    client = EcrClient.create();
  }
  
  public List<String> deleteImagesWithTagPrefix(final String repositoryName,
      final String imageTagPrefix) {
    LOG.info("Delete images with tags prefix " + imageTagPrefix + " from " +
        "repo" + repositoryName);
    ListImagesIterable imagesIterable =
        client.listImagesPaginator(builder ->
            builder.repositoryName(repositoryName)
                              .filter(filterBuilder ->
                                  filterBuilder.tagStatus(TagStatus.TAGGED)));
    
    List<ImageIdentifier> filteredImages =
        imagesIterable.stream().flatMap(listImagesResponse ->
            listImagesResponse.imageIds().stream().filter(imageIdentifier ->
                imageIdentifier.imageTag().startsWith(imageTagPrefix))).collect(Collectors.toList());
    
    LOG.info("Delete filtered images \n " + filteredImages);
    
    if(!filteredImages.isEmpty()){
      client.batchDeleteImage(builder -> builder.repositoryName(repositoryName)
          .imageIds(filteredImages));
    }
    
    List<String> filteredTags =
        filteredImages.stream().map(ImageIdentifier::imageTag)
            .collect(Collectors.toList());
    
    return filteredTags;
  }
  
  public List<String> getImageTags(final String repositoryName, String filter) {
    ListImagesIterable imagesIterable
            = client.listImagesPaginator(builder
              -> builder.repositoryName(repositoryName)
                    .filter(filterBuilder
                      -> filterBuilder.tagStatus(TagStatus.TAGGED)));

    List<ImageIdentifier> filteredImages
            = imagesIterable.stream().flatMap(listImagesResponse
              -> listImagesResponse.imageIds().stream().filter(imageIdentifier -> {
                  return imageIdentifier.imageTag().contains(filter);
              }
            )).collect(Collectors.toList());

    List<String> filteredTags
            = filteredImages.stream().map(ImageIdentifier::imageTag)
                    .collect(Collectors.toList());

    return filteredTags;
  }
}
