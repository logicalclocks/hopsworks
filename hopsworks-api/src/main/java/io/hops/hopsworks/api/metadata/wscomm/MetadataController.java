/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.metadata.wscomm;

import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.log.meta.MetaLog;
import io.hops.hopsworks.common.dao.log.meta.MetaLogFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.log.operation.OperationsLog;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.Field;
import io.hops.hopsworks.common.dao.metadata.FieldPredefinedValue;
import io.hops.hopsworks.common.dao.metadata.InodeTableComposite;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.dao.metadata.RawData;
import io.hops.hopsworks.common.dao.metadata.SchemalessMetadata;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.TupleToFile;
import io.hops.hopsworks.common.dao.metadata.db.FieldFacade;
import io.hops.hopsworks.common.dao.metadata.db.FieldPredefinedValueFacade;
import io.hops.hopsworks.common.dao.metadata.db.MTableFacade;
import io.hops.hopsworks.common.dao.metadata.db.MetadataFacade;
import io.hops.hopsworks.common.dao.metadata.db.RawDataFacade;
import io.hops.hopsworks.common.dao.metadata.db.SchemalessMetadataFacade;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.metadata.db.TupleToFileFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.HopsUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = "metadataController")
public class MetadataController {

  private static final Logger logger = Logger.getLogger(
          MetadataController.class.getName());

  @EJB
  private TemplateFacade templateFacade;
  @EJB
  private MTableFacade tableFacade;
  @EJB
  private FieldFacade fieldFacade;
  @EJB
  private FieldPredefinedValueFacade fieldPredefinedValueFacade;
  @EJB
  private RawDataFacade rawDataFacade;
  @EJB
  private TupleToFileFacade tupletoFileFacade;
  @EJB
  private MetadataFacade metadataFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private MetaLogFacade metaLogFacade;
  @EJB
  private OperationsLogFacade opsLogFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private SchemalessMetadataFacade schemalessMetadataFacade;

  public MetadataController() {
  }

  /**
   * Persist a new template in the database
   * <p/>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int addNewTemplate(Template template) throws ApplicationException {

    try {
      if (!this.templateFacade.isTemplateAvailable(template.getName().
              toLowerCase())) {
        return this.templateFacade.addTemplate(template);
      } else {
        throw new DatabaseException("Template name " + template.getName()
                + " already available");
      }

    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: Could not add new template "
              + template.getName(), e);
    }
  }

  /**
   * Updates a template name. addNewTemplate handles persisting/updating the
   * entity
   * <p/>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int updateTemplateName(Template template) throws ApplicationException {
    return this.addNewTemplate(template);
  }

  /**
   * Deletes a template from the database
   * <p/>
   * @param template
   * @throws ApplicationException
   */
  public void removeTemplate(Template template) throws ApplicationException {
    try {
      this.templateFacade.removeTemplate(template);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: Could not remove template "
              + template.getName(), e);
    }
  }

  /**
   * Persist a list of tables and all their corresponding child entities in the
   * database
   * <p/>
   * @param list
   * @throws ApplicationException
   */
  public void addTables(List<EntityIntf> list) throws ApplicationException {

    for (EntityIntf entry : list) {
      MTable t = (MTable) entry;
      String tableName = t.getName();

      List<Field> tableFields = new LinkedList<>(t.getFields());
      t.resetFields();

      try {
        logger.log(Level.INFO, "STORE/UPDATE TABLE: {0} ", t);

        //persist the parent
        int tableId = this.tableFacade.addTable(t);
        for (Field field : tableFields) {
          //associate each field(child) with its table(parent)
          field.setTableid(tableId);

          List<EntityIntf> predef = new LinkedList<>(
                  (List<EntityIntf>) (List<?>) field.getFieldPredefinedValues());

          field.resetFieldPredefinedValues();
          //persist the child
          int fieldid = this.fieldFacade.addField(field);
          //remove any previous predefined values
          this.removeFieldPredefinedValues(fieldid);
          //add the new predefined values
          this.addFieldsPredefinedValues(predef, fieldid);
        }
      } catch (DatabaseException e) {
        throw new ApplicationException("Utils.java: Could not add table " + t.
                getName(), e);
      }
    }
  }

  /**
   * Stores the predefined values for a field
   * <p/>
   * @param list
   * @param fieldId
   * @throws ApplicationException
   */
  private void addFieldsPredefinedValues(List<EntityIntf> list, int fieldId)
          throws ApplicationException {

    try {
      for (EntityIntf entry : list) {
        FieldPredefinedValue predefval = (FieldPredefinedValue) entry;

        //associate each child with its parent
        predefval.setFieldid(fieldId);
        //persist the entity
        this.fieldPredefinedValueFacade.addFieldPredefinedValue(predefval);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException(
              "Utils.java: Could not add predefined values", e);
    }
  }

  /**
   * Removes a table from the database
   * <p/>
   * @param table
   * @throws ApplicationException
   */
  public void deleteTable(MTable table) throws ApplicationException {
    try {
      logger.log(Level.INFO, "DELETING TABLE {0} ", table.getName());
      this.tableFacade.deleteTable(table);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete table "
              + table);
    }
  }

  /**
   * Removes a field from the database
   * <p/>
   * @param field
   * @throws ApplicationException
   */
  public void deleteField(Field field) throws ApplicationException {

    try {
      logger.log(Level.INFO, "DELETING FIELD {0} ", field);
      this.fieldFacade.deleteField(field);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete field "
              + field);
    }
  }

  /**
   * Removes a field's predefined values
   * <p/>
   * @param fieldid
   * @throws ApplicationException
   */
  public void removeFieldPredefinedValues(int fieldid) throws
          ApplicationException {
    try {
      this.fieldPredefinedValueFacade.deleteFieldPredefinedValues(fieldid);
    } catch (DatabaseException e) {
      throw new ApplicationException(
              "Utils.java: could not remove field predefined values ", e);
    }
  }

  /**
   * Stores the raw data for an inode. Creates a tuple first and
   * associates it with the given inode. Raw data at this point is just a field
   * id and a tupleid
   * <p/>
   * @param composite
   * @param raw
   * @throws ApplicationException
   */
  public void storeRawData(List<EntityIntf> composite, List<EntityIntf> raw)
          throws ApplicationException {

    try {
      /*
       * get the inodeid from the entity in the list. It is the
       * same for all the entities, since they constitute a single tuple
       */
      InodeTableComposite itc = (InodeTableComposite) composite.get(0);

      //get the inode
      Inode parent = this.inodeFacade.findById(itc.getInodePid());
      Inode inode = this.inodeFacade.findByInodePK(parent, itc.
              getInodeName(), HopsUtils.calculatePartitionId(parent.getId(),
                      itc.getInodeName(), 3));

      //create a metadata tuple attached to be attached to an inodeid
      TupleToFile ttf = new TupleToFile(-1, inode);
      int tupleid = this.tupletoFileFacade.addTupleToFile(ttf);

      //every rawData entity carries the same inodeid
      for (EntityIntf raww : raw) {

        RawData r = (RawData) raww;
        r.getRawdataPK().setTupleid(tupleid);

        List<EntityIntf> metadataList
                = (List<EntityIntf>) (List<?>) new LinkedList<>(r.getMetadata());
        r.resetMetadata();

        //Persist the parent first
        //logger.log(Level.INFO, r.toString());
        this.rawDataFacade.addRawData(r);

        //move on to persist the child entities
        this.storeMetaData(metadataList, tupleid);
      }

    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store raw data ", e);
    }
  }

  /**
   * Updates a single raw data record.
   * <p/>
   * @param composite
   * @param metaId
   * @param metaObj
   * @throws ApplicationException
   */
  public void updateMetadata(List<EntityIntf> composite, int metaId,
          String metaObj) throws
          ApplicationException {
    try {
      Metadata metadata = this.metadataFacade.getMetadataById(metaId);
      metadata.setData(metaObj);
      this.metadataFacade.addMetadata(metadata);
      logMetadataOperation(metadata, OperationType.Update);

    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not update metadata ", e);
    }
  }

  /**
   * Remove a single raw metadata record.
   * <p/>
   * @param composite
   * @param metaId
   * @param metaObj
   * @return
   * @throws ApplicationException
   */
  public void removeMetadata(List<EntityIntf> composite, int metaId,
          String metaObj) throws
          ApplicationException {
    try {

      Metadata metadata = this.metadataFacade.getMetadataById(metaId);
      metadata.setData(metaObj);
      this.metadataFacade.removeMetadata(metadata);
      logMetadataOperation(metadata, OperationType.Delete);

    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not delete metadata ", e);
    }
  }

  /**
   * Stores the actual metadata for an inode. Associates a raw data record with
   * a metadata record in the meta_data table
   * <p/>
   * @param metadatalist
   * @param tupleid
   * @throws ApplicationException
   */
  public void storeMetaData(List<EntityIntf> metadatalist, int tupleid) throws
          ApplicationException {

    try {
      for (EntityIntf entity : metadatalist) {
        Metadata metadata = (Metadata) entity;
        metadata.getMetadataPK().setTupleid(tupleid);

        //logger.log(Level.INFO, metadata.toString());
        this.metadataFacade.addMetadata(metadata);
        logMetadataOperation(metadata, OperationType.Add);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store metadata ", e);
    }
  }

  public void addSchemaLessMetadata(String inodePath, String metadataJson)
          throws ApplicationException {
    Inode inode = inodeFacade.getInodeAtPath(inodePath);
    if (inode == null) {
      throw new ApplicationException("file " + inodePath + " doesn't exist");
    }

    boolean update = true;
    SchemalessMetadata metadata = schemalessMetadataFacade.findByInode(inode);
    if (metadata == null) {
      update = false;
      metadata = new SchemalessMetadata(inode);
    }
    metadata.setData(metadataJson);

    schemalessMetadataFacade.merge(metadata);
    logSchemaLessMetadataOperation(metadata, update ? OperationType.Update
            : OperationType.Add);
  }

  public void removeSchemaLessMetadata(String inodePath) throws
          ApplicationException {
    Inode inode = inodeFacade.getInodeAtPath(inodePath);
    if (inode == null) {
      throw new ApplicationException("file " + inodePath + " doesn't exist");
    }
    SchemalessMetadata metadata = schemalessMetadataFacade.findByInode(inode);
    if (metadata == null) {
      throw new ApplicationException("No metadata attached with " + inodePath);
    }
    MetaLog removeOpLog = new MetaLog(metadata, OperationType.Delete);
    schemalessMetadataFacade.remove(metadata);
    metaLogFacade.persist(removeOpLog);
  }

  private void logSchemaLessMetadataOperation(SchemalessMetadata metadata,
          OperationType opType) {
    metaLogFacade.persist(new MetaLog(metadata, opType));
  }

  private void logMetadataOperation(Metadata metadata, OperationType optype) {
    metaLogFacade.persist(new MetaLog(metadata, optype));
  }

  public void logTemplateOperation(Template template, Inode inode,
          OperationType optype) {
    Pair<Inode, Inode> project_dataset = inodeFacade.
            getProjectAndDatasetRootForInode(inode);
    Project project = projectFacade.findByInodeId(project_dataset.getL().
            getInodePK().getParentId(),
            project_dataset.getL().getInodePK().getName());
    Dataset dataset = datasetFacade.findByProjectAndInode(project,
            project_dataset.getR());

    opsLogFacade.persist(new OperationsLog(project, dataset, template, inode,
            optype));
  }

}
