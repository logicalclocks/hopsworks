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
package io.hops.hopsworks.api.auth;

import org.apache.commons.codec.digest.DigestUtils;

public class Secret {

  private static final String KEY_ID_SEPARATOR = ".";
  public static final String KEY_ID_SEPARATOR_REGEX = "\\.";
  private final String prefix;
  private final String secret;
  private final String salt;
  private final int prefixMinLength;
  private final int secretMinLength;
  private final int saltMinLength;
  private final boolean prefixed;

  public Secret(String prefix, String secret, String salt, int prefixMinLength, int secretMinLength,
      int saltMinLength) {
    this.prefix = prefix;
    this.secret = secret;
    this.salt = salt;
    this.prefixMinLength = prefixMinLength;
    this.secretMinLength = secretMinLength;
    this.saltMinLength = saltMinLength;
    this.prefixed = true;
  }

  public Secret(String prefix, String secret, String salt) {
    this.prefix = prefix;
    this.secret = secret;
    this.salt = salt;
    this.prefixMinLength = 0;
    this.secretMinLength = 0;
    this.saltMinLength = 0;
    this.prefixed = true;
  }

  public Secret(String secret, String salt, int secretMinLength, int saltMinLength) {
    this.prefix = "";
    this.secret = secret;
    this.salt = salt;
    this.prefixMinLength = 0;
    this.secretMinLength = secretMinLength;
    this.saltMinLength = saltMinLength;
    this.prefixed = false;
  }

  public Secret(String secret, String salt) {
    this.prefix = "";
    this.secret = secret;
    this.salt = salt;
    this.prefixMinLength = 0;
    this.secretMinLength = 0;
    this.saltMinLength = 0;
    this.prefixed = false;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getSecret() {
    return secret;
  }

  public String getSalt() {
    return salt;
  }

  public String getSecretPlusSalt() {
    return this.secret + this.salt;
  }

  public int getPrefixMinLength() {
    return prefixMinLength;
  }

  public int getSecretMinLength() {
    return secretMinLength;
  }

  public int getSaltMinLength() {
    return saltMinLength;
  }

  public boolean isPrefixed() {
    return prefixed;
  }

  public String getPrefixPlusSecret() {
    return this.prefix + KEY_ID_SEPARATOR + this.secret;
  }

  public String getSha256HexDigest() {
    String secPlusSalt = getSecretPlusSalt();
    return DigestUtils.sha256Hex(secPlusSalt);
  }

  public String getSha512HexDigest() {
    String secPlusSalt = getSecretPlusSalt();
    return DigestUtils.sha512Hex(secPlusSalt);
  }

  public String getSha1HexDigest() {
    String secPlusSalt = getSecretPlusSalt();
    return DigestUtils.sha1Hex(secPlusSalt);
  }

  public boolean validateNotNullOrEmpty() {
    if (this.prefixed) {
      return this.prefix != null && !this.prefix.isEmpty() && this.secret != null && !this.secret.isEmpty() &&
          this.salt != null && !this.salt.isEmpty();
    } else {
      return this.secret != null && !this.secret.isEmpty() && this.salt != null && !this.salt.isEmpty();
    }
  }

  public boolean validateSize() {
    boolean notNullOrEmpty = validateNotNullOrEmpty();
    if (this.prefixed) {
      return notNullOrEmpty && !(this.prefix.length() < prefixMinLength || this.secret.length() < secretMinLength ||
          this.salt.length() < saltMinLength);
    } else {
      return notNullOrEmpty && !(this.secret.length() < secretMinLength || this.salt.length() < saltMinLength);
    }
  }
}
