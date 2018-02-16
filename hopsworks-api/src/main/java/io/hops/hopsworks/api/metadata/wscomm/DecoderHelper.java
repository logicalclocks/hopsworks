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
