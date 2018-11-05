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

package io.hops.hopsworks.api.metadata.wscomm.message;

import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.Field;
import io.hops.hopsworks.common.dao.metadata.FieldPredefinedValue;
import io.hops.hopsworks.common.dao.metadata.FieldType;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.RESTCodes;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Represents a metadata schema message
 * <p>
 */
public class TemplateMessage extends ContentMessage {

  private static final Logger LOGGER = Logger.getLogger(TemplateMessage.class.
          getName());

  private final String TYPE = "TemplateMessage";

  public TemplateMessage() {
    this.status = "OK";
    this.action = "fetch_template";
  }

  public TemplateMessage(String sender) {
    this();
    this.sender = sender;
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    this.setStatus("OK");
    this.setAction(this.action);

    //when asking for a template names list, tempid is null
    try {
      JsonObject object = Json.createReader(new StringReader(this.message)).
              readObject();
      super.setTemplateid(object.getInt("tempid"));
    } catch (NullPointerException e) {
      // TODO: - Never catch a NPE!! Re-write.
      LOGGER.log(Level.SEVERE, "Error while retrieving the templateid."
              + " Probably fetching the templates");
    }
  }

  @Override
  public void setAction(String action) {
    super.setAction(action);
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build() //build the actual json structure
            .toString();

    return value;
  }

  /**
   * Returns the template object. It is used when creating a new template
   * deleting or when updating one. The message carries either the template name
   * or the template id depending on the desired action.
   *
   * @return the template to be added in the database
   */
  @Override
  public Template getTemplate() throws GenericException {
    Template temp;
    JsonObject object = Json.createReader(new StringReader(this.message)).readObject();
  
    switch (Command.valueOf(this.action.toUpperCase())) {
    
      case ADD_NEW_TEMPLATE:
        temp = new Template(-1, object.getString("templateName"));
        break;
      case REMOVE_TEMPLATE:
        temp = new Template(object.getInt("templateId"));
        break;
      case UPDATE_TEMPLATE_NAME:
        temp = new Template(object.getInt("templateId"), object.getString(
          "templateName"));
        break;
      default:
        throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ACTION, Level.FINE, "Action:" + this.action);
    }
  
    return temp;
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();
    JsonObject board = obj.getJsonObject("bd");

    List<EntityIntf> tlist = new LinkedList<>();

    //get the prospective tables
    JsonArray tables = board.getJsonArray("columns");

    //there is the case of the empty template
    if (tables == null) {
      return tlist;
    }

    int noofTables = tables.size();

    for (int i = 0; i < noofTables; i++) {
      JsonObject item = tables.getJsonObject(i);
      String tableName = item.getString("name");
      int tableId = item.getInt("id");

      /*
       * if a template is being extended, cancel the table id so that they are
       * reinserted and attached to the new template
       */
      if (Command.valueOf(this.action.toUpperCase()) == Command.EXTEND_TEMPLATE) {
        tableId = -1;
      }

      MTable table = new MTable(tableId, tableName);
      table.setTemplateid(super.getTemplateid());

      //get the table attributes/fields
      JsonArray fields = item.getJsonArray("cards");

      int fieldId = -1;
      String fieldName;
      boolean searchable = false;
      boolean required = false;
      String maxsize;
      String description;
      int fieldtypeid;
      int position;

      //retrieve the table fields/attributes
      for (int j = 0; j < fields.size(); j++) {

        try {
          JsonObject field = fields.getJsonObject(j);
          fieldId = field.getInt("id");

          /*
           * if a template is being extended, cancel the field id so that they
           * are reinserted and attached to the new template
           */
          if (Command.valueOf(this.action.toUpperCase())
                  == Command.EXTEND_TEMPLATE) {
            fieldId = -1;
          }

          fieldName = field.getString("title");
          searchable = field.getBoolean("find");
          required = field.getBoolean("required");
          maxsize = field.getJsonObject("sizefield").getString("value");
          description = field.getString("description");
          fieldtypeid = field.getInt("fieldtypeid");
          position = field.getInt("position");

          try {
            //just in case the user has entered shit
            Double.parseDouble(maxsize);
            //sanitize maxsize
            maxsize = (!"".equals(maxsize)) ? maxsize : "0";
          } catch (NumberFormatException e) {
            maxsize = "0";
          }

          Field f = new Field(fieldId, tableId, fieldName,
                  "VARCHAR(50)", Integer.parseInt(maxsize), searchable, required,
                  description, fieldtypeid, position);
          f.setFieldTypes(new FieldType(fieldtypeid));

          //get the predefined values of the field if it is a yes/no field or a dropdown list field
          if (fieldtypeid != 1) {

            JsonArray predefinedFieldValues = field.getJsonArray(
                    "fieldtypeContent");
            List<FieldPredefinedValue> ll = new LinkedList<>();

            for (JsonValue predefinedFieldValue : predefinedFieldValues) {

              JsonObject defaultt = Json.createReader(new StringReader(
                      predefinedFieldValue.toString())).readObject();
              String defaultValue = defaultt.getString("value");

              FieldPredefinedValue predefValue = new FieldPredefinedValue(-1,
                      f.getId(), defaultValue);
              //predefValue.setFields(field);
              ll.add(predefValue);
            }

            f.setFieldPredefinedValues(ll);
          }

          table.addField(f);

        } catch (NullPointerException e) {
          LOGGER.log(Level.SEVERE, null, e);
          searchable = false;
          required = false;
          
        }
      }
      tlist.add(table);
    }
    return tlist;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public void setMessage(String msg) {
    this.message = msg;
  }

  @Override
  public String getSender() {
    return this.sender;
  }

  @Override
  public void setSender(String sender) {
    this.sender = sender;
  }

  @Override
  public String getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "{\"sender\": \"" + this.sender + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"status\": \"" + this.status + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }

}
