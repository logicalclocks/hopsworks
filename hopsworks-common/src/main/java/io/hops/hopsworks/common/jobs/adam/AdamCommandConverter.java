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

package io.hops.hopsworks.common.jobs.adam;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

@FacesConverter(value = "adamCommandConverter")
public class AdamCommandConverter implements Converter {

  @Override
  public Object getAsObject(FacesContext context, UIComponent component,
          String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return AdamCommand.getFromCommand(value);
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component,
          Object value) {
    if (value == null || "".equals(value)) {
      return "";
    }
    if (!(value instanceof AdamCommand)) {
      throw new ConverterException("Object " + value
              + " is not of the expected type AdamCommand.");
    }
    return ((AdamCommand) value).getCommand();
  }

}
