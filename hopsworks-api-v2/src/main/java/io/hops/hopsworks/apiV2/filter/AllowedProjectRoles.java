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

package io.hops.hopsworks.apiV2.filter;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations that can be used to restrict users from accessing project methods
 * based on the role they have for that project.
 * For this annotation to work the method annotated should be a web service with
 * a path project/{id}/*.
 * if no role is specified the default will be OWNER only access
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedProjectRoles {

  /**
   * Allowed for everyone. This does not mean both roles it means literally
   * everyone
   */
  String ANYONE = AllowedRoles.ALL;
  /**
   * Allowed only to the owner
   */
  String DATA_OWNER = AllowedRoles.DATA_OWNER;
  /**
   * Allowed to contributors or members of the project. There is no hierarchy if
   * only this annotation is used only members will be granted access. So to
   * allow
   * owners and members use both.
   */
  String DATA_SCIENTIST = AllowedRoles.DATA_SCIENTIST;

  /**
   * Used to annotate methods that work with project resources
   * <p/>
   * @return allowed roles
   */
  String[] value() default {AllowedProjectRoles.DATA_OWNER};
}
