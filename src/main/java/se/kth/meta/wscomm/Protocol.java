package se.kth.meta.wscomm;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.InodeTableComposite;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.RawData;
import se.kth.meta.exception.ApplicationException;
import se.kth.meta.wscomm.message.Command;
import se.kth.meta.wscomm.message.Message;
import se.kth.meta.wscomm.message.StoreMetadataMessage;
import se.kth.meta.wscomm.message.TextMessage;

/**
 * Constructs responses depending on the incoming message requests. Maintains
 * the communication with the front end.
 * <p>
 * @author Vangelis
 */
@Stateless(name = "protocol")
public class Protocol {

  private static final Logger logger = Logger.
          getLogger(Protocol.class.getName());

  @EJB
  private Utils utils;
  @EJB
  private ResponseBuilder builder;

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
        return this.builder.addNewTemplate(message);

      case REMOVE_TEMPLATE:
        return this.builder.removeTemplate(message);

      case STORE_FIELD:
      case EXTEND_TEMPLATE:
      case STORE_TEMPLATE:
        this.builder.storeSchema(message);
        //create and send the new schema back to everyone
        return this.builder.createSchema(message);

      case FETCH_TEMPLATE:
        //create and send the new schema back to everyone
        return this.builder.createSchema(message);

      case FETCH_TEMPLATES:
        return this.builder.fetchTemplates(message);

      case DELETE_TABLE:
        MTable table = (MTable) message.parseSchema().get(0);
        this.builder.checkDeleteTable(table);
        return this.builder.createSchema(message);

      case DELETE_FIELD:
        Field field = ((MTable) message.parseSchema().get(0)).getFields().
                get(0);
        this.builder.checkDeleteField(field);
        return this.builder.createSchema(message);

      case FETCH_METADATA:
        //need to fetch the metadata a table carries for a specific inode
        //joining two different entities
        InodeTableComposite itc = (InodeTableComposite) message.parseSchema().
                get(0);
        return this.builder.fetchInodeMetadata(itc);

      case FETCH_TABLE_METADATA:
        table = (MTable) message.parseSchema().get(0);
        return this.builder.fetchTableMetadata(table);

      case FETCH_FIELD_TYPES:
        return this.builder.fetchFieldTypes(message);

      //saves the actual metadata
      case STORE_METADATA:
        List<EntityIntf> composite = ((StoreMetadataMessage) message).
                superParseSchema();
        List<EntityIntf> rawData = message.parseSchema();
        this.utils.storeMetadata(composite, rawData);
        return new TextMessage("Server", "Metadata was stored successfully");

      case BROADCAST:
      case TEST:
      case QUIT:
        return new TextMessage(message.getSender(), message.getMessage());

      case IS_TABLE_EMPTY:
        table = (MTable) message.parseSchema().get(0);
        return this.builder.checkTableFields(table);

      case IS_FIELD_EMPTY:
        field = (Field) message.parseSchema().get(0);
        return this.builder.checkFieldContents(field);

      case UPDATE_METADATA:
        RawData raw = (RawData) message.parseSchema().get(0);
        this.utils.updateMetadata(raw);
        return new TextMessage("Server", "Raw data was updated successfully");
    }

    return new TextMessage();
  }
}
