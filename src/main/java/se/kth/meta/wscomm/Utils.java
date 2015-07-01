package se.kth.meta.wscomm;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.meta.db.Dbao;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.FieldPredefinedValues;
import se.kth.meta.entity.Fields;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.Tables;
import se.kth.meta.entity.Templates;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author Vangelis
 */
public class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());

  private Dbao db;

  public Utils(Dbao db) {
    this.db = db;
  }

  public void addNewTemplate(Templates template) throws ApplicationException {

    try {
      this.db.addTemplate(template);
    } catch (DatabaseException e) {
      throw new ApplicationException("Could not add new template " + template.
              getName() + ""
              + " " + e.getMessage());
    }
  }

  public void removeTemplate(Templates template) throws ApplicationException {
    try {
      this.db.removeTemplate(template);
    } catch (DatabaseException e) {
      throw new ApplicationException("Could not remove template " + template.
              getName() + ""
              + " " + e.getMessage());
    }
  }

  public void addTables(List<EntityIntf> list) throws ApplicationException {

    for (EntityIntf entry : list) {
      Tables t = (Tables) entry;
      String tableName = t.getName();

      List<Fields> tableFields = new LinkedList<>(t.getFields());
      t.resetFields();

      try {
        //persist the parent
        int tableId = this.db.addTable(t);

        logger.log(Level.INFO, "TABLE: {0}", tableName);

        for (Fields field : tableFields) {
          //associate each field(child) with the table(parent) it belongs to
          field.setTableid(tableId);

          List<EntityIntf> predef = new LinkedList<>(
                  (List<EntityIntf>) (List<?>) field.getFieldPredefinedValues());

          field.resetFieldPredefinedValues();
          //persist the child
          int fieldid = this.db.addField(field);
          //remove any previous predefined values
          this.removeFieldPredefinedValues(fieldid);
          //add the new predefined values
          this.addFieldsPredefinedValues(predef, fieldid);
        }
      } catch (DatabaseException ex) {
        logger.log(Level.SEVERE, null, ex);
        throw new ApplicationException("Could not add table " + t.getName()
                + " " + ex.getMessage());
      }
    }
  }

  private void addFieldsPredefinedValues(List<EntityIntf> list, int fieldId)
          throws ApplicationException {

    try {
      for (EntityIntf entry : list) {
        FieldPredefinedValues predefval = (FieldPredefinedValues) entry;

        //associate each child with its parent
        predefval.setFieldid(fieldId);
        //persist the entity
        this.db.addFieldPredefinedValue(predefval);
      }
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException("Could not add predefined value " + e.
              getMessage());
    }
  }

  public void deleteTable(Tables table) throws ApplicationException {
    try {
      logger.log(Level.SEVERE, "DELETING TABLE {0} ", table.getName());
      this.db.deleteTable(table);
    } catch (DatabaseException e) {
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteTable "
              + "encountered a problem");
    }
  }

  public void deleteField(Fields field) throws ApplicationException {

    try {
      logger.log(Level.SEVERE, "DELETING FIELD {0} ", field);
      this.db.deleteField(field);
    } catch (DatabaseException e) {
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteField "
              + "encountered a problem");
    }
  }

  public void removeFieldPredefinedValues(int fieldid) throws
          ApplicationException {
    try {
      logger.log(Level.SEVERE, "DELETING PREDEFINED VALUES FOR FIELD {0} ",
              fieldid);
      this.db.deleteFieldPredefinedValues(fieldid);
    } catch (DatabaseException e) {
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteField "
              + "encountered a problem");
    }
  }

  public void storeMetadata(List<EntityIntf> list) throws ApplicationException {

    try {
      int tupleid = this.db.getLastInsertedTupleId() + 1;
      int inodeid = -1;

      //every rawData entity carries the same inodeid
      for (EntityIntf raw : list) {

        RawData r = (RawData) raw;
        r.setData(r.getData().replaceAll("\"", ""));
        r.setTupleid(tupleid);
        inodeid = r.getInodeid();

        logger.log(Level.INFO, r.toString());
        ((Dbao) this.db).addRawData(r);
      }

      TupleToFile ttf = new TupleToFile(tupleid, inodeid);
      ((Dbao) this.db).addTupleToFile(ttf);

    } catch (DatabaseException e) {
      throw new ApplicationException(e.getMessage(),
              "Utils.java: storeMetadata(List<?> list) "
              + "encountered a problem");
    }
  }

}
