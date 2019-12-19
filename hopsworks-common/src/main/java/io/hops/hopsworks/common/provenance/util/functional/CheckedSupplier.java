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
package io.hops.hopsworks.common.provenance.util.functional;

import java.util.Objects;

/**
 * Functional interface that can also throw exceptions which can thus be handled properly
 * @param <R>
 * @param <E>
 */
@FunctionalInterface
public interface CheckedSupplier<R, E extends Exception> {
  R get() throws E;
  default <V> CheckedSupplier<V, E> andThen(CheckedFunction<? super R, ? extends V, ? extends E> after) {
    Objects.requireNonNull(after);
    return () -> after.apply(get());
  }
}
