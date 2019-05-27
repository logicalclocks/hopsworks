/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.proxies;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.hops.hopsworks.exceptions.CAException;

import java.io.IOException;
import java.util.logging.Level;

public class CAExceptionDeserializer extends StdDeserializer<CAException> {
  
  public CAExceptionDeserializer() {
    this(null);
  }
  
  public CAExceptionDeserializer(Class<?> cls) {
    super(cls);
  }
  
  @Override
  public CAException deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    ObjectCodec oc = jsonParser.getCodec();
    JsonNode node = oc.readTree(jsonParser);
    
    final JsonNode usrMsgNode = node.get("usrMsg");
    final String usrMsg = usrMsgNode != null ? usrMsgNode.asText() : "";
    final JsonNode devMsgNode = node.get("devMsg");
    final String devMsg = devMsgNode != null ? devMsgNode.asText() : "";
    
    return new CAException(null, Level.SEVERE, usrMsg, devMsg, null);
  }
}
