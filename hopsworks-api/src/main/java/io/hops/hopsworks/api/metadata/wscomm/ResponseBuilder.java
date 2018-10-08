/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.metadata.wscomm;

import io.hops.hopsworks.api.metadata.wscomm.message.ContentMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.FetchMetadataMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.FetchTableMetadataMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.FieldTypeMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.api.metadata.wscomm.message.TemplateMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.TextMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.UploadedTemplateMessage;
import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.Field;
import io.hops.hopsworks.common.dao.metadata.FieldType;
import io.hops.hopsworks.common.dao.metadata.InodeTableComposite;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.RawData;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.TupleToFile;
import io.hops.hopsworks.common.dao.metadata.db.FieldFacade;
import io.hops.hopsworks.common.dao.metadata.db.FieldTypeFacade;
import io.hops.hopsworks.common.dao.metadata.db.MTableFacade;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.metadata.db.TupleToFileFacade;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.MetadataException;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Helper class to assist Protocol in constructing responses. Keeps Protocol
 * clean
 */
@Stateless
public class ResponseBuilder {

  private static final Logger logger = Logger.
          getLogger(ResponseBuilder.class.getName());

  @EJB
  private MetadataController metadataController;
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
   * <p/>
   * @param message
   * @return
   */
  public Message addNewTemplate(Message message) throws MetadataException, GenericException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.metadataController.addNewTemplate(template);

    return this.fetchTemplates(message);
  }

  /**
   * Removes a template from the database
   * <p/>
   * @param message
   * @return
   */
  public Message removeTemplate(Message message) throws GenericException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.metadataController.removeTemplate(template);

    return this.fetchTemplates(message);
  }

  /**
   * Updates the name of a given template
   * <p/>
   * @param message
   * @return
   */
  public Message updateTemplateName(Message message) throws MetadataException, GenericException {
    ContentMessage cmsg = (ContentMessage) message;

    Template template = cmsg.getTemplate();
    this.metadataController.updateTemplateName(template);

    TextMessage response = new TextMessage("Server");
    response.setMessage("Template updated successfully");

    return response;
  }

  /**
   * Persists a whole metadata template (schema) in the database. The message
   * contains all the tables and fields this template contains
   * <p/>
   * @param message
   */
  public void storeSchema(Message message) {
    List<EntityIntf> schema = message.parseSchema();
    this.metadataController.addTables(schema);
  }

  /**
   * Retrieves all the templates (template names) from the database
   * <p/>
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
   * <p/>
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
   * <p/>
   * @param table
   * @return
   */
  public Message fetchTableMetadata(MTable table) {
  
    FetchTableMetadataMessage message
      = new FetchTableMetadataMessage("Server", "");
    MTable t = this.tableFacade.getTable(table.getId());
  
    List<MTable> tables = new LinkedList<>();
    tables.add(t);
    String jsonMsg = message.buildSchema((List<EntityIntf>) (List<?>) tables);
    message.setMessage(jsonMsg);
  
    return message;
  }

  /**
   * Fetches the metadata a metadata table carries ONLY for a specific inodeid
   *
   * @param itc
   * @return
   */
  public Message fetchInodeMetadata(InodeTableComposite itc) {
  
    FetchMetadataMessage message = new FetchMetadataMessage("Server", "");
  
    MTable t = this.tableFacade.getTable(itc.getTableid());
    List<TupleToFile> ttfList = this.tupletofileFacade.getTuplesByInodeId(itc.
      getInodePid(), itc.getInodeName());
  
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
  }

  /**
   * Deletes a table and its corresponding child entities (fields). First
   * removes any corresponding tuples from meta_tuple_to_file table
   * <p/>
   * @param table
   */
  public void checkDeleteTable(MTable table) {
    MTable t = this.tableFacade.contains(table) ? table : this.tableFacade.
      getTable(table.getId());
    List<Field> fields = t.getFields();
  
    for (Field f : fields) {
      this.checkDeleteField(f);
    }
  
    this.metadataController.deleteTable(t);
  }

  /**
   * Deletes a field and its corresponding child entities (rawData). First
   * removes any corresponding tuples from meta_tuple_to_file table
   * <p/>
   * @param field
   */
  public void checkDeleteField(Field field) {
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
  }

  /**
   * A tuple consists of one or more instantiated raw data objects. If a raw
   * data object is partially deleted (i.e. delete only one instantiated field
   * out of many) the tuple should not be removed from the database.
   * <p/>
   * @param ttf
   */
  private void checkDeleteTupleToFile(TupleToFile ttf) {
  
    List<RawData> rawlist = ttf.getRawData();
  
    /*
     * remove a tuple if and only if all the raw data entries composing this
     * tuple have been removed
     */
    if (rawlist.isEmpty()) {
      this.tupletofileFacade.deleteTTF(ttf);
    }
  }

  /**
   * Checks if a table contains fields. This is necessary when the user wants to
   * delete a table
   * <p/>
   * @param table
   * @return
   */
  public Message checkTableFields(MTable table) {
  
    TextMessage message = new TextMessage("Server");
  
    MTable t = this.tableFacade.getTable(table.getId());
    List<Field> fields = t.getFields();
  
    String msg = fields.size() > 0 ? "This table contains fields" : "EMPTY";
    message.setMessage(msg);
  
    return message;
  }

  /**
   * Checks if a fields is associated (contains) raw data (metadata) in the
   * database. This is necessary when the user wants to delete a field
   * <p/>
   * @param field
   * @return
   */
  public Message checkFieldContents(Field field) {
  
    TextMessage message = new TextMessage("Server");
  
    Field f = this.fieldFacade.getField(field.getId());
    List<RawData> rawdata = f.getRawData();
  
    String msg = rawdata.size() > 0 ? "This field contains raw data" : "EMPTY";
    message.setMessage(msg);
  
    return message;
  }

  /**
   * Takes a list of raw data values and reduces it to a list of grouped tuple
   * ids. Equivalent to sql's GROUP BY operator
   * <p/>
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
   * <p/>
   * @param message
   */
  public void persistUploadedTemplate(UploadedTemplateMessage message) throws GenericException, MetadataException {

    //compile the message
    message.parseSchema();
    TemplateMessage tmplMsg = (TemplateMessage) message.addNewTemplateMessage();
    Template template = tmplMsg.getTemplate();

    int templateId = this.metadataController.addNewTemplate(template);

    tmplMsg = (TemplateMessage) message.addNewTemplateContentMessage(templateId);

    this.storeSchema(tmplMsg);
  }

}
