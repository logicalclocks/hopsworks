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

import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.helper.AuditAction;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotated class or method will be audited.
 * Needs @AuditTarget annotation to identify the target user of the operation being audited.
 *
 * Default audit type is: AuditType.ACCOUNT_AUDIT
 * Default audit action is: AuditAction.ALL
 * Default audit message is: ""
 */

@InterceptorBinding
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@Repeatable(AuditedList.class)
public @interface Audited {
  @Nonbinding
  AuditType type() default AuditType.ACCOUNT_AUDIT;
  @Nonbinding
  AuditAction action() default AuditAction.ALL;
  @Nonbinding
  String message() default "";
}
