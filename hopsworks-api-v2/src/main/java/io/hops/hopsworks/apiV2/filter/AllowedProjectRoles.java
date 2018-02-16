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
