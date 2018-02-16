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

package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.jobs.MutableJsonObject;

/**
 * Signifies that this object can be translated in a more compact JSON format.
 * Mainly used to store JSON objects in the DB.
 * <p/>
 */
public interface JsonReduceable {

  /**
   * Get the contents of this instance in a compact JSON format.
   * <p/>
   * @return
   */
  public MutableJsonObject getReducedJsonObject();

  /**
   * Update the contents of the current object from the given JSON object.
   * <p/>
   * @param json
   * @throws IllegalArgumentException If the given JSON object cannot be
   * converted to the current class.
   */
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException;
}
