package se.kth.meta.wscomm;

import java.util.LinkedList;
import se.kth.meta.db.Dbao;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Fields;
import se.kth.meta.entity.Tables;
import se.kth.meta.exception.ApplicationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.meta.entity.FieldTypes;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.Templates;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.DatabaseException;
import se.kth.meta.wscomm.message.Command;
import se.kth.meta.wscomm.message.ContentMessage;
import se.kth.meta.wscomm.message.ErrorMessage;
import se.kth.meta.wscomm.message.FieldTypesMessage;
import se.kth.meta.wscomm.message.Message;
import se.kth.meta.wscomm.message.MetadataMessage;
import se.kth.meta.wscomm.message.TextMessage;

/**
 *
 * @author Vangelis
 */
public class Protocol {

  private static final Logger logger = Logger.
          getLogger(Protocol.class.getName());

  private Dbao db;
  private Utils utils;

  public Protocol() {
  }

  public Protocol(Dbao db) {
    this.db = db;
    this.utils = new Utils(db);
    logger.log(Level.SEVERE, "Protocol initialized");
  }

  /**
   * Process an incoming message and create and send back the response
   * <p>
   * @param message the incoming message
   * <p>
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
        Tables table = (Tables) message.parseSchema().get(0);
        this.utils.deleteTable(table);

        return this.createSchema(message);

      case DELETE_FIELD:
        Fields field = ((Tables) message.parseSchema().get(0)).getFields().
                get(0);
        this.utils.deleteField(field);

        return this.createSchema(message);

      case FETCH_METADATA:
        table = (Tables) message.parseSchema().get(0);
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

    Templates template = cmsg.getTemplate();
    this.utils.addNewTemplate(template);

    return this.fetchTemplates(message);
  }

  private Message removeTemplate(Message message) throws ApplicationException {
    ContentMessage cmsg = (ContentMessage) message;

    Templates template = cmsg.getTemplate();
    this.utils.removeTemplate(template);

    return this.fetchTemplates(message);
  }

  private void storeSchema(Message message) throws ApplicationException {
    List<EntityIntf> schema = message.parseSchema();
    this.utils.addTables(schema);
  }

  private Message fetchTemplates(Message message) {

    List<Templates> templates = this.db.loadTemplates();

    String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) templates);
    message.setMessage(jsonMsg);

    return message;
  }

  private Message fetchFieldTypes(Message message) {

    List<FieldTypes> ftypes = this.db.loadFieldTypes();

    FieldTypesMessage newMsg = new FieldTypesMessage();

    String jsonMsg = newMsg.buildSchema((List<EntityIntf>) (List<?>) ftypes);

    newMsg.setSender(message.getSender());
    newMsg.setMessage(jsonMsg);

    return newMsg;
  }

  private Message createSchema(Message message) {

    ContentMessage cmsg = (ContentMessage) message;

    List<Tables> tables = this.db.loadTemplateContent(cmsg.getTemplateid());

    String jsonMsg = cmsg.buildSchema((List<EntityIntf>) (List<?>) tables);
    message.setMessage(jsonMsg);

    return message;
  }

  private Message fetchTableMetadata(Tables table) {

    try {
      MetadataMessage message = new MetadataMessage("Server", "");
      Tables t = this.db.getTable(table.getId());

      List<Fields> fields = t.getFields();
      for (Fields field : fields) {
        List<RawData> raw = field.getRawData();

        for (RawData rawdata : raw) {
          TupleToFile ttf = this.db.getTupletofile(rawdata.getTupleid());
          rawdata.setInodeid(ttf.getInodeid());
        }
      }

      List<Tables> tables = new LinkedList<>();
      tables.add(t);
      String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) tables);
      //System.out.println("JSONMSG " + jsonMsg);

      message.setMessage(jsonMsg);

      return message;

    } catch (DatabaseException e) {
      return new ErrorMessage("Server", e.getMessage());
    }
  }
}
