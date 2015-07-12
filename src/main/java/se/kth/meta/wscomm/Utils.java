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
import se.kth.meta.db.RawDataFacade;
import se.kth.meta.db.TemplateFacade;
import se.kth.meta.db.TupleToFileFacade;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.FieldPredefinedValue;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.Template;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author Vangelis
 */
@Stateless(name="utils")
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
  
  public Utils() {
  }

  public void addNewTemplate(Template template) throws ApplicationException {

    try {
      this.templateFacade.addTemplate(template);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException("Could not add new template " + template.
              getName() + ""
              + " " + e.getMessage());
    }
  }

  public void removeTemplate(Template template) throws ApplicationException {
    try {
      this.templateFacade.removeTemplate(template);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException("Could not remove template " + template.
              getName() + ""
              + " " + e.getMessage());
    }
  }

  public void addTables(List<EntityIntf> list) throws ApplicationException {

    for (EntityIntf entry : list) {
      MTable t = (MTable) entry;
      String tableName = t.getName();

      List<Field> tableFields = new LinkedList<>(t.getFields());
      t.resetFields();

      try {
        //persist the parent
        int tableId = this.tableFacade.addTable(t);

        logger.log(Level.INFO, "STORE/UPDATE TABLE: {0}", tableName);

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
        logger.log(Level.SEVERE, null, e);
        throw new ApplicationException("Could not add table " + t.getName()
                + " " + e.getMessage());
      }
    }
  }

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
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException("Could not add predefined value " + e.
              getMessage());
    }
  }

  public void deleteTable(MTable table) throws ApplicationException {
    try {
      logger.log(Level.INFO, "DELETING TABLE {0} ", table.getName());
      this.tableFacade.deleteTable(table);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteTable "
              + "encountered a problem");
    }
  }

  public void deleteField(Field field) throws ApplicationException {

    try {
      logger.log(Level.INFO, "DELETING FIELD {0} ", field);
      this.fieldFacade.deleteField(field);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteField "
              + "encountered a problem");
    }
  }

  public void removeFieldPredefinedValues(int fieldid) throws
          ApplicationException {
    try {

      this.fieldPredefinedValueFacade.deleteFieldPredefinedValues(fieldid);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException(e.getMessage(),
              "Utils.java: method deleteField "
              + "encountered a problem");
    }
  }

  public void storeMetadata(List<EntityIntf> list) throws ApplicationException {

    try {
      /*
       * get the inodeid present in the entities in the list. It is the
       * same for all the entities, since they constitute a single tuple
       */
      RawData r = (RawData) list.get(0);
      
      //create a metadata tuple attached to an inodeid
      TupleToFile ttf = new TupleToFile(-1, r.getInodeid());
      int tupleid = this.tupletoFileFacade.addTupleToFile(ttf);
      
      //every rawData entity carries the same inodeid
      for (EntityIntf raw : list) {

        r = (RawData) raw;
        r.setData(r.getData().replaceAll("\"", ""));
        r.setTupleid(tupleid);

        logger.log(Level.INFO, r.toString());
        this.rawDataFacade.addRawData(r);
      }

    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new ApplicationException(e.getMessage(),
              "Utils.java: storeMetadata(List<?> list) "
              + "encountered a problem");
    }
  }

}
