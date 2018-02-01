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

import io.hops.hopsworks.common.dao.metadata.DirPath;
import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

public class RenameDirMessage extends TextMessage {

  private final String TYPE = "RenameDirMessage";

  private static final Logger logger = Logger.
          getLogger(RenameDirMessage.class.getName());

  /**
   * Default constructor. Used by the class loader to create an instance of
   * this class
   */
  public RenameDirMessage() {
    super();
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   */
  public RenameDirMessage(String sender) {
    this();
    this.sender = sender;
  }

  public RenameDirMessage(String sender, String message) {
    this(sender);
    this.message = message;
  }

  @Override
  public void init(JsonObject json) {

    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
  }

  @Override
  public String encode() {

    if (this.sender == null || this.message == null) {
      this.sender = "Unknown";
      this.message = "Nothing to do";
    }

    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build()
            .toString();

    return value;
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> list = null;

    try {
      String path = obj.getString("path");

      DirPath dir = new DirPath(path);
      list = new LinkedList<>();
      list.add(dir);
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Inodeid path not present in the message");
    }

    return list;
  }

  @Override
  public String buildSchema(List<EntityIntf> list) {

    return "Inode renamed successfully";
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public void setSender(String sender) {
    this.sender = sender;
  }

  @Override
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String getSender() {
    return this.sender;
  }

  @Override
  public String getMessage() {
    return this.message;
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
