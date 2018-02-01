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

import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import javax.json.JsonObject;

public class DecoderHelper {

  private JsonObject json;

  public DecoderHelper(JsonObject json) {
    this.json = json;
  }

  /**
   * Instantiates a message object on the runtime based on the 'type' parameter
   * of the message
   * <p/>
   * @return the initialized Message object
   * <p/>
   * @throws ApplicationException
   */
  public Message getMessage() throws ApplicationException {

    Message msg = null;
    try {
      String message = this.json.getString("type");
      Class c = getClass().getClassLoader().loadClass(
              "io.hops.hopsworks.api.metadata.wscomm.message." + message);
      msg = (Message) c.newInstance();
    } catch (ClassNotFoundException | InstantiationException |
            IllegalAccessException e) {
      throw new ApplicationException(
              "Could not find the right class to initialize the message", e);
    }

    return msg;
  }
}
