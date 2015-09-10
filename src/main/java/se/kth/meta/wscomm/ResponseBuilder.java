package se.kth.meta.wscomm;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.meta.db.FieldFacade;
import se.kth.meta.db.FieldTypeFacade;
import se.kth.meta.db.MTableFacade;
import se.kth.meta.db.TemplateFacade;
import se.kth.meta.db.TupleToFileFacade;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.FieldType;
import se.kth.meta.entity.HdfsMetadataLog;
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
import se.kth.meta.wscomm.message.TemplateMessage;
import se.kth.meta.wscomm.message.TextMessage;
import se.kth.meta.wscomm.message.UploadedTemplateMessage;

/**
 * Helper class to assist Protocol in constructing responses. Keeps Protocol
 * clean
 * <p>
 * @author vangelis
 */
@Stateless
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

  public ResponseBuilder() {
    logger.log(Level.INFO, "ResponseBuilder initialized");
  }

  /**
   * Persists a new template in the database
   * <p>
   * @param message
   * @return
   * @throws ApplicationException
   */
  public Message addNewTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.addNewTemplate(template);

    return this.fetchTemplates(message);
  }

  /**
   * Removes a template from the database
   * <p>
   * @param message
   * @return
   * @throws ApplicationException
   */
  public Message removeTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.removeTemplate(template);

    return this.fetchTemplates(message);
  }

  /**
   * Updates the name of a given template
   * <p>
   * @param message
   * @return
   * @throws ApplicationException
   */
  public Message updateTemplateName(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.updateTemplateName(template);

    TextMessage response = new TextMessage("Server");
    response.setMessage("Template updated successfully");

    return response;
  }

  /**
   * Persists a whole metadata template (schema) in the database. The message
   * contains all the tables and fields this template contains
   * <p>
   * @param message
   * @throws ApplicationException
   */
  public void storeSchema(Message message) throws ApplicationException {
    List<EntityIntf> schema = message.parseSchema();
    this.utils.addTables(schema);
  }

  /**
   * Retrieves all the templates (template names) from the database
   * <p>
   * @param message
   * @return
   */
  public Message fetchTemplates(Message message) {

    List<Template> templates = this.templateFacade.loadTemplates();

    Collections.sort(templates);
    String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) templates);
    message.setMessage(jsonMsg);

    return message;
  }

  /**
   * Retrieves all the field types from the database
   * <p>
   * @param message
   * @return
   */
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
    Collections.sort(tables);

    String jsonMsg = cmsg.buildSchema((List<EntityIntf>) (List<?>) tables);
    message.setMessage(jsonMsg);

    return message;
  }

  /**
   * Fetches ALL the metadata a metadata table carries. It does not do any
   * filtering
   * <p>
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
      throw new ApplicationException("Server", e.getMessage());
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

            if (raw.getRawdataPK().getTupleid() == ttf.getId()) {
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
      throw new ApplicationException("Server", e.getMessage());
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
      throw new ApplicationException("Server", e.getMessage());
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
      throw new ApplicationException("Server", e.getMessage());
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
      throw new ApplicationException("Server", e.getMessage());
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
      throw new ApplicationException("Server", e.getMessage());
    }
  }

  /**
   * Checks if a fields is associated (contains) raw data (metadata) in the
   * database. This is necessary when the user wants to delete a field
   * <p>
   * @param field
   * @return
   * @throws ApplicationException
   */
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
      throw new ApplicationException("Server", e.getMessage());
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
      grouped.put(raw.getRawdataPK().getTupleid(), null);
    }

    return new LinkedList<>(grouped.keySet());
  }

  /**
   * Persists an uploaded template to the database. The template comes from an
   * uploaded file and contains the template name and all the template tables
   * and fields. First create a 'new template' command message and then a 'store
   * template content' command message
   * <p>
   * @param message
   * @throws se.kth.meta.exception.ApplicationException
   */
  public void persistUploadedTemplate(UploadedTemplateMessage message) throws
          ApplicationException {

    //compile the message
    message.parseSchema();
    TemplateMessage tmplMsg = (TemplateMessage) message.addNewTemplateMessage();
    Template template = tmplMsg.getTemplate();

    int templateId = this.utils.addNewTemplate(template);

    tmplMsg = (TemplateMessage) message.addNewTemplateContentMessage(templateId);

    this.storeSchema(tmplMsg);
  }

  /**
   * Creates an inode mutation by copying a folder from a given path to the
   * destination path. SHOULD change to mv, but mv fails for now
   * <p>
   * @param log
   * @return
   * @throws ApplicationException
   */
  public Message inodeMutationResponse(HdfsMetadataLog log) throws
          ApplicationException {

    int i = 0;
    double factor = 0.4;

    while (i < se.kth.bbc.lims.Constants.MAX_RETRIES) {
      try {
        this.sleep((int) (10 * factor));
        
        //write directly to hdfs_metadata_log table
        this.utils.createMetadataLog(log);
        return this.textResponse("Inode mutation was successful");
      } catch (DatabaseException ex) {

      } finally {
        i++;
        factor += 0.4;
      }
      log.getHdfsMetadataLogPK().setLtime(log.getHdfsMetadataLogPK().
              getLtime() + 1);
    }

    //flow control reached here, means the log was not persisted to database 
    throw new ApplicationException(
            "Inode log was not written to database. Max number of retries reached");
  }

  private Message textResponse(String message) {
    TextMessage response = new TextMessage();
    response.setSender("Server");
    response.setMessage(message);

    return response;
  }

  private void sleep(int mSec) {
    try {
      Thread.sleep(mSec);
    } catch (InterruptedException e) {

    }
  }
}
