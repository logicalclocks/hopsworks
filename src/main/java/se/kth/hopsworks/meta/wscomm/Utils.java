package se.kth.hopsworks.meta.wscomm;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodePK;
import se.kth.hopsworks.meta.db.FieldFacade;
import se.kth.hopsworks.meta.db.FieldPredefinedValueFacade;
import se.kth.hopsworks.meta.db.HdfsMetadataLogFacade;
import se.kth.hopsworks.meta.db.MTableFacade;
import se.kth.hopsworks.meta.db.MetadataFacade;
import se.kth.hopsworks.meta.db.RawDataFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.db.TupleToFileFacade;
import se.kth.hopsworks.meta.entity.DirPath;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.FieldPredefinedValue;
import se.kth.hopsworks.meta.entity.Field;
import se.kth.hopsworks.meta.entity.HdfsMetadataLog;
import se.kth.hopsworks.meta.entity.InodeTableComposite;
import se.kth.hopsworks.meta.entity.RawData;
import se.kth.hopsworks.meta.entity.MTable;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.entity.TupleToFile;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.exception.DatabaseException;

/**
 *
 * @author Vangelis
 */
@Stateless(name = "utils")
public class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());

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
  private FileOperations fops;
  @EJB
  private HdfsMetadataLogFacade mlf;
  @EJB
  private InodeFacade inodeFacade;

  public Utils() {
  }

  /**
   * Persist a new template in the database
   * <p>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int addNewTemplate(Template template) throws ApplicationException {

    try {
      return this.templateFacade.addTemplate(template);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: Could not add new template "
              + template.getName(), e);
    }
  }

  /**
   * Updates a template name. addNewTemplate handles persisting/updating the
   * entity
   * <p>
   * @param template
   * @return
   * @throws ApplicationException
   */
  public int updateTemplateName(Template template) throws ApplicationException {
    return this.addNewTemplate(template);
  }

  /**
   * Deletes a template from the database
   * <p>
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
   * <p>
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
   * <p>
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
   * <p>
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
   * <p>
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
   * <p>
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
   * <p>
   * @param composite
   * @param raw
   * @return The inode
   * @throws ApplicationException
   */
  public Inode storeRawData(List<EntityIntf> composite, List<EntityIntf> raw)
          throws ApplicationException {

    try {
      /*
       * get the inodeid present in the entities in the list. It is the
       * same for all the entities, since they constitute a single tuple
       */
      InodeTableComposite itc = (InodeTableComposite) composite.get(0);

      //get the inode
      Inode parent = this.inodeFacade.findById(itc.getInodePid());
      Inode inode = this.inodeFacade.findByParentAndName(parent, itc.
              getInodeName());
      
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
      return inode;
      
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store raw data ", e);
    }
  }

  /**
   * Updates a single raw data record.
   * <p>
   * @param meta
   * @throws ApplicationException
   */
  public void updateMetadata(Metadata meta) throws ApplicationException {
    try {

      Metadata metadata = this.metadataFacade.getMetadataById(meta.
              getMetadataPK().getId());
      metadata.setData(meta.getData());
      this.metadataFacade.addMetadata(metadata);
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not update metadata ", e);
    }
  }

  /**
   * Stores the actual metadata for an inode. Associates a raw data record with
   * a metadata record in the meta_data table
   * <p>
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
      }
    } catch (DatabaseException e) {
      throw new ApplicationException("Utils.java: could not store metadata ", e);
    }
  }

  /**
   * Renames a given dir path. Used when attaching metadata to already
   * indexed inodes as a means to get those inodes again in the metadata log
   * table. Elastic rivers will pick them up again along with their metadata
   * <p>
   * @param path
   * @param specialChar
   * @throws ApplicationException
   */
  public void renameDir(DirPath path, String specialChar) throws
          ApplicationException {

    String pathh = path.getPath();
    int pathLength = path.getLength();
    String oldDirPath = null;
    String newDirPath = null;

    //the path refers to a folder
    if (this.fops.isDir(pathh)) {
      oldDirPath = pathh;
    } else {
      //the path refers to a file
      oldDirPath = se.kth.bbc.lims.Utils.getDirectoryPart(pathh);
      //refresh the length, file name was removed
      pathLength = oldDirPath.length();
    }

    if (oldDirPath.endsWith(File.separator)) {
      //leave out the trailing slash
      newDirPath = oldDirPath.substring(0, pathLength - 2);
      oldDirPath = newDirPath;
    } else {
      newDirPath = oldDirPath;
    }

    if (specialChar.equals("_")) {
      //add the underscore to the dir name
      newDirPath += "_";
    } else {
      oldDirPath += "_";
    }

    try {
      this.fops.renameInHdfs(oldDirPath, newDirPath);
    } catch (IOException e) {
      throw new ApplicationException("ApplicationException",
              "Utils.java: renameDir(String) encountered a problem " + e.
              getMessage());
    }
  }

  /**
   * Copies a given dir to destination. Used when attaching metadata to already
   * indexed inodes as a means to get those inodes again in the metadata log
   * table. Elastic rivers will pick them up again along with their metadata
   * <p>
   * @param path the source path
   * @param destination the destination path
   * @throws ApplicationException
   */
  public void copyDir(String path, String destination) throws
          ApplicationException {

    try {
      //create the destination path if it doesn't exist
      if (!this.fops.isDir(destination)) {
        this.fops.mkDir(destination);
      }

      //copy the dir to the destination
      this.fops.copyWithinHdfs(path, destination);
    } catch (IOException e) {
      throw new ApplicationException("ApplicationException",
              "Utils.java: copyDir(String, String) encountered a problem " + e.
              getMessage());
    }
  }

  /**
   * Removes a dir recursively from the file system.
   * <p>
   * @param path. The path of the directory to be removed
   * @throws ApplicationException
   */
  public void removeDir(String path) throws ApplicationException {
    try {
      this.fops.rmRecursive(path);
    } catch (IOException e) {
      throw new ApplicationException("ApplicationException",
              "Utils.java: removeDir(String) encountered a problem " + e.
              getMessage());
    }
  }

  /**
   * Creates a fake inode operation in hdfs_metadata_log table.
   * <p>
   * @param log
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void createMetadataLog(HdfsMetadataLog log) throws DatabaseException {

    this.mlf.addHdfsMetadataLog(log);
  }
}
