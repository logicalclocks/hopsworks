/*
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
 *
 */

package io.hops.hopsworks.api.metadata.wscomm.message;

import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

public class TextMessage extends PlainMessage {

  private static final Logger logger = Logger.
          getLogger(TextMessage.class.getName());

  private final String TYPE = "TextMessage";
  protected String sender;
  protected String message;
  protected String action;
  protected String status;

  /**
   * Default constructor. Used by the class loader to create an instance of
   * this class
   */
  public TextMessage() {
    this.status = "OK";
  }

  /**
   * Used to send custom messages
   *
   * @param sender the message sender
   */
  public TextMessage(String sender) {
    this();
    this.sender = sender;
  }

  public TextMessage(String sender, String message) {
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
