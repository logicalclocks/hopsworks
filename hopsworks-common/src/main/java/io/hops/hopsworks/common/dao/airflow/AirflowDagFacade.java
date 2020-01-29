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

package io.hops.hopsworks.common.dao.airflow;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is special case of a facade. It does not use the EntityManager.
 *
 * Installation of Airflow is optional. This means that we can't have the
 * airflow persistence unit statically in persistence.xml file as the Connection
 * Pool and JDBC Resource won't exist in Glassfish.
 *
 * All persistence units in persistence.xml *must* exist during application
 * deployment. Otherwise the deployment fails.
 *
 * To tackle this dynamic installation, we skip adding airflow PU in persistence.xml
 * and we inject the JDBC Resource programmatically.
 *
 * The method in this EJB will only be invoked when Airflow is installed, so it is
 * safe to inject jdbc/airflow DataSource.
 *
 * DO NOT invoke the DataSource directly!
 *
 * Look on other facades for examples on how to properly access the database.
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AirflowDagFacade {
  
  private static final Logger LOGGER = Logger.getLogger(AirflowDagFacade.class.getName());
  private static final String DAGS_STATUS_QUERY = "SELECT dag_id, is_paused FROM airflow.dag WHERE owners = ?";
  private static final String GET_ALL_DAGS_WITH_LIMIT_QUERY = "SELECT dag_id, is_paused FROM airflow.dag LIMIT ?";
  
  @Resource(name = "jdbc/airflow")
  private DataSource airflowDataSource;
  
  public List<AirflowDag> filterByOwner(String owner) throws IOException, SQLException {
    if (owner == null || owner.isEmpty()) {
      throw new IOException("Airflow DAG owner cannot be null or empty");
    }
    List<AirflowDag> dags = new ArrayList<>();
    PreparedStatement stmt = null;
    ResultSet dagsRS = null;
    try {
      Connection connection = airflowDataSource.getConnection();
      stmt = connection.prepareStatement(DAGS_STATUS_QUERY);
      stmt.setString(1, owner);
      dagsRS = stmt.executeQuery();
      while (dagsRS.next()) {
        AirflowDag dag = new AirflowDag(dagsRS.getString("dag_id"), dagsRS.getBoolean("is_paused"));
        dags.add(dag);
      }
      return dags;
    } finally {
      try {
        if (dagsRS != null) {
          dagsRS.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException ex) {
        // Just log them, can't do much about them here
        LOGGER.log(Level.WARNING, "Could not release resources", ex);
      }
    }
  }
  
  public List<AirflowDag> getAllWithLimit(Integer limit) throws SQLException {
    List<AirflowDag> dags = new ArrayList<>();
    PreparedStatement stmt = null;
    ResultSet dagsRS = null;
    try {
      Connection connection = airflowDataSource.getConnection();
      stmt = connection.prepareStatement(GET_ALL_DAGS_WITH_LIMIT_QUERY);
      stmt.setInt(1, limit);
      dagsRS = stmt.executeQuery();
      while (dagsRS.next()) {
        AirflowDag dag = new AirflowDag(dagsRS.getString("dag_id"), dagsRS.getBoolean("is_paused"));
        dags.add(dag);
      }
      return dags;
    } finally {
      try {
        if (dagsRS != null) {
          dagsRS.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException ex) {
        // Just log them, can't do much about them here
        LOGGER.log(Level.WARNING, "Could not release resources", ex);
      }
    }
  }
}
