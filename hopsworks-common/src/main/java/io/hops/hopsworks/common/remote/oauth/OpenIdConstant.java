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
package io.hops.hopsworks.common.remote.oauth;

public class OpenIdConstant {
  
  public static final String OPENID_CONFIGURATION_URL = "/.well-known/openid-configuration";

  public static final String RESPONSE_TYPE = "response_type";
  public static final String CLIENT_ID = "client_id";
  public static final String SCOPE = "scope";
  public static final String REDIRECT_URI = "redirect_uri";
  public static final String RESPONSE_MODE = "response_mode";
  public static final String STATE = "state";
  public static final String NONCE = "nonce";
  public static final String CODE = "code";

  public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
  public static final String TOKEN_ENDPOINT = "token_endpoint";
  public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
  public static final String REVOCATION_ENDPOINT = "revocation_endpoint";
  public static final String REGISTRATION_ENDPOINT = "registration_endpoint";
  public static final String JWKS_URI = "jwks_uri";
  
  public static final String ISSUER = "issuer";
  public static final String SCOPES_SUPPORTED = "scopes_supported";
  public static final String CLAIMS_SUPPORTED = "claims_supported";
  public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
  public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
  public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
  public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
  public static final String RESPONSE_MODES_SUPPORTED = "response_modes_supported";
  public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
  public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED =
    "token_endpoint_auth_signing_alg_values_supported";
  public static final String DISPLAY_VALUES_SUPPORTED = "display_values_supported";
  public static final String CLAIM_TYPES_SUPPORTED = "claim_types_supported";
  public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
  public static final String SUBJECT_IDENTIFIER = "sub";
  
  public static final String OPENID_SCOPE = "openid";
  public static final String PROFILE_SCOPE = "profile";
  public static final String EMAIL_SCOPE = "email";
  public static final String ROLES_SCOPE = "roles";
  public static final String GROUPS_SCOPE = "groups";

  public static final String NAME = "name";
  public static final String FAMILY_NAME = "family_name";
  public static final String GIVEN_NAME = "given_name";
  public static final String MIDDLE_NAME = "middle_name";
  public static final String NICKNAME = "nickname";
  public static final String PREFERRED_USERNAME = "preferred_username";
  public static final String GROUPS = "groups";
  public static final String ROLES = "roles";
  public static final String PROFILE = "profile";
  public static final String PICTURE = "picture";
  public static final String WEBSITE = "website";
  public static final String GENDER = "gender";
  public static final String BIRTHDATE = "birthdate";
  public static final String ZONEINFO = "zoneinfo";
  public static final String LOCALE = "locale";
  public static final String UPDATED_AT = "updated_at";
  public static final String EMAIL = "email";
  public static final String EMAIL_VERIFIED = "email_verified";
  public static final String ADDRESS = "address";
}
