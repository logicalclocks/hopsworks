/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.kube.project;

import com.google.common.collect.ImmutableMap;
import io.hops.hopsworks.common.util.IoUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeProjectConfigMaps {
  
  private final String SEPARATOR = "-";
  private final String HADOOP_CONF = "hadoopconf";
  private final String FLINK = "flink";
  private final String SPARK = "spark";
  private final String PROJECT_TEAMS = "project-teams";
  private final String HADOOP_CONF_SUFFIX = SEPARATOR + HADOOP_CONF;
  private final String SPARK_SUFFIX = SEPARATOR + SPARK;
  private final String FLINK_SUFFIX = SEPARATOR + FLINK;
  private final String PROJECT_TEAMS_SUFFIX = SEPARATOR + PROJECT_TEAMS;
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private Settings settings;
  
  public void createConfigMaps(Project project) throws IOException{
    createServiceConfigMaps(project);
    createProjectConfigMaps(project);
  }
  
  public void deleteConfigMaps(Project project){
    kubeClientService.deleteConfigMap(kubeClientService.getKubeProjectName(project),
        getHadoopConfigMapName(project));
    kubeClientService.deleteConfigMap(kubeClientService.getKubeProjectName(project),
        getSparkConfigMapName(project));
    kubeClientService.deleteConfigMap(kubeClientService.getKubeProjectName(project),
        getFlinkConfigMapName(project));
    kubeClientService.deleteConfigMap(kubeClientService.getKubeProjectName(project),
        getProjectTeamsConfigMapName(project));
  }
  
  public void reloadConfigMaps(Project project) throws IOException {
    //TODO: check for some condition to avoid continuous reload
    createServiceConfigMaps(project);
  }
  
  public String getHadoopConfigMapName(Project project){
    return kubeClientService.getKubeProjectName(project) + HADOOP_CONF_SUFFIX;
  }
  
  public String getSparkConfigMapName(Project project){
    return kubeClientService.getKubeProjectName(project) + SPARK_SUFFIX;
  }
  
  public String getFlinkConfigMapName(Project project){
    return kubeClientService.getKubeProjectName(project) + FLINK_SUFFIX;
  }
  
  public String getProjectTeamsConfigMapName(Project project){
    return kubeClientService.getKubeProjectName(project) + PROJECT_TEAMS_SUFFIX;
  }
  
  private void createServiceConfigMaps(Project project) throws IOException {
    // create config maps that are reloaded periodically
    createHadoopConfigMap(project);
    createSparkConfigMap(project);
    createFlinkConfigMap(project);
  }
  
  private void createProjectConfigMaps(Project project) throws IOException {
    // create config maps that are only removed when the project is deleted
    createProjectTeamsConfigMap(project);
  }
  
  private void createHadoopConfigMap(Project project) throws IOException {
    List<String> confFiles = Arrays.asList("core-site.xml", "hdfs-site.xml",
        "log4j.properties", "hadoop-env.sh");
    createConfigMap(project, settings.getHadoopConfDir(), confFiles,
        HADOOP_CONF_SUFFIX);
  }
  
  private void createSparkConfigMap(Project project) throws IOException {
    List<String> confFiles = Arrays.asList("hive-site.xml", "log4j.properties",
        "metrics.properties", "spark-blacklisted-properties.txt", "spark-defaults.conf",
        "spark-env.sh");
    createConfigMap(project, settings.getSparkConfDir(), confFiles,
        SPARK_SUFFIX);
  }
  
  private void createFlinkConfigMap(Project project) throws IOException {
    List<String> confFiles = Arrays.asList("flink-conf.yaml", "log4j.properties",
        "logback-yarn.xml", "logback.xml", "sql-client-defaults.yaml", "zoo.cfg");
    createConfigMap(project, settings.getFlinkConfDir(), confFiles,
        FLINK_SUFFIX);
  }
  
  private void createProjectTeamsConfigMap(Project project) {
    kubeClientService.createOrUpdateConfigMap(project, PROJECT_TEAMS_SUFFIX,
      ImmutableMap.of(project.getOwner().getUsername(), ProjectRoleTypes.DATA_OWNER.getRole()));
  }
  
  private void createConfigMap(Project project, String confDir, List<String> confFiles, String suffix)
      throws IOException {
    
    Map<String, String> fileNamesToContent = new HashMap<>();
    for(String file : confFiles){
      fileNamesToContent.put(file,
          IoUtils.readContentFromPath(new File(confDir, file)));
    }
    
    kubeClientService.createOrUpdateConfigMap(project, suffix, fileNamesToContent);
  }
  
}