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

'use strict';
angular.module('hopsWorksApp')
        .controller('SearchResultCtrl', ['DelaService', '$routeParams', '$scope', '$rootScope', '$interval',
          function (DelaService, $routeParams, $scope, $rootScope, $interval) {
            var self = this;
            self.userContents = [];
            self.projectId = parseInt($routeParams.projectID, 10);
            self.results = [];
            var dataset = false;
            
            self.init = function(results) {
              self.results = results;
              getUserContents();
            };
            
            self.init_ds = function(results) {
              dataset = true;
              self.results = results;
              getUserContents();
            };
            
            var getUserContents = function () {
              if (!$rootScope.isDelaEnabled) {
                return;
              }
              var elementSummaries = [];
              if (dataset) {
                DelaService.getUserContents().then(function (success) {
                  self.userContents = success.data;
                  elementSummaries = getElementSummary();
                  if (self.results.files.length > 0 && self.userContents.length > 0) {
                    self.results.files.forEach(function (entry) {
                      checkProgress(entry, elementSummaries);
                    });
                  }
                }, function (error) {
                  console.log("Error getting user contents: ", error);
                });
              } else {
                DelaService.getUserContents().then(function (success) {
                  self.userContents = success.data;
                  elementSummaries = getElementSummary();
                  if (self.results.length > 0 && self.userContents.length > 0) {
                    self.results.forEach(function (entry) {
                      checkProgress(entry, elementSummaries);
                    });
                  }
                }, function (error) {
                  console.log("Error getting user contents: ", error);
                });
              };

            };

            var getUserContentsInterval;
            if ($rootScope.isDelaEnabled) {
              getUserContentsInterval = $interval(function () {
                getUserContents();
              }, 10000);
            }

            var getElementSummary = function () {
              var elementSummaries = [];
              //if we have a project id look only for summaries in a project
              if (self.projectId) {
                self.userContents.forEach(function (entry) {
                  if (self.projectId === entry.projectId) {
                    elementSummaries = entry.elementSummaries;
                  }
                });
              } else {
                self.userContents.forEach(function (entry) {
                  elementSummaries = elementSummaries.concat(entry.elementSummaries);
                });
              } 
              return elementSummaries;
            };
            
            var checkProgress = function (ds, elementSummaries) {
              var torrentId = ds.publicId;
              if (torrentId === undefined) {
                return;
              }

              elementSummaries.forEach(function(entry) {
                if (torrentId === entry.torrentId.val && 
                    (entry.torrentStatus === "DOWNLOADING" || 
                     entry.torrentStatus === "PREPARE_DOWNLOAD")) {
                  ds['downloading'] = true;
                  return;
                } else if (torrentId === entry.torrentId.val) {
                  ds['downloading'] = false;
                  ds['downloaded'] = true;
                  return;
                } else {
                  ds['downloading'] = false;
                  ds['downloaded'] = false;
                }
              });
              return;
            };

            $scope.$on("$destroy", function () {
              console.log("Destroy getUserContentsInterval.");
              if(getUserContentsInterval) {
                $interval.cancel(getUserContentsInterval);
              }
            });
          }]);


