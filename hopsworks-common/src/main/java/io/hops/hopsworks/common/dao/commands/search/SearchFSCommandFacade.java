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
package io.hops.hopsworks.common.dao.commands.search;

import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.dao.commands.CommandFacade;
import io.hops.hopsworks.common.dao.commands.CommandFilter;
import io.hops.hopsworks.persistence.entity.commands.CommandStatus;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommand;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommandOp;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

@Stateless
public class SearchFSCommandFacade extends CommandFacade<SearchFSCommand> {
  private static final String FEATURE_GROUP_FIELD = "featureGroup";
  private static final String FEATURE_VIEW_FIELD = "featureView";
  private static final String TRAINING_DATASET_FIELD = "trainingDataset";
  private static final String DOC_ID_FIELD = "inodeId";
  private static final String OP_FIELD = "op";
  
  public SearchFSCommandFacade() {
    super(SearchFSCommand.class);
  }
  
  @Override
  protected String getTableName() {
    return SearchFSCommand.TABLE_NAME;
  }
  
  public List<SearchFSCommand> findByQuery(QueryParam queryParam, Set<Project> excludeProjects, Set<Long> excludeDocs)
    throws CommandException {
    if(queryParam == null) {
      throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO, "query param is null");
    }
    String queryStrPrefix = "SELECT c FROM " + getTableName() + " c ";
    String queryStr = buildQuery(queryStrPrefix, queryParam.getFilters(), queryParam.getSorts(), "");
    if(!excludeProjects.isEmpty()) {
      queryStr += " AND c." + PROJECT_FIELD + " NOT IN :exclude_" + PROJECT_FIELD;
    }
    if(!excludeDocs.isEmpty()) {
      queryStr += " AND c." + DOC_ID_FIELD + " NOT IN :exclude_" + DOC_ID_FIELD;
    }
    Query q = em.createQuery(queryStr, entityClass);
    setParams(q, queryParam.getFilters());
    if(queryParam.getLimit() != null) {
      q.setMaxResults(queryParam.getLimit());
    }
    if(!excludeProjects.isEmpty()) {
      q.setParameter("exclude_" + PROJECT_FIELD, excludeProjects);
    }
    if(!excludeDocs.isEmpty()) {
      q.setParameter("exclude_" + DOC_ID_FIELD, excludeDocs);
    }
    return q.getResultList();
  }
  
  public List<SearchFSCommand> findToProcess(Set<Project> excludeProjects, Set<Long> excludeDocs, int limit) {
    UnaryOperator<String> filterFoLive = tableName -> {
      String filter = "(";
      filter += tableName + "." + FEATURE_GROUP_FIELD + " IS NOT NULL OR ";
      filter += tableName + "." + FEATURE_VIEW_FIELD + " IS NOT NULL OR ";
      filter += tableName + "." + TRAINING_DATASET_FIELD + " IS NOT NULL";
      filter += ")";
      return filter;
    };
    return findToProcessInt(excludeProjects, excludeDocs, limit, filterFoLive);
  }
  
  public List<SearchFSCommand> findDeleteCascaded(Set<Project> excludeProjects, Set<Long> excludeDocs, int limit) {
    UnaryOperator<String> filterForDeleteCascaded = tableName -> {
      String filter = "(";
      filter += tableName + "." + FEATURE_GROUP_FIELD + " IS NULL AND ";
      filter += tableName + "." + FEATURE_VIEW_FIELD + " IS NULL AND ";
      filter += tableName + "." + TRAINING_DATASET_FIELD + " IS NULL";
      filter += ")";
      return filter;
    };
    return findToProcessInt(excludeProjects, excludeDocs, limit, filterForDeleteCascaded);
  }
  
  private List<SearchFSCommand> findToProcessInt(Set<Project> excludeProjects, Set<Long> excludeDocs, int limit,
                                                 UnaryOperator<String> queryAppendFilter) {
    String queryStr = "";
    queryStr += "SELECT jc FROM " + getTableName() + " jc WHERE jc.id IN (";
    //get oldest per artifact
    queryStr += "SELECT MIN(c.id) FROM " + getTableName() +" c";
    //for specific status and excluding specific artifacts
    queryStr += " WHERE c.status = :status";
    queryStr += " AND " + queryAppendFilter.apply("c");
    if(!excludeProjects.isEmpty()) {
      queryStr += " AND c." + PROJECT_FIELD + " NOT IN :exclude_" + PROJECT_FIELD;
    }
    if(!excludeDocs.isEmpty()) {
      queryStr += " AND c." + DOC_ID_FIELD + " NOT IN :exclude_" + DOC_ID_FIELD;
    }
    //per artifact
    queryStr += " GROUP BY c." + DOC_ID_FIELD;
    queryStr += ")";
    
    TypedQuery<SearchFSCommand> query = em.createQuery(queryStr, entityClass);
    query.setParameter("status", CommandStatus.NEW);
    if (!excludeProjects.isEmpty()) {
      query.setParameter("exclude_" + PROJECT_FIELD, excludeProjects);
    }
    if (!excludeDocs.isEmpty()) {
      query.setParameter("exclude_" + DOC_ID_FIELD, excludeDocs);
    }
    query.setMaxResults(limit);
    return query.getResultList();
  }
  
  public enum SearchFSFilters implements CommandFilter {
    FG_IS_NULL(FEATURE_GROUP_FIELD, "c." + FEATURE_GROUP_FIELD + " IS NULL ", null),
    FG_NOT_NULL(FEATURE_GROUP_FIELD, "c." + FEATURE_GROUP_FIELD + " IS NOT NULL ", null),
    FG_EQ(FEATURE_GROUP_FIELD, "c." + FEATURE_GROUP_FIELD + ".id = :" + FEATURE_GROUP_FIELD, null),
    FV_IS_NULL(FEATURE_VIEW_FIELD, "c." + FEATURE_VIEW_FIELD +" IS NULL ", null),
    FV_NOT_NULL(FEATURE_VIEW_FIELD, "c." + FEATURE_VIEW_FIELD + " IS NOT NULL ", null),
    FV_EQ(FEATURE_VIEW_FIELD, "c." + FEATURE_VIEW_FIELD + ".id = :" + FEATURE_VIEW_FIELD, null),
    TD_IS_NULL(TRAINING_DATASET_FIELD, "c." + TRAINING_DATASET_FIELD + " IS NULL ", null),
    TD_NOT_NULL(TRAINING_DATASET_FIELD, "c." + TRAINING_DATASET_FIELD + " IS NOT NULL ", null),
    TD_EQ(TRAINING_DATASET_FIELD, "c." + TRAINING_DATASET_FIELD + ".id = :" + TRAINING_DATASET_FIELD, null),
    DOC_EQ(DOC_ID_FIELD, "c."+ DOC_ID_FIELD + " = :" + DOC_ID_FIELD, null),
    OP_EQ(OP_FIELD, "c." + OP_FIELD + " = :" + OP_FIELD, SearchFSCommandOp.CREATE.name()),
    OP_NEQ(OP_FIELD, "c." + OP_FIELD + " != :" + OP_FIELD, SearchFSCommandOp.CREATE.name());
    
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    SearchFSFilters(String field, String sql, String defaultParam) {
      this.field = field;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    @Override
    public String getValue() {
      return this.name();
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return this.name();
    }
  }
  
  @Override
  protected void setParam(Query q, AbstractFacade.FilterBy filterBy) throws CommandException {
    try {
      if(filterBy.getField().equals(FEATURE_GROUP_FIELD) ||
        filterBy.getField().equals(FEATURE_VIEW_FIELD) ||
        filterBy.getField().equals(TRAINING_DATASET_FIELD)) {
        if(filterBy.getParam() != null) {
          q.setParameter(filterBy.getField(), filterBy.getParam());
        }
      } else if(filterBy.getField().equals(OP_FIELD)) {
        q.setParameter(filterBy.getField(), SearchFSCommandOp.valueOf(filterBy.getParam()));
      } else if(filterBy.getField().equals(DOC_ID_FIELD)) {
        q.setParameter(filterBy.getField(), Long.parseLong(filterBy.getParam()));
      } else {
        super.setParam(q, filterBy);
      }
    } catch (IllegalArgumentException e) {
      String msg = "invalid filter:" + filterBy.toString();
      throw new CommandException(RESTCodes.CommandErrorCode.INVALID_SQL_QUERY, Level.INFO,
        msg, msg, e);
    }
  }
}
