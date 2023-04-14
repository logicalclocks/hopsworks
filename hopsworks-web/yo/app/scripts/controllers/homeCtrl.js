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
        .controller('HomeCtrl', ['ProjectService', 'UserService',
        'ModalService', 'growl', 'ActivityService', '$q', 'TourService', 
        'StorageService', '$location', '$scope', '$rootScope',
          function (ProjectService, UserService, ModalService, growl,
          ActivityService, $q, TourService, StorageService, $location, $scope, $rootScope) {

            var self = this;

            self.histories = [];
            self.loadedView = false;
            self.tourService = TourService;
            self.projects = [];
            self.currentPage = 1;
            self.showTours = true;

            $scope.creating = {"spark" : false, "zeppelin" : false};
            self.exampleProjectID;
            self.tours = [
                {'name': 'Deep Learning', 'tip': 'Take a tour by creating a project and running a Deep Learning notebook!'},
                {'name': 'Spark', 'tip': 'Take a tour of Hopsworks by creating a project and running a Spark job!'},
                {'name': 'Kafka', 'tip': 'Take a tour of Hopsworks by creating a project and running a Kafka job!'}
            ];
            self.tutorials = [];
            self.working = [];
            self.user = {};
            $rootScope.showTourTips = false;
            self.showTT = false;
            self.sortBy='-project.created';


            $scope.$on('$viewContentLoaded', function () {
              self.loadedView = true;
            });

            self.getTutorials = function () {
              self.tutorials = [
                {'url': 'spark.apache.org/docs/latest/running-on-yarn.html', 'description': 'Spark on YARN'},
                {'url': 'ci.apache.org/projects/flink/flink-docs-master/setup/yarn_setup.html', 'description': 'Flink on YARN'}
              ];
            };

            $scope.isCreating = function (tourName) {
              if ($scope.creating[tourName] === true) {
                return true;
              }
              return false;
            };


            // Load all projects
            var loadProjects = function (success) {
              self.projects = success;
              self.pageSizeProjects = 10;
              self.totalPagesProjects = Math.ceil(self.projects.length / self.pageSizeProjects);
              self.totalItemsProjects = self.projects.length;
              self.currentPageProjects = 1;
            };

            var updateUIAfterChange = function (exampleProject) {
              ProjectService.query().$promise.then(function (result) {
                  if (exampleProject) {
                    self.tourService.currentStep_TourOne = 0;
                    self.tourService.KillTourOneSoon();
                  }
                  loadProjects(result);
                },
                function (error) {
                  console.log(error);
                });
            };
            
            updateUIAfterChange(false);

            var disableTT = function () {
              self.tourService.disableTourTips();
              $rootScope.showTourTips = false;
              self.showTT = false;
            };

            var enableTT = function () {
              self.tourService.enableTourTips();
              $rootScope.showTourTips = true;
              self.showTT = true;
            };

             var initCheckBox = function () {
                if (self.tourService.showTips) {
                    $rootScope.showTourTips = true;
                    self.showTT = true;
                } else {
                    $rootScope.showTourTips = false;
                    self.showTT = false;
                }
            };
            self.tourService.init(initCheckBox);

            self.disableInformBalloon = function () {
                disableTT();
            };

            $scope.toggleTourTips = function () {
              if (self.showTT) {
                self.enableTourTips();
              } else {
                self.disableTourTips();
              }
            };

            self.disableTourTips = function () {
                disableTT();
            };

            self.enableTourTips = function () {
                enableTT();
            };

            self.updateProfile = function (fun) {
              UserService.updateProfile(self.user).then (
                function (success) {
                  fun();
                }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
                }
              );
            };

            self.showGettingStarted = function () {
              if (self.projects === undefined || self.projects === null || self.projects.length === 0) {
                return true;
              }
              return false;
            };

            // Create a new project
            self.newProject = function () {
              ModalService.createProject('lg').then(
                function (success) {
                  updateUIAfterChange(false);
                }, function (error) {
                  if (typeof error.data !== 'undefined' && typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                  } else if (typeof error.data !== 'undefined' && typeof error.data.errorMsg !== 'undefined') {
                    growl.error("", {title: error.data.errorMsg, ttl: 5000});
                  }
              });
            };

            self.EnterExampleProject = function (id) {
              $location.path('/project/' + id);
              $location.replace();
              self.tourService.resetTours();
            };
          }]);
