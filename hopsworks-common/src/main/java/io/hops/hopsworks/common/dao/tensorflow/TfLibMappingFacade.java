/*
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
 */

package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class TfLibMappingFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private Settings settings;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TfLibMappingFacade() { }

  public TfLibMapping findByTfVersion(String tfVersion) {
    try {
      return em.createNamedQuery("TfLibMapping.findByTfVersion", TfLibMapping.class)
          .setParameter("tfVersion", tfVersion)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public TfLibMapping findTfMappingForProject(Project project) {
    
    if (project.getCondaEnv() == false) {
      return findByTfVersion(settings.getTensorflowVersion());
    }
    
    
    CondaCommands command = pythonDepsFacade.getOngoingEnvCreation(project);

    if(command == null) {
      return project.getPythonDepCollection().stream()
          .filter(dep -> dep.getDependency().equals("tensorflow") || dep.getDependency().equals("tensorflow-gpu"))
          .findAny()
          .map(tfDep -> findByTfVersion(tfDep.getVersion()))
          .orElse(null);
    } else if(command.getOp().compareTo(PythonDepsFacade.CondaOp.CREATE) == 0) {
      return findByTfVersion(settings.getTensorflowVersion());
    } else if(command.getOp().compareTo(PythonDepsFacade.CondaOp.YML) == 0) {
      String envYml = command.getEnvironmentYml();

      Pattern tfCPUPattern = Pattern.compile("(tensorflow==\\d*.\\d*.\\d*)");
      Matcher tfCPUMatcher = tfCPUPattern.matcher(envYml);

      if(tfCPUMatcher.find()) {
        String [] libVersionPair = tfCPUMatcher.group(0).split("==");
        return findByTfVersion(libVersionPair[1]);
      }

      Pattern tfGPUPattern = Pattern.compile("(tensorflow-gpu==\\d*.\\d*.\\d*)");
      Matcher tfGPUMatcher = tfGPUPattern.matcher(envYml);

      if(tfGPUMatcher.find()) {
        String [] libVersionPair = tfGPUMatcher.group(0).split("==");
        return findByTfVersion(libVersionPair[1]);
      }
    }
    return null;
  }
}
