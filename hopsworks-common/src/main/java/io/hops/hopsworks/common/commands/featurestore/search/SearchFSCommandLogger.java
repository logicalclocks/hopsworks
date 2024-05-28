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
package io.hops.hopsworks.common.commands.featurestore.search;

import io.hops.hopsworks.common.dao.commands.search.SearchFSCommandFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommand;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommandOp;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SearchFSCommandLogger {
  private static final Logger LOGGER = Logger.getLogger(SearchFSCommandLogger.class.getName());
  
  @EJB
  private SearchFSCommandFacade commandFacade;
  @EJB
  private InodeController inodeCtrl;
  @EJB
  private FeaturegroupController featureGroupCtrl;
  @EJB
  private FeatureViewController featureViewCtrl;
  @EJB
  private TrainingDatasetController trainingDatasetCtrl;
  @EJB
  private Settings settings;
  
  public long count() {
    return commandFacade.count();
  }
  
  public void create(Featuregroup featureGroup) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureGroup, SearchFSCommandOp.CREATE);
    persistCommand(command);
  }
  
  public void create(FeatureView featureView) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureView, SearchFSCommandOp.CREATE);
    persistCommand(command);
  }
  
  public void create(TrainingDataset trainingDataset) throws FeaturestoreException {
    SearchFSCommand command = getCommand(trainingDataset, SearchFSCommandOp.CREATE);
    persistCommand(command);
  }
  
  public void updateMetadata(Featuregroup featureGroup) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureGroup, SearchFSCommandOp.UPDATE_METADATA);
    persistCommand(command);
  }
  
  public void updateMetadata(FeatureView featureView) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureView, SearchFSCommandOp.UPDATE_METADATA);
    persistCommand(command);
  }
  
  public void updateMetadata(TrainingDataset trainingDataset) throws FeaturestoreException {
    SearchFSCommand command = getCommand(trainingDataset, SearchFSCommandOp.UPDATE_METADATA);
    persistCommand(command);
  }
  
  public void updateKeywords(Featuregroup featureGroup) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureGroup, SearchFSCommandOp.UPDATE_KEYWORDS);
    persistCommand(command);
  }
  
  public void updateKeywords(FeatureView featureView) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureView, SearchFSCommandOp.UPDATE_KEYWORDS);
    persistCommand(command);
  }
  
  public void updateKeywords(TrainingDataset trainingDataset) throws FeaturestoreException {
    SearchFSCommand command = getCommand(trainingDataset, SearchFSCommandOp.UPDATE_KEYWORDS);
    persistCommand(command);
  }
  
  public void updateTags(Featuregroup featureGroup) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureGroup, SearchFSCommandOp.UPDATE_TAGS);
    persistCommand(command);
  }
  
  public void updateTags(FeatureView featureView) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureView, SearchFSCommandOp.UPDATE_TAGS);
    persistCommand(command);
  }
  
  public void updateTags(TrainingDataset trainingDataset) throws FeaturestoreException {
    SearchFSCommand command = getCommand(trainingDataset, SearchFSCommandOp.UPDATE_TAGS);
    persistCommand(command);
  }
  
  public void delete(Featuregroup featuregroup) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featuregroup, SearchFSCommandOp.DELETE_ARTIFACT);
    persistCommand(command);
  }
  
  public void delete(FeatureView featureView) throws FeaturestoreException {
    SearchFSCommand command = getCommand(featureView, SearchFSCommandOp.DELETE_ARTIFACT);
    persistCommand(command);
  }
  
  public void delete(TrainingDataset trainingDataset) throws FeaturestoreException {
    SearchFSCommand command = getCommand(trainingDataset, SearchFSCommandOp.DELETE_ARTIFACT);
    persistCommand(command);
  }
  
  public void delete(Project project) {
    SearchFSCommand command = getCommand(project, SearchFSCommandOp.DELETE_PROJECT);
    persistCommand(command);
  }
  
  private SearchFSCommand getCommand(Featuregroup featureGroup, SearchFSCommandOp op) throws FeaturestoreException {
    SearchFSCommand command = new SearchFSCommand();
    String path = featureGroupCtrl.getFeatureGroupLocation(featureGroup);
    Inode inode = inodeCtrl.getInodeAtPath(path);
    if(inode == null) {
      LOGGER.log(Level.FINE, "feature group:<{0},{1},{2}> does not have an inode",
        new Object[]{featureGroup.getId(), featureGroup.getName(), featureGroup.getVersion()});
      return null;
    }
    command.setInodeId(inode.getId());
    command.setProject(featureGroup.getFeaturestore().getProject());
    command.setStatus(CommandStatus.NEW);
    command.setFeatureGroup(featureGroup);
    command.setOp(op);
    return command;
  }
  
  private SearchFSCommand getCommand(FeatureView featureView, SearchFSCommandOp op) throws FeaturestoreException {
    SearchFSCommand command = new SearchFSCommand();
    String path = featureViewCtrl.getLocation(featureView);
    Inode inode = inodeCtrl.getInodeAtPath(path);
    if(inode == null) {
      LOGGER.log(Level.FINE, "feature view:<{0},{1},{2}> does not have an inode",
        new Object[]{featureView.getId(), featureView.getName(), featureView.getVersion()});
      return null;
    }
    command.setInodeId(inode.getId());
    command.setProject(featureView.getFeaturestore().getProject());
    command.setStatus(CommandStatus.NEW);
    command.setFeatureView(featureView);
    command.setOp(op);
    return command;
  }
  
  private SearchFSCommand getCommand(TrainingDataset trainingDataset, SearchFSCommandOp op)
    throws FeaturestoreException {
    SearchFSCommand command = new SearchFSCommand();
    FeaturestoreHopsfsConnector fsConnector =
      trainingDatasetCtrl.getHopsFsConnector(trainingDataset).getHopsfsConnector();
    String datasetPath = Utils.getDatasetPath(fsConnector.getHopsfsDataset(), settings).toString();
    String tagPath = trainingDatasetCtrl.getTrainingDatasetPath(datasetPath, trainingDataset.getName(),
      trainingDataset.getVersion());
    Inode inode = inodeCtrl.getInodeAtPath(tagPath);
    if(inode == null) {
      LOGGER.log(Level.FINE, "training dataset:<{0},{1},{2},{3}> does not have an inode",
        new Object[]{trainingDataset.getId(), trainingDataset.getFeatureView().getName(),
          trainingDataset.getFeatureView().getVersion(), trainingDataset.getVersion()});
      return null;
    }
    command.setInodeId(inode.getId());
    command.setProject(trainingDataset.getFeaturestore().getProject());
    command.setStatus(CommandStatus.NEW);
    command.setTrainingDataset(trainingDataset);
    command.setOp(op);
    return command;
  }
  
  private SearchFSCommand getCommand(Project p, SearchFSCommandOp op) {
    SearchFSCommand command = new SearchFSCommand();
    command.setInodeId(-1L);
    command.setProject(p);
    command.setStatus(CommandStatus.NEW);
    command.setOp(op);
    return command;
  }

  private void persistCommand(SearchFSCommand command) {
    if (command != null) {
      commandFacade.persistAndFlush(command);
    }
  }
}