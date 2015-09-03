package se.kth.meta.wscomm;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.meta.db.FieldFacade;
import se.kth.meta.db.FieldPredefinedValueFacade;
import se.kth.meta.db.MTableFacade;
import se.kth.meta.db.MetadataFacade;
import se.kth.meta.db.RawDataFacade;
import se.kth.meta.db.TemplateFacade;
import se.kth.meta.db.TupleToFileFacade;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.FieldPredefinedValue;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.InodeTableComposite;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.Metadata;
import se.kth.meta.entity.Template;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;

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
   * @throws ApplicationException
   */
  public void storeRawData(List<EntityIntf> composite, List<EntityIntf> raw)
          throws ApplicationException {

    try {
      /*
       * get the inodeid present in the entities in the list. It is the
       * same for all the entities, since they constitute a single tuple
       */
      InodeTableComposite itc = (InodeTableComposite) composite.get(0);

      //create a metadata tuple attached to be attached to an inodeid
      TupleToFile ttf = new TupleToFile(-1, itc.getInodeid());
      int tupleid = this.tupletoFileFacade.addTupleToFile(ttf);

      //every rawData entity carries the same inodeid
      for (EntityIntf raww : raw) {

        RawData r = (RawData) raww;
        r.getRawdataPK().setTupleid(tupleid);

        List<EntityIntf> metadataList
                = (List<EntityIntf>) (List<?>) new LinkedList<>(r.getMetadata());
        r.resetMetadata();

        //Persist the parent first
        logger.log(Level.INFO, r.toString());
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
}
