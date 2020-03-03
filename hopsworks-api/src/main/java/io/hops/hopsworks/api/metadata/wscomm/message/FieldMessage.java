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
import io.hops.hopsworks.common.dao.metadata.MTable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class FieldMessage extends ContentMessage {

  private static final Logger logger = Logger.getLogger(FieldMessage.class.
          getName());

  public FieldMessage() {
    super();
    this.TYPE = "FieldMessage";
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
    super.setAction(this.action);

    try {
      JsonObject object = Json.createReader(new StringReader(this.message)).
              readObject();
      super.setTemplateid(object.getInt("tempid"));
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Error while retrieving the templateid", e);
    }
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build() //pretty necessary so as to build the actual json structure
            .toString();

    return value;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  /**
   * Returns a list with just one MTable object containing all its fields,
   * based on the contents of the JSON incoming message. It's the opposite of
   * buildSchema()
   *
   * @return the created schema
   */
  @Override
  public List<EntityIntf> parseSchema() {

    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    int fieldId = obj.getInt("id");
    int tableId = obj.getInt("tableid");
    String tableName = obj.getString("tablename");
    String name = obj.getString("name");
    String type = obj.getString("type");
    String maxsize = obj.getJsonObject("sizefield").getString("value");

    boolean searchable = obj.getBoolean("searchable");
    boolean required = obj.getBoolean("required");
    String description = obj.getString("description");
    int fieldtypeid = obj.getInt("fieldtypeid");
    int position = obj.getInt("position");

    try {
      /*
       * check the field type id. If the field type id is other than 1, that
       * means the field is either a single or multi selection dropdown list
       * so the number of characters this field should hold (size) doesn't
       * matter.
       * Only a text field applies a max characters limit
       */
      if (fieldtypeid != 1) {
        maxsize = "0";
      } else {
        //sanitize maxsize in case the user has entered shit
        maxsize = (!"".equals(maxsize)) ? maxsize : "0";
        Integer.parseInt(maxsize);
      }
    } catch (NumberFormatException e) {
      maxsize = "0";
    }

    Field field = new Field(fieldId, tableId, name, type,
            Integer.parseInt(maxsize), searchable, required,
            description, fieldtypeid, position);

    //-- ATTACH the field's parent entity (FieldType)
    field.setFieldTypeId(fieldtypeid);

    //-- ATTACH fieldtype's child entity (Field)
    //fieldtype.getFields().add(field);
    //get the predefined values of the field if it is a yes/no field or a dropdown list field
    if (fieldtypeid != 1) {

      JsonArray predefinedFieldValues = obj.getJsonArray("fieldtypeContent");
      List<FieldPredefinedValue> ll = new LinkedList<>();

      for (JsonValue predefinedFieldValue : predefinedFieldValues) {

        JsonObject defaultt = Json.createReader(new StringReader(
                predefinedFieldValue.toString())).readObject();
        String defaultValue = defaultt.getString("value");

        FieldPredefinedValue predefValue = new FieldPredefinedValue(-1, field.
                getId(), defaultValue);

        //-- ATTACH predefinedValue's parent entity (Field)
        //predefValue.setFieldid(field.getId());
        ll.add(predefValue);
      }

      //-- ATTACH the field's children entities (FieldPredefinedValue)
      field.setFieldPredefinedValues(ll);
    }

    MTable table = new MTable(tableId, tableName);
    table.setTemplateid(super.getTemplateid());

    //-- ATTACH the field's parent entity (MTable)
    field.setTableid(table.getId());
    //-- ATTACH the table's child entity (Field)
    table.addField(field);

    List<EntityIntf> list = new LinkedList<>();
    list.add(table);

    return list;
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
