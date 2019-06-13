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

package io.hops.hopsworks.common.dao.featurestore.featuregroup;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.hive.HiveTableType;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A facade for the feature_group table in the Hopsworks database, use this interface when performing database
 * operations against the table.
 */
@Stateless
public class FeaturegroupFacade extends AbstractFacade<Featuregroup> {
  private static final Logger LOGGER = Logger.getLogger(FeaturegroupFacade.class.getName());
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  /**
   * Class constructor, invoke parent class and initialize Hive Queries
   */
  public FeaturegroupFacade() {
    super(Featuregroup.class);
  }
  
  /**
   * Retrieves a particular featuregroup given its Id from the database
   *
   * @param id id of the featuregroup
   * @return a single Featuregroup entity
   */
  public Featuregroup findById(Integer id) {
    try {
      return em.createNamedQuery("Featuregroup.findById", Featuregroup.class)
        .setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Retrieves a particular featuregroup given its Id and featurestore from the database
   *
   * @param id id of the featuregroup
   * @param featurestore featurestore of the featuregroup
   * @return a single Featuregroup entity
   */
  public Featuregroup findByIdAndFeaturestore(Integer id, Featurestore featurestore) {
    try {
      return em.createNamedQuery("Featuregroup.findByFeaturestoreAndId", Featuregroup.class)
        .setParameter("featurestore", featurestore)
        .setParameter("id", id)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Retrieves all featuregroups from the database
   *
   * @return list of featuregroup entities
   */
  @Override
  public List<Featuregroup> findAll() {
    TypedQuery<Featuregroup> q = em.createNamedQuery("Featuregroup.findAll", Featuregroup.class);
    return q.getResultList();
  }
  
  /**
   * Retrieves all featuregroups for a particular featurestore
   *
   * @param featurestore the featurestore to query
   * @return list of featuregroup entities
   */
  public List<Featuregroup> findByFeaturestore(Featurestore featurestore) {
    TypedQuery<Featuregroup> q = em.createNamedQuery("Featuregroup.findByFeaturestore", Featuregroup.class)
      .setParameter("featurestore", featurestore);
    return q.getResultList();
  }
  
  /**
   * Transaction to create a new featuregroup in the database
   *
   * @param featuregroup the featuregroup to persist
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void persist(Featuregroup featuregroup) {
    try {
      em.persist(featuregroup);
      em.flush();
    } catch (ConstraintViolationException cve) {
      LOGGER.log(Level.WARNING, "Could not persist the new Featuregroup", cve);
    }
  }
  
  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  /**
   * Gets the features of a featuregroup from the Hive Metastore
   *
   * @param hiveTableId the id of the Hive table for the featuregroup
   * @return list of featureDTOs with name,type,comment
   */
  public List<FeatureDTO> getHiveFeatures(Long hiveTableId) {
    List<Object[]> featureObjects = em.createNativeQuery("SELECT c.`COLUMN_NAME`, c.`TYPE_NAME`, " +
      "c.`COMMENT` FROM metastore.`TBLS` t " +
      "JOIN metastore.`SDS` s JOIN metastore.`COLUMNS_V2` c ON t.`SD_ID`=s.`SD_ID` AND " +
      "s.`CD_ID`=c.`CD_ID` WHERE t.`TBL_ID` = ?1;").setParameter(1, hiveTableId).getResultList();
    ArrayList<FeatureDTO> featureDTOs = new ArrayList<>();
    for (Object[] featureObject : featureObjects) {
      FeatureDTO featureDTO = new FeatureDTO((String) featureObject[0], (String) featureObject[1],
        (String) featureObject[2]);
      featureDTOs.add(featureDTO);
    }
    featureDTOs.addAll(getHivePartitionKeys(hiveTableId));
    return featureDTOs;
  }
  
  /**
   * Gets the Hive TableId for the featuregroup and version and DB-ID by querying the metastore
   *
   * @param hiveTableName the id of the hive table
   * @param hiveDbId the id of the hive database
   * @return the hive table id
   */
  public Long getHiveTableId(String hiveTableName, Long hiveDbId) {
    try {
      return (Long) em.createNativeQuery("SELECT `TBL_ID` FROM metastore.`TBLS` WHERE " +
        "`TBL_NAME` = ?1 AND `DB_ID` = ?2;")
        .setParameter(1, hiveTableName.toLowerCase())
        .setParameter(2, hiveDbId)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the partition keys of a featuregroup from the Hive metastore
   *
   * @param hiveTableId the id of the Hive table for the featuregroup
   * @return list of featureDTOs with name,type,comment of the partition keys
   */
  public List<FeatureDTO> getHivePartitionKeys(Long hiveTableId) {
    try {
      List<Object[]> featureObjects = em.createNativeQuery("SELECT `PKEY_NAME`, `PKEY_TYPE`, `PKEY_COMMENT` " +
        "FROM metastore.`PARTITION_KEYS` WHERE `TBL_ID` = ?1;").setParameter(1, hiveTableId)
        .getResultList();
      ArrayList<FeatureDTO> featureDTOs = new ArrayList<>();
      for (Object[] featureObject : featureObjects) {
        FeatureDTO featureDTO = new FeatureDTO((String) featureObject[0], (String) featureObject[1],
          (String) featureObject[2], false, true);
        featureDTOs.add(featureDTO);
      }
      return featureDTOs;
    } catch (NoResultException e) {
      return new ArrayList<>();
    }
  }
  
  /**
   * Gets the InodeId of the featuregroup by querying the metastore
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the inode id
   */
  public Long getFeaturegroupInodeId(Long hiveTableId) {
    try {
      return (Long) em.createNativeQuery("SELECT i.`id` FROM metastore.`TBLS` t " +
        "JOIN metastore.`SDS` s JOIN hops.`hdfs_inodes` i ON t.`SD_ID`=s.`SD_ID` " +
        "AND s.`PARTITION_ID`=i.`partition_id` AND s.`PARENT_ID`=i.`parent_id` AND s.`NAME`=i.`name`" +
        " WHERE `TBL_ID` = ?1;").setParameter(1, hiveTableId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the Hive Table Comment for the featuregroup by querying the metastore with the tableId
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the hive table comment
   */
  public String getHiveTableComment(Long hiveTableId) {
    try {
      return (String) em.createNativeQuery("SELECT `PARAM_VALUE` FROM metastore.`TABLE_PARAMS` " +
        "WHERE `TBL_ID` = ?1 AND `PARAM_KEY`='comment';").setParameter(1, hiveTableId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the Hive Table Name for the featuregroup by querying the metastore with the tableId
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the hive table name
   */
  public String getHiveTableName(Long hiveTableId) {
    try {
      return (String) em.createNativeQuery("SELECT `TBL_NAME` FROM metastore.`TBLS` " +
        "WHERE `TBL_ID` = ?1;").setParameter(1, hiveTableId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the Hive Table Primary Key by querying the metastore with the tableId
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the name of the column marked as primary key
   */
  public String getHiveTablePrimaryKey(Long hiveTableId) {
    try {
      return (String) em.createNativeQuery("SELECT c.`COLUMN_NAME` FROM metastore.`TBLS` t " +
        "JOIN metastore.`SDS` s JOIN metastore.`COLUMNS_V2` c ON t.`SD_ID`=s.`SD_ID` AND " +
        "s.`CD_ID`=c.`CD_ID` WHERE t.`TBL_ID` = ?1 AND EXISTS " +
        "(SELECT * FROM metastore.`KEY_CONSTRAINTS` k WHERE k.`PARENT_CD_ID`=c.`CD_ID` " +
        "AND k.`PARENT_TBL_ID` = ?1" +
        " AND k.`PARENT_INTEGER_IDX`=c.`INTEGER_IDX` AND k.`CONSTRAINT_TYPE`=0)")
        .setParameter(1, hiveTableId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the Hive table HDFS paths (in case partitions)
   *
   * @param hiveTblId the id of the hive table in the metastore
   * @return gets the HDFS paths stored in Hive metastore for the hive-table
   */
  public List<String> getHiveTableHdfsPaths(Long hiveTblId) {
    try {
      List<Object> hdfsPathObjects = em.createNativeQuery("SELECT s.`LOCATION` FROM metastore.`TBLS` t " +
        "JOIN metastore.`SDS` s ON t.`SD_ID` = s.`SD_ID` WHERE t.`TBL_ID` = ?1;")
        .setParameter(1, hiveTblId)
        .getResultList();
      return hdfsPathObjects.stream().map(o -> (String) o).collect(Collectors.toList());
    } catch (NoResultException e) {
      return new ArrayList<>();
    }
  }
  
  /**
   * Gets the table type (e.g external or managed from the Hive metastore)
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the table type
   */
  public HiveTableType getHiveTableType(Long hiveTableId) {
    try {
      return HiveTableType.valueOf((String) em.createNativeQuery("SELECT `TBL_TYPE` FROM metastore.`TBLS` " +
        "WHERE `TBL_ID`= ?1;")
        .setParameter(1, hiveTableId)
        .getSingleResult());
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Gets the Hive Table Input Format
   *
   * @param hiveTableId the id of the hive table in the metastore
   * @return the hive table format (Java class)
   */
  public String getHiveInputFormat(Long hiveTableId) {
    try {
      return (String) em.createNativeQuery("SELECT `INPUT_FORMAT` FROM metastore.`TBLS` t JOIN " +
        "metastore.`SDS` s ON t.`SD_ID`=s.`SD_ID` WHERE t.`TBL_ID`=?1")
        .setParameter(1, hiveTableId)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
  
  /**
   * Updates metadata about a featuregroup (since only metadata is changed, the Hive table does not need
   * to be modified)
   *
   * @param featuregroup the featuregroup to update
   * @param job the new job of the featuregroup
   * @return the updated featuregroup entity
   */
  public Featuregroup updateFeaturegroupMetadata(
    Featuregroup featuregroup, Jobs job) {
    featuregroup.setJob(job);
    em.merge(featuregroup);
    return featuregroup;
  }
  
}
