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
