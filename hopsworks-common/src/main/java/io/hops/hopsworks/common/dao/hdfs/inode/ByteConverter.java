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

package io.hops.hopsworks.common.dao.hdfs.inode;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 * Convert between MySQL bit(8) and Java byte.
 */
public class ByteConverter implements Converter {

  /**
   * Not tested
   *
   * @param o
   * @param sn
   * @return
   */
  @Override
  public Object convertObjectValueToDataValue(Object o, Session sn) {
    byte[] b = new byte[1];
    b[0] = (byte) o;
    return b;
  }

  @Override
  public Object convertDataValueToObjectValue(Object o, Session sn) {
    byte[] b = (byte[]) o;
    return b[0];
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public void initialize(DatabaseMapping dm, Session sn) {
    //Do nothing  
  }

}
