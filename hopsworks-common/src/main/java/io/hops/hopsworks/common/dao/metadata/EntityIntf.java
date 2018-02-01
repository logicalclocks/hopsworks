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

package io.hops.hopsworks.common.dao.metadata;

public interface EntityIntf {

  /**
   * Return the id of this entity
   * <p/>
   * @return the integer id
   */
  public abstract Integer getId();

  /**
   * Sets the id of this entity
   * <p/>
   * @param id the new id of this entity
   */
  public abstract void setId(Integer id);

  /**
   * Copies another entity into this one. Convenient when updating entities
   * through JPA
   * <p/>
   * @param entity the entity to be copied to the current one
   */
  public abstract void copy(EntityIntf entity);

}
