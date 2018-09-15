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
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.exception.GenericException;

import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public abstract class ContentMessage implements Message {

  private static final Logger logger = Logger.
          getLogger(ContentMessage.class.getName());

  protected String TYPE;
  protected String sender;
  protected String message;
  protected String status;
  protected int templateId;
  protected String action;
  protected Template template;

  public ContentMessage() {
    this.status = "OK";
  }

  public void setTemplate(Template template) {
    this.template = template;
  }

  public Template getTemplate() throws GenericException {
    return this.template;
  }

  public void setTemplateid(int templateId) {
    this.templateId = templateId;
  }

  public int getTemplateid() {
    return this.templateId;
  }

  @Override
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  /**
   * Builds a JSON object that represents the front end board based on the
   * message action. It's the inverse of parseSchema()
   * <p/>
   *
   * @param entities Entities is a list of MTable objects
   * @return the schema as a JSON string
   */
  @Override
  public String buildSchema(List<EntityIntf> entities) {

    switch (Command.valueOf(this.action.toUpperCase())) {
      case STORE_FIELD:
      case DELETE_FIELD:
      case DELETE_TABLE:
      case STORE_TEMPLATE:
      case EXTEND_TEMPLATE:
      case FETCH_TEMPLATE:
        return this.templateContents(entities);
      case ADD_NEW_TEMPLATE:
      case REMOVE_TEMPLATE:
      case FETCH_TEMPLATES:
        return this.templateNames(entities);
      default:
        return "Unknown command";
    }
  }

  /**
   * Creates a json response with just the template names
   * <p/>
   * @param entities
   * @return
   */
  private String templateNames(List<EntityIntf> entities) {
    JsonObjectBuilder builder = Json.createObjectBuilder();

    builder.add("numberOfTemplates", entities.size());

    JsonArrayBuilder templates = Json.createArrayBuilder();
    for (EntityIntf e : entities) {

      Template te = (Template) e;
      JsonObjectBuilder template = Json.createObjectBuilder();

      template.add("id", te.getId());
      template.add("name", te.getName());
      templates.add(template);
    }

    builder.add("templates", templates);

    return builder.build().toString();
  }

  /**
   * Creates a json response with all the content a template carries
   * <p/>
   * @param entities
   * @return
   */
  private String templateContents(List<EntityIntf> entities) {
    JsonObjectBuilder builder = Json.createObjectBuilder();

    builder.add("name", "MainBoard");
    builder.add("numberOfColumns", 3 /*
     * entities.size()
     */);
    builder.add("templateId", this.templateId);

    JsonArrayBuilder columns = Json.createArrayBuilder();

    //create the columns/lists/tables of the board
    for (EntityIntf ta : entities) {

      MTable t = (MTable) ta;
      JsonObjectBuilder column = Json.createObjectBuilder();

      column.add("id", t.getId());
      column.add("name", t.getName());

      List<Field> fields = t.getFields();
      //array holding the cards
      JsonArrayBuilder cards = Json.createArrayBuilder();

      //create the column cards
      for (Field c : fields) {
        JsonObjectBuilder card = Json.createObjectBuilder();
        card.add("id", c.getId());
        card.add("title", c.getName());
        card.add("find", c.getSearchable());
        card.add("required", c.getRequired());
        card.add("description", c.getDescription());
        card.add("fieldtypeid", c.getFieldTypeId());
        card.add("position", c.getPosition());

        //"fieldtypeContent":[{"id":-1,"fieldid":-1,"value":"cc"},{"id":-1,
        //"fieldid":-1,"value":"ccc"},{"id":-1,"fieldid":-1,"value":"cccc"}]
        JsonObjectBuilder temp = Json.createObjectBuilder();
        temp.add("showing", false);
        temp.add("value", String.valueOf(c.getMaxsize()));
        card.add("sizefield", temp);

        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (FieldPredefinedValue value : c.getFieldPredefinedValues()) {
          JsonObjectBuilder obj = Json.createObjectBuilder();
          obj.add("id", value.getId());
          obj.add("fieldid", value.getFieldid());
          obj.add("value", value.getValue());
          arr.add(obj);
        }

        card.add("fieldtypeContent", arr);
        cards.add(card);
      }
      //add the cards to the column
      column.add("cards", cards);
      //add the current column to columns array
      columns.add(column);
    }

    //last but not least
    builder.add("columns", columns);

    return builder.build().toString();
  }

}
