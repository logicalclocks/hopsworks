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
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class MessageDecoder implements Decoder.Text<Message> {

  private static final Logger logger = Logger.getLogger(MessageDecoder.class.
          getName());

  @Override
  public Message decode(String textMessage) throws DecodeException {

    Message msg = null;
    JsonObject obj = Json.createReader(new StringReader(textMessage)).
            readObject();

    try {
      DecoderHelper helper = new DecoderHelper(obj);
      msg = helper.getMessage();
      msg.init(obj);
    } catch (ApplicationException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    return msg;
  }

  @Override
  public void init(final EndpointConfig ec) {
  }

  @Override
  public boolean willDecode(final String s) {
    return true;
  }

  @Override
  public void destroy() {
  }
}
