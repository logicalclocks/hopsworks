/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.metadata.wscomm;

import io.hops.hopsworks.api.metadata.wscomm.message.Command;
import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.api.metadata.wscomm.message.RemoveMetadataMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.StoreMetadataMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.TextMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.UpdateMetadataMessage;
import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.Field;
import io.hops.hopsworks.common.dao.metadata.InodeTableComposite;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Constructs responses depending on the incoming message requests. Maintains
 * the communication with the front end.
 */
@Stateless(name = "metadataProtocol")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MetadataProtocol {

  private static final Logger logger = Logger.
          getLogger(MetadataProtocol.class.getName());

  @EJB
  private MetadataController metadataController;
  @EJB
  private ResponseBuilder builder;

  public MetadataProtocol() {
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

  /**
   * Receives a user message, translates it into the command it represents,
   * executes the command and sends back the produced response.
   * Commands 'store_metadata' and 'update_metadata' are always followed by
   * 'create_meta_log', and these two must be atomic. Hence the
   * TransactionAttribute annotation
   * <p/>
   * @param message
   * @return
   * @throws ApplicationException
   */
  private Message processMessage(Message message) throws ApplicationException {

    Command action = Command.valueOf(message.getAction().toUpperCase());

    switch (action) {
      case STORE_METADATA:
        return this.processStoreMetadataMessageCm(message);
      case UPDATE_METADATA:
        return this.processUpdateMetadataMessageCm(message);
      case REMOVE_METADATA:
        return this.processRemoveMetadataMessageCm(message);
      default:
        return processMessageNm(message);
    }
  }

  /**
   * Handles the case of attaching metadata to an inode. Action store metadata
   * has to be followed by an inode mutation (i.e. add an entry to
   * hdfs_metadata_log table) so that elastic rivers will pick up the already
   * indexed inode, but this time along with its attached metadata.
   * Those two actions have to be executed atomically (either all or nothing)
   * <p/>
   * @param message. The incoming message
   * @return
   * @throws ApplicationException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processStoreMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((StoreMetadataMessage) message).
            superParseSchema();

    //variables not set in the json message
    if (composite == null) {
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    List<EntityIntf> rawData = message.parseSchema();

    //Persist metadata
    this.metadataController.storeRawData(composite, rawData);

    return new TextMessage("Server");
  }

  /**
   * Handles the case of updating existing inode metadata. This action has to be
   * followed by an inode mutation (i.e. add an entry to hdfs_metadata_log
   * table) so that elastic rivers will pick up the already indexed inode
   * <p/>
   * @param message
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processUpdateMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((UpdateMetadataMessage) message).
            superParseSchema();

    if (composite == null) {
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    //update metadata
    Metadata metadata = (Metadata) message.parseSchema().get(0);
    this.metadataController.updateMetadata(composite, metadata.getMetadataPK().
            getId(), metadata.getData());

    return new TextMessage("Server");

  }

  /**
   * Handles the case of deleting existing inode metadata.
   * This will delete the metadata from the table only.
   * This will not remove the elastic index of this record.
   * <p/>
   * @param message
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processRemoveMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((RemoveMetadataMessage) message).
            superParseSchema();

    if (composite == null) {
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    //delete metadata
    Metadata metadata = (Metadata) message.parseSchema().get(0);
    this.metadataController.removeMetadata(composite, metadata.getMetadataPK().
            getId(), metadata.getData());

    return new TextMessage("Server");
  }

  /**
   * Processes incoming messages according to the command they carry, and
   * produces the appropriate message response
   * <p/>
   * @param message. The incoming message
   * @return
   * @throws ApplicationException
   */
  private Message processMessageNm(Message message) throws ApplicationException {

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

      case UPDATE_TEMPLATE_NAME:
        return this.builder.updateTemplateName(message);

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
    }

    return new TextMessage();
  }
}
