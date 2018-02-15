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
package io.hops.hopsworks.common.security;

/**
 * Exception thrown in {@link CertificateMaterializer} when operating on remote material and cannot acquire a lock in
 * the database
 */
public class AcquireLockException extends Exception {
  public AcquireLockException() {
    super();
  }
  
  public AcquireLockException(String message) {
    super(message);
  }
  
  public AcquireLockException(Throwable cause) {
    super(cause);
  }
  
  public AcquireLockException(String message, Throwable cause) {
    super(message, cause);
  }
}
