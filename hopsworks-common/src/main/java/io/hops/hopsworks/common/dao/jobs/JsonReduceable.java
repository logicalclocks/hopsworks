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
