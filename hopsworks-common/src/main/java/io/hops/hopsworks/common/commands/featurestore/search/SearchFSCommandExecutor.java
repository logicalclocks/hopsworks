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

import com.google.common.collect.Sets;
import com.lambdista.util.Try;
import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.commands.CommandFilterBy;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.dao.commands.CommandFacade;
import io.hops.hopsworks.common.dao.commands.search.SearchFSCommandFacade;
import io.hops.hopsworks.common.dao.commands.search.SearchFSCommandHistoryFacade;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.persistence.entity.commands.Command;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommand;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommandHistory;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommandOp;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Startup
@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SearchFSCommandExecutor {
  private static final Logger LOGGER = Logger.getLogger(SearchFSCommandExecutor.class.getName());
  private static final String EXECUTOR_SERVICE_NAME = "concurrent/condaExecutorService";
  
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private SearchFSCommandFacade commandFacade;
  @EJB
  private SearchFSCommandHistoryFacade commandHistoryFacade;
  @EJB
  private SearchFSOpenSearchController searchController;
  
  private boolean init = false;
  
  private ManagedExecutorService executorService;
  private Timer timer;
  
  @PostConstruct
  public void init() {
    init = initInt();
    schedule();
  }
  
  private void schedule() {
    timer = timerService.createSingleActionTimer(settings.commandSearchFSProcessTimerPeriod(),
      new TimerConfig("feature store command executor", false));
  }
  
  private boolean initInt() {
    try {
      executorService = InitialContext.doLookup(EXECUTOR_SERVICE_NAME);
      
      List<SearchFSCommand> resetOngoingCommands = commandFacade.updateByQuery(queryByStatus(CommandStatus.ONGOING),
        c -> c.failWith("Could not run search command due to internal server error. Please try again.")
      );
      resetOngoingCommands.forEach(this::saveHistory);
      List<SearchFSCommand> resetCleanCommands = commandFacade.updateByQuery(queryByStatus(CommandStatus.CLEANING),
        c -> c.failWith("Could not clean search command due to internal server error. Please try again.")
      );
      resetCleanCommands.forEach(this::saveHistory);
      return true;
    } catch(CommandException e) {
      LOGGER.log(Level.SEVERE, "Error resetting commands", e);
      // Nothing else we can do here
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error looking up for the condaExecutorService", e);
      // Nothing else we can do here
    }
    return false;
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void process() {
    //extra method in order to make sure we never forget to catch any exception (runtime included)
    //as this is a timer that reschedules itself
    if(!init) {
      init = initInt();
    }
    if(init) {
      try {
        processInt();
      } catch (Exception t) {
        LOGGER.log(Level.INFO, "Command processing failed with error:", t);
      }
    }
    schedule();
  }
  
  private void processInt() throws CommandException {
    if (!payaraClusterManager.amIThePrimary()) {
      LOGGER.log(Level.INFO, "not primary");
      return;
    }
    
    //making sure we don't overload opensearch - have at most settings.getMaxOngoingOpensearchDocIndexOps() parallel ops
    int active = 0;
    int maxOngoing = settings.getMaxOngoingOpensearchDocIndexOps();
    //do not do new operations on an artifact with an ongoing op
    List<SearchFSCommand> updatingCommands = commandFacade.findByQuery(queryByStatus(CommandStatus.ONGOING));
    active += updatingCommands.size();
    if (active >= maxOngoing) {
      return;
    }
    Set<Long> updatingDocs = updatingCommands.stream().map(this::getDocId).collect(Collectors.toSet());
    Map<Integer, Project> updatingProjects = updatingCommands.stream()
      .collect(Collectors.toMap(c -> c.getProject().getId(), Command::getProject, (p1, p2) -> p1));
    
    List<SearchFSCommand> cleaningCommands = commandFacade.findByQuery(queryByStatus(CommandStatus.CLEANING));
    active += cleaningCommands.size();
    if (active >= maxOngoing) {
      return;
    }
    Set<Long> cleaningDocs = cleaningCommands.stream().map(this::getDocId).collect(Collectors.toSet());
    updatingProjects.putAll(cleaningCommands.stream().collect(Collectors.toMap(c -> c.getProject().getId(),
      Command::getProject, (existingP, newP) -> existingP)));
    
    Map<Integer, SearchFSCommand> toDeleteProjects = commandFacade.findByQuery(queryDeletedProjects(CommandStatus.NEW))
      .stream().collect(Collectors.toMap(c -> c.getProject().getId(), c -> c, (existingC, newC) -> existingC));
    Map<Integer, SearchFSCommand> deletingProjects
      = commandFacade.findByQuery(queryDeletedProjects(CommandStatus.CLEANING))
      .stream().collect(Collectors.toMap(c -> c.getProject().getId(), c -> c, (existingC, newC) -> existingC));
    
    List<SearchFSCommand> failedCommands = commandFacade.findByQuery(queryByStatus(CommandStatus.FAILED));
    Set<Long> failedDocs = failedCommands.stream().map(this::getDocId).collect(Collectors.toSet());
    
    //clean deleted projects that are not actively worked on
    Set<Integer> deletedAndNotActive = Sets.difference(toDeleteProjects.keySet(), updatingProjects.keySet());
    for (Integer idx : deletedAndNotActive) {
      cleanDeletedProject(toDeleteProjects.get(idx));
    }
    //determine projects to be excluded
    Set<Project> excludeProjects = unionProjects(toDeleteProjects, deletingProjects);
    //determine documents to be excluded
    Set<Long> excludeDocs = new HashSet<>();
    excludeDocs.addAll(updatingDocs);
    excludeDocs.addAll(cleaningDocs);
    //clean deleted artifacts
    //that are not actively worked on
    //and are in projects that are not deleted (toDelete or deleting)
    Set<Long> deletingDocs = cleanDeletedArtifacts(excludeProjects, excludeDocs, maxOngoing - active);
    active += deletingDocs.size();
    excludeDocs.addAll(deletingDocs);
    //clean deleted (cascade) artifacts
    //that are not actively worked on
    //and are in projects that are not deleted (toDelete or deleting)
    Set<Long> deletingDocs2 = cleanDeleteCascadedArtifacts(excludeProjects, excludeDocs, maxOngoing - active);
    active += deletingDocs2.size();
    excludeDocs.addAll(deletingDocs2);
    //new
    //also exclude failed artifacts
    excludeDocs.addAll(failedDocs);
    Set<Long> processingDocs = processArtifacts(excludeProjects, excludeDocs, maxOngoing - active);
    excludeDocs.addAll(processingDocs);
    for(SearchFSCommand c : failedCommands) {
      if(shouldRetry(c)) {
        //reset failed ops if retry allows
        updateCommand(c, CommandStatus.NEW);
      }
    }
  }
  
  private Set<Project> unionProjects(Map<Integer, SearchFSCommand> p1, Map<Integer, SearchFSCommand> p2) {
    Set<Project> result = new HashSet<>();
    p1.values().forEach(c -> result.add(c.getProject()));
    p2.values().forEach(c -> result.add(c.getProject()));
    return result;
  }
  
  private void cleanDeletedProject(SearchFSCommand command) {
    updateCommand(command, CommandStatus.CLEANING);
    executorService.submit(() -> {
      Try<Boolean> result = processFunction().apply(command);
      try {
        if(result.checkedGet()) {
          List<SearchFSCommand> toSkip = commandFacade.findByQuery(queryByProject(command.getProject(),
            SearchFSCommandOp.DELETE_PROJECT));
          toSkip.forEach(c -> removeCommand(c, CommandStatus.SKIPPED));
          removeCommand(command, CommandStatus.SUCCESS);
        }
      } catch (Throwable t) {
        LOGGER.log(Level.INFO, "Project:{0} clean failed with error:{1}",
          new Object[]{command.getProject().getId(), t.getStackTrace()});
        failCommand(command, t.getMessage());
      }
    });
  }
  
  private Set<Long> cleanDeletedArtifacts(Set<Project> excludeProjects, Set<Long> excludeDocs, int maxOngoing)
    throws CommandException {
    Set<Long> deleting = new HashSet<>();
    List<SearchFSCommand> toDelete = commandFacade
      .findByQuery(queryByOp(SearchFSCommandOp.DELETE_ARTIFACT), excludeProjects, excludeDocs);
    for(SearchFSCommand command : toDelete) {
      cleanDeletedArtifact(command);
      deleting.add(getDocId(command));
      if(deleting.size() > maxOngoing) {
        break;
      }
    }
    return deleting;
  }
  
  private void cleanDeletedArtifact(SearchFSCommand command) {
    updateCommand(command, CommandStatus.CLEANING);
    executorService.submit(() -> {
      Try<Boolean> result = processFunction().apply(command);
      try {
        if(result.checkedGet()) {
          QueryParam query = queryCommandsExcept(getDocId(command), SearchFSCommandOp.DELETE_ARTIFACT);
          commandFacade.findByQuery(query).forEach(c -> removeCommand(c, CommandStatus.SKIPPED));
          removeCommand(command, CommandStatus.SUCCESS);
        }
      } catch (Throwable t) {
        LOGGER.log(Level.INFO, "Doc:{0} clean failed with error:{1}",
          new Object[]{command.getFeatureGroup().getId(), t.getStackTrace()});
        failCommand(command, t.getMessage());
      }
    });
  }
  
  private Set<Long> cleanDeleteCascadedArtifacts(Set<Project> excludeProjects, Set<Long> excludeDocs, int maxOngoing) {
    Set<Long> deleting = new HashSet<>();
    List<SearchFSCommand> toDelete = commandFacade.findDeleteCascaded(excludeProjects, excludeDocs, maxOngoing);
    for(SearchFSCommand command : toDelete) {
      cleanDeleteCascadedArtifact(command);
      deleting.add(getDocId(command));
      if(deleting.size() > maxOngoing) {
        break;
      }
    }
    return deleting;
  }
  
  private void cleanDeleteCascadedArtifact(SearchFSCommand command) {
    SearchFSCommand deleteArtifact = command.shallowClone();
    deleteArtifact.setOp(SearchFSCommandOp.DELETE_ARTIFACT);
    deleteArtifact.setStatus(CommandStatus.CLEANING);
    commandFacade.persistAndFlush(deleteArtifact);
    updateCommand(deleteArtifact, CommandStatus.CLEANING);
    cleanDeletedArtifact(deleteArtifact);
  }
  
  private Set<Long> processArtifacts(Set<Project> excludeProjects, Set<Long> excludeDocs, int maxOngoing) {
    Set<Long> processing = new HashSet<>();
    List<SearchFSCommand> toProcess = commandFacade.findToProcess(excludeProjects, excludeDocs, maxOngoing);
    for(SearchFSCommand command : toProcess) {
      processArtifact(command, processFunction());
      processing.add(getDocId(command));
      if (processing.size() >= maxOngoing) {
        break;
      }
    }
    return processing;
  }
  
  private void processArtifact(SearchFSCommand command, Function<SearchFSCommand, Try<Boolean>> execute) {
    updateCommand(command, CommandStatus.ONGOING);
    
    executorService.submit(() -> {
      Try<Boolean> result = execute.apply(command);
      try {
        if(result.checkedGet()) {
          removeCommand(command, CommandStatus.SUCCESS);
        }
      } catch (Throwable t) {
        LOGGER.log(Level.INFO, "Command:{0} failed with error:{1}",
          new Object[]{command, t.getStackTrace()});
        failCommand(command, t.getMessage());
      }
    });
  }
  
  private QueryParam queryByStatus(CommandStatus status) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new CommandFilterBy(CommandFacade.Filters.STATUS_EQ, status.toString()));
    return new QueryParam(null, null, filters, null);
  }
  
  private QueryParam queryDeletedProjects(CommandStatus status) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new CommandFilterBy(
      SearchFSCommandFacade.SearchFSFilters.OP_EQ, SearchFSCommandOp.DELETE_PROJECT.name()));
    filters.add(new CommandFilterBy(CommandFacade.Filters.STATUS_EQ, status.toString()));
    return new QueryParam(null, null, filters, null);
  }
  
  private QueryParam queryByProject(Project project, SearchFSCommandOp notOp) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new CommandFilterBy(CommandFacade.Filters.PROJECT_ID_EQ, project.getId().toString()));
    filters.add(new CommandFilterBy(SearchFSCommandFacade.SearchFSFilters.OP_NEQ, notOp.name()));
    return new QueryParam(null, null, filters, null);
  }
  
  private QueryParam queryByOp(SearchFSCommandOp op) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new CommandFilterBy(SearchFSCommandFacade.SearchFSFilters.OP_EQ, op.name()));
    return new QueryParam(null, null, filters, null);
  }
  
  private QueryParam queryCommandsExcept(Long docId, SearchFSCommandOp notOp) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new CommandFilterBy(SearchFSCommandFacade.SearchFSFilters.OP_NEQ, notOp.name()));
    filters.add(new CommandFilterBy(SearchFSCommandFacade.SearchFSFilters.DOC_EQ, docId.toString()));
    return new QueryParam(null, null, filters, null);
  }
  
  private void updateCommand(SearchFSCommand command, CommandStatus status) {
    command.setStatus(status);
    commandFacade.update(command);
    saveHistory(command);
  }
  
  private void removeCommand(SearchFSCommand command, CommandStatus status) {
    commandFacade.removeById(command.getId());
    command.setStatus(status);
    saveHistory(command);
  }
  
  private void failCommand(SearchFSCommand command, String msg) {
    command.failWith(msg);
    commandFacade.update(command);
    saveHistory(command);
  }
  
  private void saveHistory(SearchFSCommand command) {
    if(settings.commandSearchFSHistoryEnabled()) {
      commandHistoryFacade.persistAndFlush(getHistoryStep(command));
    }
  }
  
  private boolean shouldRetry(SearchFSCommand command) {
    if(settings.commandSearchFSHistoryEnabled()) {
      return commandHistoryFacade.countRetries(command.getId()) < settings.commandRetryPerCleanInterval();
    } else {
      return false;
    }
  }
  
  private Long getDocId(SearchFSCommand command) {
    return command.getInodeId();
  }
  
  private SearchFSCommandHistory getHistoryStep(SearchFSCommand command) {
    return new SearchFSCommandHistory(command);
  }
  
  private Function<SearchFSCommand, Try<Boolean>> processFunction() {
    return (SearchFSCommand c) -> {
      //extra method in order to make sure we never forget to catch any exception (runtime included)
      //as this is a function
      try {
        return processCommand(c);
      } catch (Exception t) {
        String errMsg = "command failed due to unhandled error";
        CommandException ex = new CommandException(RESTCodes.CommandErrorCode.INTERNAL_SERVER_ERROR, Level.WARNING,
          errMsg, errMsg, t);
        return new Try.Failure<>(ex);
      }
    };
  }
  
  private Try<Boolean> processCommand(SearchFSCommand c) {
    try {
      if(c.getOp().equals(SearchFSCommandOp.DELETE_PROJECT)) {
        searchController.deleteProject(c.getProject());
        return Try.apply(() -> true);
      } else if (c.getOp().equals(SearchFSCommandOp.DELETE_ARTIFACT)) {
        searchController.delete(c.getInodeId());
        return Try.apply(() -> true);
      } else {
        if (c.getProject() == null) {
          LOGGER.log(Level.FINE, "project deleted - delaying command");
          return Try.apply(() -> false);
        }
        if (c.getFeatureGroup() == null && c.getFeatureView() == null && c.getTrainingDataset() == null) {
          LOGGER.log(Level.FINE, "artifact deleted - delaying command");
          return Try.apply(() -> false);
        }
        switch(c.getOp()) {
          case CREATE: {
            searchController.create(c.getInodeId(), c);
            return Try.apply(() -> true);
          }
          case UPDATE_TAGS: {
            searchController.updateTags(c.getInodeId(), c);
            return Try.apply(() -> true);
          }
          case UPDATE_FEATURESTORE: {
            searchController.setFeaturestore(c.getInodeId(), c);
            return Try.apply(() -> true);
          }
          default :
            CommandException ex = new CommandException(RESTCodes.CommandErrorCode.NOT_IMPLEMENTED, Level.WARNING,
              "unhandled command op:" + c.getOp());
            return new Try.Failure<>(ex);
        }
      }
    } catch (OpenSearchException e) {
      String errMsg = "command failed due to opensearch error";
      CommandException ex = new CommandException(RESTCodes.CommandErrorCode.OPENSEARCH_ACCESS_ERROR, Level.WARNING,
        errMsg, errMsg, e);
      return new Try.Failure<>(ex);
    } catch (CommandException e) {
      return new Try.Failure<>(e);
    }
  }
}
