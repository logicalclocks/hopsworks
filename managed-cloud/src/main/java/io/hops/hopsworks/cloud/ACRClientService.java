/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ACRClientService {
  private static final Logger LOG =
      Logger.getLogger(ACRClientService.class.getName());
  
  @EJB
  Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private ProjectUtils projectUtils;
  
  @PostConstruct
  private void initClient() {
  }
  
  public List<String> deleteImagesWithTagPrefix(final String repositoryName,
      final String imageTagPrefix) throws ServiceDiscoveryException {
    
    String registry = projectUtils.getRegistryAddress();
    
    String prog = settings.getSudoersDir() + "/dockerImage.sh";
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand(prog)
        .addCommand("delete-acr")
        .addCommand(registry)
        .addCommand(repositoryName)
        .addCommand(imageTagPrefix)
        .redirectErrorStream(true)
        .setWaitTimeout(1, TimeUnit.MINUTES)
        .build();
    
    try {
      ProcessResult processResult =
          osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        LOG.info("Could not delete docker images in " + repositoryName +
            " under prefix " + imageTagPrefix + ". Exit code: " +
            processResult.getExitCode() + " out: " + processResult.getStdout());
        return new ArrayList<>();
      }
      return Arrays.asList(processResult.getStdout().split("\n"));
    } catch (IOException ex) {
      LOG.info("Could not delete docker images in " + repositoryName +
          " under prefix " + imageTagPrefix + ". Exception caught: " +
          ex.getMessage());
      return new ArrayList<>();
    }
  }
  
  public List<String> getImageTags(final String repositoryName, String filter) throws IOException, 
          ServiceDiscoveryException {

    String registry = projectUtils.getRegistryAddress();
    
    String prog = settings.getSudoersDir() + "/dockerImage.sh";
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
            .addCommand("/usr/bin/sudo")
            .addCommand(prog)
            .addCommand("list-tags-acr")
            .addCommand(registry)
            .addCommand(repositoryName)
            .addCommand(filter)
            .redirectErrorStream(true)
            .setWaitTimeout(1, TimeUnit.MINUTES)
            .build();

    ProcessResult processResult
            = osProcessExecutor.execute(processDescriptor);
    if (processResult.getExitCode() != 0) {
      throw new IOException("Failed to get the images tags from the repositor");
    }
    return Arrays.asList(processResult.getStdout().split("\n"));
  }
}
