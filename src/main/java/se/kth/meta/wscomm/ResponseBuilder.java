package se.kth.meta.wscomm;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.meta.db.FieldFacade;
import se.kth.meta.db.FieldPredefinedValueFacade;
import se.kth.meta.db.FieldTypeFacade;
import se.kth.meta.db.MTableFacade;
import se.kth.meta.db.TemplateFacade;
import se.kth.meta.db.TupleToFileFacade;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.FieldType;
import se.kth.meta.entity.InodeTableComposite;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.Template;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;
import se.kth.meta.wscomm.message.ContentMessage;
import se.kth.meta.wscomm.message.FetchMetadataMessage;
import se.kth.meta.wscomm.message.FetchTableMetadataMessage;
import se.kth.meta.wscomm.message.FieldTypeMessage;
import se.kth.meta.wscomm.message.Message;
import se.kth.meta.wscomm.message.TextMessage;

/**
 * Helper class to assist Protocol in constructing responses. Keeps Protocol
 * clean
 * <p>
 * @author vangelis
 */
@Stateless(name = "responseBuilder")
public class ResponseBuilder {

  private static final Logger logger = Logger.
          getLogger(ResponseBuilder.class.getName());

  @EJB
  private Utils utils;
  @EJB
  private TemplateFacade templateFacade;
  @EJB
  private MTableFacade tableFacade;
  @EJB
  private TupleToFileFacade tupletofileFacade;
  @EJB
  private FieldTypeFacade fieldTypeFacade;
  @EJB
  private FieldFacade fieldFacade;
  @EJB
  private FieldPredefinedValueFacade fpf;

  public ResponseBuilder() {
    logger.log(Level.INFO, "ResponseBuilder initialized");
  }

  public Message addNewTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.addNewTemplate(template);

    return this.fetchTemplates(message);
  }

  public Message removeTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.removeTemplate(template);

    return this.fetchTemplates(message);
  }

  public void storeSchema(Message message) throws ApplicationException {
    List<EntityIntf> schema = message.parseSchema();
    this.utils.addTables(schema);
  }

  public Message fetchTemplates(Message message) {
    List<Template> templates = this.templateFacade.loadTemplates();

    Collections.sort(templates);
    String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) templates);
    message.setMessage(jsonMsg);

    return message;
  }

  public Message fetchFieldTypes(Message message) {
    List<FieldType> ftypes = this.fieldTypeFacade.loadFieldTypes();
    Collections.sort(ftypes);

    FieldTypeMessage newMsg = new FieldTypeMessage();

    String jsonMsg = newMsg.buildSchema((List<EntityIntf>) (List<?>) ftypes);

    newMsg.setSender(message.getSender());
    newMsg.setMessage(jsonMsg);

    return newMsg;
  }

  public Message createSchema(Message message) {
    ContentMessage cmsg = (ContentMessage) message;

    List<MTable> tables = this.templateFacade.loadTemplateContent(cmsg.
            getTemplateid());

    String jsonMsg = cmsg.buildSchema((List<EntityIntf>) (List<?>) tables);
    message.setMessage(jsonMsg);

    return message;
  }

  /**
   * Fetches ALL the metadata a metadata table carries. It does not do any
   * filtering
   *
   * @param table
   * @return
   * @throws se.kth.meta.exception.ApplicationException
   */
  public Message fetchTableMetadata(MTable table) throws ApplicationException {
    try {
      FetchTableMetadataMessage message
              = new FetchTableMetadataMessage("Server", "");
      MTable t = this.tableFacade.getTable(table.getId());

      List<MTable> tables = new LinkedList<>();
      tables.add(t);
      String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) tables);
      message.setMessage(jsonMsg);

      return message;
    } catch (DatabaseException e) {
      throw new ApplicationException("Failed to fetch metadata for " + table, e);
    }
  }

  /**
   * Fetches the metadata a metadata table carries ONLY for a specific inodeid
   *
   * @param itc
   * @return
   * @throws se.kth.meta.exception.ApplicationException
   */
  public Message fetchInodeMetadata(InodeTableComposite itc) throws
          ApplicationException {
    try {
      FetchMetadataMessage message = new FetchMetadataMessage("Server", "");

      MTable t = this.tableFacade.getTable(itc.getTableid());
      List<TupleToFile> ttfList = this.tupletofileFacade.getTuplesByInode(itc.
              getInodeid());

      List<Field> fields = t.getFields();

      for (Field field : fields) {
        /*
         * Load raw data based on the field id. Keep only data related to
         * a specific inode
         */
        List<RawData> rawList = field.getRawData();
        List<RawData> toKeep = new LinkedList<>();

        for (RawData raw : rawList) {
          for (TupleToFile ttf : ttfList) {

            if (raw.getTupleid() == ttf.getId()) {
              toKeep.add(raw);
            }
          }
        }
        field.setRawData(toKeep);
      }

      List<MTable> tables = new LinkedList<>();
      tables.add(t);
      String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) tables);
      message.setMessage(jsonMsg);

      return message;
    } catch (DatabaseException e) {
      throw new ApplicationException("Failed to fetch metadata for inode " + itc,
              e);
    }
  }

  /**
   * Deletes a table and its corresponding child entities (fields). First
   * removes any corresponding tuples from meta_tuple_to_file table
   * <p>
   * @param table
   * @throws se.kth.meta.exception.ApplicationException
   */
  public void checkDeleteTable(MTable table) throws ApplicationException {
    try {
      MTable t = this.tableFacade.contains(table) ? table : this.tableFacade.
              getTable(table.getId());
      List<Field> fields = t.getFields();

      for (Field f : fields) {
        this.checkDeleteField(f);
      }
      this.utils.deleteTable(t);
    } catch (DatabaseException e) {
      throw new ApplicationException("Failed to delete table " + table, e);
    }
  }

  /**
   * Deletes a field and its corresponding child entities (rawData). First
   * removes any corresponding tuples from meta_tuple_to_file table
   * <p>
   * @param field
   * @throws se.kth.meta.exception.ApplicationException
   */
  public void checkDeleteField(Field field) throws ApplicationException {
    try {
      Field f = this.fieldFacade.contains(field) ? field : this.fieldFacade.
              getField(field.getId());

      List<RawData> raw = f.getRawData();
      List<Integer> rawdataAsTuple = this.groupByTupleid(raw);

      //remove the child entities first
      this.fieldFacade.deleteField(field);

      for (Integer id : rawdataAsTuple) {
        //get the changed tupletofile object from the database
        TupleToFile ttf = this.tupletofileFacade.getTupletofile(id);
        this.checkDeleteTupleToFile(ttf);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException("Failed to delete field " + field, e);
    }
  }

  /**
   * A tuple consists of one or more instantiated raw data objects. If a raw
   * data object is partially deleted (i.e. delete only one instantiated field
   * out of many) the tuple should not be removed from the database.
   * <p>
   * @param ttf
   * @throws ApplicationException
   */
  private void checkDeleteTupleToFile(TupleToFile ttf) throws
          ApplicationException {
    try {
      List<RawData> rawlist = ttf.getRawData();
      /*
       * remove a tuple if and only if all the raw data entries composing this
       * tuple have been removed
       */
      if (rawlist.isEmpty()) {
        this.tupletofileFacade.deleteTTF(ttf);
      }
    } catch (DatabaseException e) {
      throw new ApplicationException("Failed to delete tuple " + ttf, e);
    }
  }

  /**
   * Checks if a table contains fields. This is necessary when the user wants to
   * delete a table
   * <p>
   * @param table
   * @return
   * @throws se.kth.meta.exception.ApplicationException
   */
  public Message checkTableFields(MTable table) throws ApplicationException {
    try {
      TextMessage message = new TextMessage("Server");

      MTable t = this.tableFacade.getTable(table.getId());
      List<Field> fields = t.getFields();

      String msg = fields.size() > 0 ? "This table contains fields" : "EMPTY";
      message.setMessage(msg);

      return message;
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, "Could not retrieve table " + table.getId()
              + ". ", e);
      throw new ApplicationException("Failed to check existence for " + table, e);
    }
  }

  public Message checkFieldContents(Field field) throws ApplicationException {
    try {
      TextMessage message = new TextMessage("Server");

      Field f = this.fieldFacade.getField(field.getId());
      List<RawData> rawdata = f.getRawData();

      String msg = rawdata.size() > 0 ? "This field contains raw data" : "EMPTY";
      message.setMessage(msg);

      return message;
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, "Could not retrieve field " + field.getId()
              + ".", e);
      throw new ApplicationException("Failed to check contents of " + field, e);
    }
  }

  /**
   * Takes a list of raw data values and reduces it to a list of grouped tuple
   * ids. Equivalent to sql's GROUP BY operator
   * <p>
   * @param list
   * @return
   */
  private List<Integer> groupByTupleid(List<RawData> list) {
    Map<Integer, List<RawData>> grouped = new HashMap<>();
    for (RawData raw : list) {
      grouped.put(raw.getTupleid(), null);
    }
    return new LinkedList<>(grouped.keySet());
  }
}
