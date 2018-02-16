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

import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import java.util.LinkedList;
import java.util.List;
import javax.json.JsonObject;

public abstract class PlainMessage implements Message {

  @Override
  public void setAction(String action) {

  }

  @Override
  public String getAction() {
    return null;
  }

  @Override
  public void setStatus(String status) {
  }

  @Override
  public String getStatus() {
    return "No status";
  }

  @Override
  public void init(JsonObject obj) {
  }

  @Override
  public String encode() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public List<EntityIntf> parseSchema() {
    return new LinkedList<>();
  }

  @Override
  public String buildSchema(List<EntityIntf> tables) {
    return "PlainMessage.java";
  }

  @Override
  public abstract String getMessage();

  @Override
  public abstract void setMessage(String msg);

  @Override
  public abstract String getSender();

  @Override
  public abstract void setSender(String sender);

}
