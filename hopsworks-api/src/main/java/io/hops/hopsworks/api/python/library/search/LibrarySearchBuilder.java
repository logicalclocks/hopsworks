/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.python.library.search;

import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.python.library.LibraryVersionDTO;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.PythonDep;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibrarySearchBuilder {

  @EJB
  private LibraryController libraryController;
  
  public LibrarySearchDTO uri(LibrarySearchDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  public LibrarySearchDTO uri(LibrarySearchDTO dto, UriInfo uriInfo, String foundLibrary) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .queryParam("query", foundLibrary)
      .build());
    return dto;
  }
  
  public LibrarySearchDTO uri(LibrarySearchDTO dto, UriInfo uriInfo, String foundLibrary, String url) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .queryParam("query", foundLibrary)
      .queryParam("channel", url)
      .build());
    return dto;
  }
  
  public LibrarySearchDTO build(UriInfo uriInfo, List<LibraryVersionDTO> libVersions, String foundLibrary,
    Collection<PythonDep> installedDeps, String url) {
    LibrarySearchDTO dto = new LibrarySearchDTO();
    if (url != null) {
      uri(dto, uriInfo, foundLibrary, url);
    } else {
      uri(dto, uriInfo, foundLibrary);
    }
    dto.setLibrary(foundLibrary);
    dto.setVersions(libVersions);
    for (PythonDep pd : installedDeps) {
      if (pd.getDependency().equalsIgnoreCase(foundLibrary)) {
        dto.setStatus("Installed");
      }
    }
    return dto;
  }
  
  public LibrarySearchDTO buildCondaItems(UriInfo uriInfo, String library, Project project, String url)
    throws ServiceException {
    LibrarySearchDTO dto = new LibrarySearchDTO();
    uri(dto, uriInfo, library, url);
    dto.setLibrary(library);
    dto.setChannelUrl(url);
    HashMap<String, List<LibraryVersionDTO>> libVersions = libraryController.condaSearch(library, url);
    dto.setCount((long) libVersions.size());
    return buildItems(dto, uriInfo, libVersions, project, url);
  }
  
  public LibrarySearchDTO buildPipItems(UriInfo uriInfo, String library, Project project) throws ServiceException {
    LibrarySearchDTO dto = new LibrarySearchDTO();
    uri(dto, uriInfo, library);
    dto.setLibrary(library);
    HashMap<String, List<LibraryVersionDTO>> libVersions = libraryController.pipSearch(library, project);
    dto.setCount((long) libVersions.size());
    return buildItems(dto, uriInfo, libVersions, project, null);
  }
  
  private LibrarySearchDTO buildItems(LibrarySearchDTO dto, UriInfo uriInfo,
    HashMap<String, List<LibraryVersionDTO>> libVersions, Project project, String url) {
    Collection<PythonDep> installedDeps = project.getPythonDepCollection();
    if (libVersions != null && !libVersions.isEmpty()) {
      libVersions.forEach((k, v) -> dto.addItem(build(uriInfo, v, k, installedDeps, url)));
    }
    return dto;
  }
  
}
