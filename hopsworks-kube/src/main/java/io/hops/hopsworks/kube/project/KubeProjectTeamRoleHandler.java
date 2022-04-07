/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.project;

import io.hops.hopsworks.common.project.ProjectTeamRoleHandler;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.serving.utils.KubeServingUtils;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.INFO;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeProjectTeamRoleHandler implements ProjectTeamRoleHandler {
  
  private static final Logger logger = Logger.getLogger(KubeProjectTeamRoleHandler.class.getName());
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  @EJB
  private KubeServingUtils kubeServingUtils;
  
  @Override
  public void addMembers(Project project, List<Users> members, ProjectRoleTypes teamRole, boolean serviceUsers) {
    if (serviceUsers) { return; /* ignore service users */ }
    update(project, members, teamRole);
  }
  
  @Override
  public void updateMembers(Project project, List<Users> members, ProjectRoleTypes teamRole) {
    update(project, members, teamRole);
  }
  
  @Override
  public void removeMembers(Project project, List<Users> members) {
    if (members.size() == 0) return; // noop
    
    Map<String, String> userRoleMap = members.stream().collect(HashMap::new, (m,u)-> m.put(u.getUsername(), null),
      HashMap::putAll);
    logger.log(INFO, "Removing members of project " + project.getName() + ": " + String.join(", ",
      userRoleMap.keySet()));
    patch(project, userRoleMap);
  }
  
  private void update(Project project, List<Users> members, ProjectRoleTypes teamRole) {
    if (members.size() == 0) return; // noop
    if (teamRole == ProjectRoleTypes.UNDER_REMOVAL) {
      // if cleaning up, remove roles
      removeMembers(project, members);
      return;
    }
  
    Map<String, String> userRoleMap = members.stream().collect(Collectors.toMap(Users::getUsername,
      t -> teamRole.getRole()));
    logger.log(INFO, "Updating members to project " + project.getName() + ": " + String.join(", ",
      userRoleMap.keySet()));
    patch(project, userRoleMap);
  }
  
  private void patch(Project project, Map<String, String> userRoleMap) {
    Map<String, String> labels = kubeServingUtils.getServingScopeLabels(true);
    kubeClientService.patchConfigMap(kubeClientService.getKubeProjectName(project),
      kubeProjectConfigMaps.getProjectTeamsConfigMapName(project), userRoleMap, labels);
  }
  
  @Override
  public String getClassName() { return KubeProjectTeamRoleHandler.class.getName(); }
}
