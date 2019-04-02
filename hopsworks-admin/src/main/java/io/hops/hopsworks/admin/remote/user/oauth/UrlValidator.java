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
package io.hops.hopsworks.admin.remote.user.oauth;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import java.net.URI;
import java.net.URISyntaxException;

@FacesValidator("io.hops.hopsworks.admin.remote.user.oauth.UrlValidator")
public class UrlValidator implements Validator {
  
  @Override
  public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
    String urlValue = value.toString();
    if (urlValue == null || urlValue.isEmpty()) {
      return;
    }
    validateUri(urlValue);
  }
  
  private void validateUri(String uriStr) {
    FacesMessage msg = new FacesMessage("URL validation failed", "Invalid URL format");
    msg.setSeverity(FacesMessage.SEVERITY_ERROR);
    try {
      URI uri = new URI(uriStr);
      if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
        throw new ValidatorException(msg);
      }
      if (uri.getHost() == null || uri.getHost().isEmpty()) {
        throw new ValidatorException(msg);
      }
    } catch (URISyntaxException e) {
      throw new ValidatorException(msg);
    }
  }
}
