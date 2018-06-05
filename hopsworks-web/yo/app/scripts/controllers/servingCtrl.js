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

/**
 * Controller for the serving page.
 */
'use strict';

angular.module('hopsWorksApp')
        .controller('servingCtrl', ['$scope', '$routeParams', 'growl', 'ServingService', '$location', 'ModalService', '$interval', 'StorageService', '$mdSidenav', 'DataSetService',
          function ($scope, $routeParams, growl, ServingService, $location, ModalService, $interval, StorageService, $mdSidenav, DataSetService) {

            var self = this;

            // Instance number slider settings
            self.sliderOptions = {
              value: 1,
              options: {
                floor: 1,
                ceil: 1,
                step: 1
              }
            };

            self.projectId = $routeParams.projectID;

            self.servings = [];
            self.editServing = {};

            self.activeTab = 'serving';
            self.showCreateNewServingForm = false;

            self.sendingRequest = false;

            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            var datasetService = DataSetService(self.projectId)

            self.ignorePoll = false;
            self.createNewServingMode = false;

            this.selectFile = function () {

              ModalService.selectDir('lg', '*', '').then(
                function (success) {
                  self.onDirSelected(success);
                },
                function (error) {
                  // Users changed their minds.
                });
            };

            self.getModelVersions = function (modelPath) {
              // /Projects/project_name/RelativePath
              var modelPathSplits = modelPath.split("/");
              var relativePath = modelPathSplits.slice(3).join("/");

              datasetService.getContents(relativePath).then(
                function (success) {
                  var versions = [];
                  for (var version in success.data) {
                    if (!isNaN(success.data[version].name)) {
                      versions.push(success.data[version].name);
                    } else {
                      growl.error("Directory doesn't respect the required structure", {
                        title: 'Error',
                        ttl: 15000
                      });
                      return;
                    }
                  }

                  if (versions.length === 0) {
                      growl.error("The selected model doesn't have any servable version", {
                        title: 'Error',
                        ttl: 15000
                      });
                      return;
                  }

                  self.editServing.versions = versions.sort().reverse();

                  if (self.editServing.modelVersion == null) {
                    self.editServing.modelVersion = self.editServing.versions[0];
                  }
                },
                function (error) {
                  growl.error(error.data.errorMsg, {
                    title: 'Error',
                    ttl: 15000
                  });
                });
            };

            // Check that the directory selected follows the required structure.
            // In particular check that the children are integers (version numbers)
            self.onDirSelected = function(modelPath) {
              var modelPathSplits = modelPath.split("/");
              var modelName = modelPathSplits[modelPathSplits.length-1];

              self.getModelVersions(modelPath);
              self.editServing.modelPath = modelPath;
              self.editServing.modelName = modelName;
            }

            self.showCreateServingForm = function () {
              self.showCreateNewServingForm = true
              self.createNewServingMode = true
            };

            self.hideCreateServingForm = function() {
              self.showCreateNewServingForm = false;
              self.createNewServingMode = false;
              self.editServing = {};
              self.versions = [];
              self.sliderOptions.value = 1;
            };

            self.getAllServings = function () {
              ServingService.getAllServings(self.projectId).then(
                 function (success) {
                   if (!self.ignorePoll){
                     self.servings = success.data;
                   }
                 },
                 function (error) {
                   growl.error(error.data.errorMsg, {
                     title: 'Error',
                     ttl: 15000
                   });
                 });
            };


            self.containsServingStatus = function (status) {

              for (var j = 0; j < self.servings.length; j++) {
                if (self.servings[j].status === status) {
                  return true;
                }
              }

              return false;
            };

            self.getAllServings()

            self.createOrUpdate = function () {
              self.sendingRequest = true;
              self.editServing.requestedInstances = self.sliderOptions.value;

              // Check that all the fields are populated
              if (self.editServing.modelPath === "" || self.editServing.modelPath == null ||
                  self.editServing.modelName === "" || self.editServing.modelName == null ||
                  self.editServing.modelVersion === "" || self.editServing.modelVersion == null) {
                growl.error("Please fill out all the fields", {
                  title: 'Error',
                  ttl: 15000
                });
                return;
              }

              ServingService.createOrUpdate(self.projectId, self.editServing).then(
                function (success) {
                  self.getAllServings();
                  self.hideCreateServingForm();
                  self.sendingRequest = false;
                },
                function (error) {
                  if (error.data !== undefined) {
                    growl.error(error.data.errorMsg, {
                      title: 'Error',
                      ttl: 15000
                    });
                  }
                  self.sendingRequest = false;
                });
            };

            self.updateServing = function(serving) {
              angular.copy(serving, self.editServing);
              self.editServing.modelVersion = self.editServing.modelVersion.toString();
              self.getModelVersions(serving.modelPath);
              self.sliderOptions.value = self.editServing.requestedInstances;
              self.showCreateServingForm();
            };

            self.deleteServing = function (serving) {

              ServingService.deleteServing(self.projectId, serving.id).then(
                function (success) {
                  self.getAllServings();
                },
                function (error) {
                  growl.error(error.data.errorMsg, {
                    title: 'Error',
                    ttl: 15000
                  });
                });
            };

            self.startOrStopServing = function (serving, action) {
              if (action === 'START'){
                self.ignorePoll = true;
                serving.status = 'Starting';
              } else {
                self.ignorePoll;
                serving.status = 'Stopping';
              }

              ServingService.startOrStop(self.projectId, serving.id, action).then(
                function (success) {
                  self.ignorePoll = false;
                },
                function (error) {
                  self.ignorePoll = false;
                  growl.error(error.data.errorMsg, {
                    title: 'Error',
                    ttl: 15000
                  });
                 });

            };

            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            self.poller = $interval(function () {
              self.getAllServings();
            }, 5000);

            self.init = function() {
                ServingService.getConfiguration().then(
                    function (success) {
                        self.sliderOptions.options.ceil = success.data.maxNumInstances;
                    },
                    function (error) {
                        self.ignorePoll = false;
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    }
                );
            };
            self.init();
          }
        ]);
