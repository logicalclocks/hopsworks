/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.util;

import io.hops.hopsworks.util.models.Dataset;
import io.hops.hopsworks.util.models.Project;
import io.hops.hopsworks.util.models.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBHelper {
  private static final String DB_HOST_ENV = "DB_HOST";
  private static final String DB_PORT_ENV = "DB_PORT";
  private static final String DB_USER_ENV = "DB_USER";
  private static final String DB_PASSWORD_ENV = "DB_PASSWORD";
  private static final String DEFAULT_PORT = "3306";
  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  private static final String DB_URL = "jdbc:mysql://%%s%%/hopsworks";
  private static final String DEFAULT_USER = "kthfs";
  private static final String DEFAULT_PASSWORD = "kthfs";
  private static final int NUM_RETRIES = 10;
  private Connection conn;
  
  public DBHelper() {
    String dbHost = (System.getenv(DB_HOST_ENV) != null) ? System.getenv(DB_HOST_ENV) : DEFAULT_HOST;
    String dbPort = (System.getenv(DB_PORT_ENV) != null) ? System.getenv(DB_PORT_ENV) : DEFAULT_PORT;
    
    String dbUser = (System.getenv(DB_USER_ENV) != null) ? System.getenv(DB_USER_ENV) : DEFAULT_USER;
    String dbPassword = (System.getenv(DB_PASSWORD_ENV) != null) ? System.getenv(DB_PASSWORD_ENV) : DEFAULT_PASSWORD;
    
    String dbURL = DB_URL.replace("%%s%%", dbHost + ":" + dbPort);
    try {
      Class.forName(JDBC_DRIVER);
      this.conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
    } catch (ClassNotFoundException | SQLException ex) {
      ex.printStackTrace();
    }
  }
  
  public String getValidationKey(String email) {
    User user = getUserByEmail(email);
    int retry = NUM_RETRIES;
    while ((user == null || user.getValidationKey() == null) && (retry-- > 0)) {
      user = getUserByEmail(email);
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return (retry > 0) ? urlEncode(user.getUsername() + user.getValidationKey()) : null;
  }
  
  public void activateUser(String email) {
    String sql = "UPDATE users SET status=2 WHERE email=?";
    update(sql, email);
  }
  
  public int addGroup(User user, String group) {
    int groupId = getGroupId(group);
    String sql = "INSERT INTO user_group (uid, gid) VALUES (?, ?)";
    PreparedStatement stmt = null;
    int rs = -1;
    try {
      stmt = conn.prepareStatement(sql);
      stmt.setInt(1, user.getUid());
      stmt.setInt(2, groupId);
      rs = stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return rs;
  }
  
  public int addGroup(String email, String group) {
    User user = getUserByEmail(email);
    return addGroup(user, group);
  }
  
  public User getUserByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email=?";
    User user = getUser(sql, email);
    return user;
  }
  
  public Project getProjectByName(String name) {
    String sql = "SELECT * FROM project WHERE projectname=?";
    Project project = getProject(sql, name);
    return project;
  }
  
  public Dataset getDatasetByName(Integer projectId, String name) {
    String sql = "SELECT * FROM dataset WHERE projectId=? AND inode_name=?";
    Dataset dataset = getDataset(sql, projectId, name);
    return dataset;
  }
  
  public int getGroupId(String group) {
    String sql = "SELECT * FROM bbc_group WHERE group_name=?";
    PreparedStatement stmt = null;
    ResultSet rs = null;
    int id = -1;
    try {
      stmt = conn.prepareStatement(sql);
      stmt.setString(1, group);
      rs = stmt.executeQuery();
      if (rs.next()) {
        id = rs.getInt("gid");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return id;
  }
  
  public boolean isTwoFactorEnabled() {
    String sql = "SELECT * FROM variables where id='twofactor_auth'";
    PreparedStatement stmt = null;
    ResultSet rs = null;
    boolean enabled = false;
    try {
      stmt = conn.prepareStatement(sql);
      rs = stmt.executeQuery();
      if (rs.next()) {
        enabled = rs.getBoolean("value");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return enabled;
  }
  
  public int update(String sql, String ... args) {
    PreparedStatement stmt = null;
    int rs = -1;
    try {
      stmt = conn.prepareStatement(sql);
      for (int i = 0; i < args.length; i++) {
        stmt.setString(i + 1, args[i]);
      }
      rs = stmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return rs;
  }
  
  public User getUser(String sql, String ... args) {
    User user = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.prepareStatement(sql);
      for (int i = 0; i < args.length; i++) {
        stmt.setString(i + 1, args[i]);
      }
      rs = stmt.executeQuery();
      if (rs.next()) {
        user = new User(rs);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return user;
  }
  
  public Project getProject(String sql, String ... args) {
    Project project = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.prepareStatement(sql);
      for (int i = 0; i < args.length; i++) {
        stmt.setString(i + 1, args[i]);
      }
      rs = stmt.executeQuery();
      if (rs.next()) {
        project = new Project(rs);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return project;
  }
  
  public Dataset getDataset(String sql, Integer arg, String ... args) {
    Dataset dataset = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.prepareStatement(sql);
      stmt.setInt(1, arg);
      for (int i = 0; i < args.length; i++) {
        stmt.setString(i + 2, args[i]);
      }
      rs = stmt.executeQuery();
      if (rs.next()) {
        dataset = new Dataset(rs);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException se) {
      }
    }
    return dataset;
  }
  
  public void getRes(PreparedStatement stmt, ResultSet rs, String sql, String ... args) {
    try {
      stmt = conn.prepareStatement(sql);
      for (int i = 0; i < args.length; i++) {
        stmt.setString(i + 1, args[i]);
      }
      rs = stmt.executeQuery();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  public static String urlEncode(String key) {
    String urlEncoded;
    try {
      urlEncoded = URLEncoder.encode(key, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
    return urlEncoded;
  }
  
  public void closeConnection() {
    try {
      if (conn != null) {
        conn.close();
      }
    } catch (SQLException se) {
      se.printStackTrace();
    }
  }
}
