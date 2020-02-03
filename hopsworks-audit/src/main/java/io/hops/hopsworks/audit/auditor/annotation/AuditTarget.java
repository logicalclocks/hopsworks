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
package io.hops.hopsworks.audit.auditor.annotation;

import io.hops.hopsworks.audit.helper.UserIdentifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotated target user in method parameters to be audited. The annotated parameter should be a string user email or
 * a class that returns user email when toString() is called.
 * <pre>
 * {@code
 *   @Audited
 *   public void login(@AuditTarget String email, String password, ...) {
 *
 *   }
 * }
 * </pre>
 * If multiple annotated parameters are present only the first one will be set to be the target.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditTarget {
  
  UserIdentifier value() default UserIdentifier.EMAIL;
}
