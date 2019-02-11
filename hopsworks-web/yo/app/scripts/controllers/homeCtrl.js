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
        'StorageService', '$location', '$scope',
          function (ProjectService, UserService, ModalService, growl,
          ActivityService, $q, TourService, StorageService, $location, $scope) {

            var self = this;

            self.histories = [];
            self.loadedView = false;
            self.loadedZeppelin = false;
            self.tourService = TourService;
            self.projects = [];
            self.currentPage = 1;
            self.showTours = false;
            $scope.creating = {"spark" : false, "zeppelin" : false};
            self.exampleProjectID;
            self.tours = [];
            self.tutorials = [];
            self.working = [];
            self.user = {};
            self.showTourTips = true;
            self.sortBy='-project.created';
            self.getTours = function () {
              self.tours = [
                {'name': 'Deep Learning', 'tip': 'Take a tour by creating a project and running a Deep Learning notebook!'},
                {'name': 'Spark', 'tip': 'Take a tour of Hopsworks by creating a project and running a Spark job!'},
                {'name': 'Kafka', 'tip': 'Take a tour of Hopsworks by creating a project and running a Kafka job!'},
                {'name': 'Feature Store', 'tip': 'Take a tour of Hopsworks by creating a project and creating/reading features from your own Feature Store!'}
              ];
            };

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
                  growl.info(error, {title: 'Info', ttl: 2000});
                });
            };
            
            updateUIAfterChange(false);

            self.initCheckBox = function () {
              self.loadToursState().then(
                function(success) {
                  if (self.tourService.informAndTips || self.tourService.tipsOnly) {
                    self.showTourTips = true;
                  } else {
                    self.showTourTips = false;
                  }
                  StorageService.store("hopsworks-showtourtips",self.showTourTips);
                }, function(error) {
                  console.log("error");
                }
              );
            };

            self.loadToursState = function () {
                return UserService.profile().then(
                  function (success) {
                    self.user = success.data;
                    var tourState = self.user.toursState;
                    if (tourState === 0) {
                      self.tourService.setInformAndTipsState();
                    } else if (tourState === 1) {
                      self.tourService.setTipsOnlyState();
                    } else if (tourState === 2) {
                      self.tourService.setInformOnly();
                    } else if (tourState === 3) {
                      self.tourService.setShowNothingState();
                    } else {
                      self.tourService.setDefaultTourState();
                    }
                  }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
                  });
            };

            self.disableInformBalloon = function () {
              if (self.tourService.informAndTips) {
                self.user.toursState = 1;
                self.updateProfile(self.tourService.setTipsOnlyState);
              } else if (self.tourService.informOnly) {
                self.user.toursState = 3;
                self.updateProfile(self.tourService.setShowNothingState);
              }
            };

            self.toggleTourTips = function () {
              if (self.showTourTips) {
                self.enableTourTips();
              } else {
                self.disableTourTips();
              }
              StorageService.store("hopsworks-showtourtips",self.showTourTips);
            };

            self.disableTourTips = function () {
              if (self.tourService.informAndTips) {
                self.user.toursState = 2;
                self.updateProfile(self.tourService.setInformOnly);
              } else if (self.tourService.tipsOnly) {
                self.user.toursState = 3;
                self.updateProfile(self.tourService.setShowNothingState);
              }
            };

            self.enableTourTips = function () {
              if (self.tourService.informOnly) {
                self.user.toursState = 0;
                self.updateProfile(self.tourService.setInformAndTipsState);
              } else if (self.tourService.showNothing) {
                self.user.toursState = 1;
                self.updateProfile(self.tourService.setTipsOnlyState);
              }
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
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                  } 
              });
            };

            self.createExampleProject = function (uiTourName) {

              $scope.creating[uiTourName] = true;

              var internalTourName = '';
              if(uiTourName === 'Deep Learning') {
                internalTourName = 'Deep_Learning';
              } else if(uiTourName === 'Feature Store') {
                internalTourName = 'featurestore';
              } else {
                internalTourName = uiTourName;
              }

              if (self.showTourTips === false) {
                self.toggleTourTips();
              }

              ProjectService.example({type: internalTourName}).$promise.then(
                      function (success) {
                        $scope.creating[uiTourName] = false;
                        self.tourService.setActiveTour(internalTourName);
                        growl.success("Created Tour Project", {title: 'Success', ttl: 5000});
                        self.exampleProjectID = success.id;
                        updateUIAfterChange(true);
                        // To make sure the new project is refreshed
//                        self.showTours = false;
                        if (success.errorMsg) {
                          $scope.creating[uiTourName] = false;
                          growl.warning("some problem", {title: 'Error', ttl: 10000});
                        }
                      },
                      function (error) {
                        $scope.creating[uiTourName] = false;
                          if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                          } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 8000});
                          }
                      }
              );
            };

            self.deleteProjectAndDatasets = function (projectId) {
              self.working[projectId] = true;
              //Clear project StorageService state
              StorageService.remove(projectId+"-tftour-finished");

              ProjectService.delete({id: projectId}).$promise.then(
                      function (success) {
                        growl.success(success.successMessage, {title: 'Success', ttl: 5000});
                        updateUIAfterChange(false);
                        if (self.tourService.currentStep_TourOne > -1) {
                          self.tourService.resetTours();
                        }
                        self.working[projectId] = false;
                      },function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
                        self.working[projectId] = false;
                      }
              );
            };

            self.EnterExampleProject = function (id) {
              $location.path('/project/' + id);
              $location.replace();
              self.tourService.resetTours();
            };
          }]);
