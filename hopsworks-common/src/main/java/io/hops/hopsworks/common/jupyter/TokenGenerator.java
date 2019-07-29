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

package io.hops.hopsworks.common.jupyter;

import java.security.SecureRandom;
import java.util.Random;

public class TokenGenerator {
  
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
  
  private static ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new SecureRandom());
  
  public static String generateToken(int length) {
    StringBuilder token = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      token.append(ALPHABET.charAt(random.get().nextInt(ALPHABET.length())));
    }
    return token.toString();
  }
}
