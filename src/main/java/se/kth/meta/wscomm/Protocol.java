package se.kth.meta.wscomm;

import java.util.LinkedList;
import java.util.List;
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
import se.kth.meta.entity.FieldType;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.Template;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.exception.DatabaseException;
import se.kth.meta.wscomm.message.Command;
import se.kth.meta.wscomm.message.ContentMessage;
import se.kth.meta.wscomm.message.ErrorMessage;
import se.kth.meta.wscomm.message.FieldTypeMessage;
import se.kth.meta.wscomm.message.Message;
import se.kth.meta.wscomm.message.MetadataMessage;
import se.kth.meta.wscomm.message.TableMetadataMessage;
import se.kth.meta.wscomm.message.TextMessage;

/**
 *
 * @author Vangelis
 */
@Stateless(name = "protocol")
public class Protocol {

  private static final Logger logger = Logger.
          getLogger(Protocol.class.getName());

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

  public Protocol() {
    logger.log(Level.INFO, "Protocol initialized");
  }

  /**
   * Process an incoming message and create and send back the response
   *
   * @param message the incoming message
   * @return a new response message or an error message
   */
  public Message GFR(Message message) {

    Message msg;
    try {
      msg = this.processMessage(message);
    } catch (ApplicationException e) {
      TextMessage response = new TextMessage("Server", e.getMessage());
      response.setStatus("ERROR");
      return response;
    }
    return msg;
  }

  private Message processMessage(Message message) throws ApplicationException {

    Command action = Command.valueOf(message.getAction().toUpperCase());

    switch (action) {
      /*
       * saves either a metadata field, or a whole template schema to the
       * database
       */
      case ADD_NEW_TEMPLATE:
        return this.addNewTemplate(message);
      case REMOVE_TEMPLATE:
        return this.removeTemplate(message);
      case STORE_FIELD:
      case EXTEND_TEMPLATE:
      case STORE_TEMPLATE:
        this.storeSchema(message);
        //create and send the new schema back to everyone
        return this.createSchema(message);

      case FETCH_TEMPLATE:
        //create and send the new schema back to everyone
        return this.createSchema(message);

      case FETCH_TEMPLATES:
        return this.fetchTemplates(message);

      case DELETE_TABLE:
        MTable table = (MTable) message.parseSchema().get(0);
        this.utils.deleteTable(table);

        return this.createSchema(message);

      case DELETE_FIELD:
        Field field = ((MTable) message.parseSchema().get(0)).getFields().
                get(0);
        this.utils.deleteField(field);

        return this.createSchema(message);

      case FETCH_METADATA:
      //SHOULD BE REFACTORED TO RETURN TUPLE_TO_FILE AS ENTITY. NOT TABLE
//        table = (MTable) message.parseSchema().get(0);
//        //return this.fetchTableMetadata(table);
//        return this.fetchTableMetadataForInode(table, table.getInodeid());

      case FETCH_TABLE_METADATA:
        table = (MTable) message.parseSchema().get(0);
        return this.fetchTableMetadata(table);
      case FETCH_FIELD_TYPES:
        return this.fetchFieldTypes(message);

      //saves the actual metadata
      case STORE_METADATA:
        List<EntityIntf> schema = message.parseSchema();
        this.utils.storeMetadata(schema);

        return new TextMessage("Server", "Metadata was stored successfully");

      case BROADCAST:
      case TEST:
      case QUIT:
        return new TextMessage(message.getSender(), message.getMessage());
    }

    return new TextMessage();
  }

  private Message addNewTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.addNewTemplate(template);

    return this.fetchTemplates(message);
  }

  private Message removeTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.utils.removeTemplate(template);

    return this.fetchTemplates(message);
  }

  private void storeSchema(Message message) throws ApplicationException {
    List<EntityIntf> schema = message.parseSchema();
    this.utils.addTables(schema);
  }

  private Message fetchTemplates(Message message) {

    List<Template> templates = this.templateFacade.loadTemplates();

    String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) templates);
    message.setMessage(jsonMsg);

    return message;
  }

  private Message fetchFieldTypes(Message message) {

    List<FieldType> ftypes = this.fieldTypeFacade.loadFieldTypes();

    FieldTypeMessage newMsg = new FieldTypeMessage();

    String jsonMsg = newMsg.buildSchema((List<EntityIntf>) (List<?>) ftypes);

    newMsg.setSender(message.getSender());
    newMsg.setMessage(jsonMsg);

    return newMsg;
  }

  private Message createSchema(Message message) {

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
   */
  private Message fetchTableMetadata(MTable table) {

    try {
      TableMetadataMessage message = new TableMetadataMessage("Server", "");
      MTable t = this.tableFacade.getTable(table.getId());

      List<Field> fields = t.getFields();
      for (Field field : fields) {
        /*
         * Load raw data based on the field id. Need to filter this further
         * according to tupleid and inodeid
         */
        List<RawData> raw = field.getRawData();

        for (RawData rawdata : raw) {
          TupleToFile ttf = this.tupletofileFacade.getTupletofile(rawdata.
                  getTupleid());
          rawdata.setInodeid(ttf.getInodeid());
        }
      }

      List<MTable> tables = new LinkedList<>();
      tables.add(t);
      String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) tables);

      message.setMessage(jsonMsg);

      return message;

    } catch (DatabaseException e) {
      return new ErrorMessage("Server", e.getMessage());
    }
  }

  /**
   * Fetches the metadata a metadata table carries ONLY for a specific inodeid
   *
   * @param table
   * @return
   */
  private Message fetchTableMetadataForInode(MTable table, int inodeid) {

    try {
      MetadataMessage message = new MetadataMessage("Server", "");
      MTable t = this.tableFacade.getTable(table.getId());

      List<Field> fields = t.getFields();

      for (Field field : fields) {
        /*
         * Load raw data based on the field id. Need to filter this further
         * according to tupleid and inodeid
         */
        List<RawData> rawList = field.getRawData();
        List<RawData> toKeep = new LinkedList<>();

        for (RawData raw : rawList) {
          TupleToFile ttf = this.tupletofileFacade.getTupletofile(raw.
                  getTupleid());

          //keep only the data related to the specific inode
          if (ttf.getInodeid() == inodeid) {
            raw.setInodeid(ttf.getInodeid());
            toKeep.add(raw);
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
      return new ErrorMessage("Server", e.getMessage());
    }
  }
}
