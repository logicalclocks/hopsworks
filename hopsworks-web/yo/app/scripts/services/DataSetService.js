/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .factory('DataSetService', ['$http', function ($http) {
            var getQuery = function (query, queryName, first) {
                var c = typeof first === "undefined" || first === false? '&' : '?';
                return typeof query === "undefined"? '': c + queryName + '=' + query;
            };
            return function (id) {
              var baseUrl = '/api/project/' + id + '/dataset/';
              var services = {
                /**
                 * Get the listing of all datasets under the current project.
                 * @returns {unresolved}
                 */
                getAllDatasets: function (path, offset, limit, sortBy, filterBy, type) {
                  var datasetPath = typeof path === "undefined"? '': path;
                  var datasetType = getQuery(type, 'type');
                  var lim = getQuery(limit, 'limit');
                  var off = getQuery(offset, 'offset');
                  var sort = typeof sortBy === "undefined" || sortBy.length < 1? '':'&sort_by=';
                  var filter = typeof filterBy === "undefined" || filterBy.length < 1? '':'&filter_by=';
                  var sortLen = typeof sortBy === "undefined" || sortBy.length < 1? 0 : sortBy.length;
                  var filterLen = typeof filterBy === "undefined" || filterBy.length < 1? 0 : filterBy.length;
                  for (var i = 0; i < sortLen; i++) {
                      sort = sort + sortBy[i];
                      if (i+1 < sortLen) {
                          sort = sort + ',';
                      }
                  }
                  for (var j = 0; j < filterLen; j++) {
                      filter = filter + filterBy[j];
                      if (j+1 < filterLen) {
                          filter = filter + '&filter_by='
                      }
                  }
                  return $http.get(baseUrl + datasetPath + '?action=listing&expand=inodes' + off + lim + sort + filter + datasetType);
                },
                getDatasetStat: function (path, type) {
                  var datasetType = getQuery(type, 'type');
                  return $http.get(baseUrl + path + '?action=stat&expand=inodes' + datasetType);
                },
                getDatasetBlob: function (path, mode, type) {
                  var datasetType = getQuery(type, 'type');
                  var previewMode = getQuery(mode, 'mode');
                  return $http.get(baseUrl + path + '?action=blob&expand=inodes' + datasetType + previewMode);
                },
                create: function (path, templateId, description, searchable, generateReadme, type) {
                  var template = getQuery(templateId, 'templateId');
                  var description = getQuery(description, 'description');
                  var searchable = getQuery(searchable, 'searchable');
                  var generateReadme = getQuery(generateReadme, 'generate_readme');
                  var datasetType = getQuery(type, 'type');
                  return $http.post(baseUrl + path + '?action=create' + template + description + searchable + generateReadme + datasetType);
                },
                copy: function (path, destinationPath) {
                  var destinationPath = getQuery(destinationPath, 'destination_path');
                  return $http.post(baseUrl + path + '?action=copy' + destinationPath);
                },
                move: function (path, destinationPath) {
                  var destinationPath = getQuery(destinationPath, 'destination_path');
                  return $http.post(baseUrl + path + '?action=move' + destinationPath);
                },
                share: function (path, targetProject) {
                  var targetProject = getQuery(targetProject, 'target_project');
                  return $http.post(baseUrl + path + '?action=share' + targetProject);
                },
                accept: function (path) {
                  return $http.post(baseUrl + path + '?action=accept');
                },
                reject: function (path) {
                  return $http.post(baseUrl + path + '?action=reject');
                },
                zip: function (path) {
                  return $http.post(baseUrl + path + '?action=zip');
                },
                unzip: function (path) {
                  return $http.post(baseUrl + path + '?action=unzip');
                },
                permissions: function (path, permissions) {
                  return $http.put(baseUrl + path + '?action=permission' + '&permissions=' + permissions);
                },
                updateDescription: function (path, description) {
                  return $http.put(baseUrl + path + '?action=description' + '&description=' + description);
                },
                delete: function (path) {
                  return $http.delete(baseUrl + path);
                },
                deleteCorrupted: function (path) {
                  return $http.delete(baseUrl + path + '?action=corrupted');
                },
                unshare: function (path, targetProject) {
                  var targetProject = getQuery(targetProject, 'target_project');
                  return $http.delete(baseUrl + path + '?action=unshare' + targetProject);
                },
                getDownloadToken: function (path, type) {
                  var datasetType = getQuery(type, 'type', true);
                  return $http.get(baseUrl + 'download/token/' + path + datasetType);
                },
                download: function (path, token, type) {
                  var datasetType = getQuery(type, 'type');
                  location.href=getPathname() + baseUrl + 'download/' + path + '?token=' + token + datasetType;
                }
              };
              return services;
            };
          }]);
