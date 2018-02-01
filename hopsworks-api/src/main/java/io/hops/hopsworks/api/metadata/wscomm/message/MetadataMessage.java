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

package io.hops.hopsworks.api.metadata.wscomm.message;

import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.InodeTableComposite;
import io.hops.hopsworks.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Represents a generic metadata message. It may be about fetching table
 * metadata or inode metadata, or about updating
 * table metadata depending on its subclasses
 */
public class MetadataMessage implements Message {

  private static final Logger logger = Logger.
          getLogger(MetadataMessage.class.getName());

  protected String TYPE = "MetadataMessage";
  protected String sender;
  protected String message;
  protected String action;
  protected String status;

  public MetadataMessage() {
    this.status = "OK";
  }

  @Override
  public void init(JsonObject obj) {
    this.sender = obj.getString("sender");
    this.message = obj.getString("message");
    this.action = obj.getString("action");
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
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  /**
   * Instantiates the custom entity InodeTableComposite to pass table id and
   * inode id to protocol. This way raw data can
   * be filtered by table id and inode id and be grouped in the front end.
   *
   * @return the list with the RawData objects
   */
  @Override
  public List<EntityIntf> parseSchema() {
    List<EntityIntf> list = new ArrayList<>();
    InodeTableComposite itc = JsonUtil.parseSchemaHeader(this.message);
    if (itc != null) {
      list.add(itc);
    }
    return list;
  }

  @Override
  public String buildSchema(List<EntityIntf> entity) {
    throw new UnsupportedOperationException("Not necessary for this message.");
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

}
