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
        .controller('ProjectCtrl', ['$scope', '$rootScope', '$location', '$routeParams', '$route', '$timeout', '$window', 'UtilsService',
          'growl', 'ProjectService', 'ModalService', 'ActivityService', '$cookies', 'DataSetService',
          'UserService', 'TourService', 'PythonService', 'StorageService', 'CertService', 'VariablesService', 'FileSaver', 'Blob',
          'AirflowService', '$http',
        function ($scope, $rootScope, $location, $routeParams, $route, $timeout, $window, UtilsService, growl, ProjectService,
                  ModalService, ActivityService, $cookies, DataSetService, UserService, TourService, PythonService,
                    StorageService, CertService, VariablesService, FileSaver, Blob, AirflowService, $http) {

            var self = this;
            self.loadedView = false;
            self.loadedProjectData = false;
            self.working = false;
            self.currentProject = [];
            self.activities = [];
            self.currentPage = 1;
            self.card = {};
            self.cards = [];
            self.projectMembers = [];
            self.tourService = TourService;
            self.tourService.currentStep_TourNine = -1; //Feature store Tour
            self.location = $location;
            self.cloak = true;
            self.isClosed = true;
            self.versions = [];

            self.role = "";

            self.endpoint = '...';

            self.disableTours = function() {
                $rootScope.showTourTips = false;
                self.tourService.disableTourTips();
            };

            // We could instead implement a service to get all the available types but this will do it for now
            if ($rootScope.isDelaEnabled) {
                // , 'RSTUDIO'
                self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'DELA', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
            } else {
                self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
            }
            $scope.activeService = "home";

            self.alreadyChoosenServices = [];
            self.selectionProjectTypes = [];
            self.projectId = $routeParams.projectID;

            self.projectFile = {
              description: null,
              id: null,
              name: null,
              parentId: null,
              path: null,
              quotas: null
            };

            $scope.$on('$viewContentLoaded', function () {
              self.loadedView = true;
            });

            self.initTour = function () {
              self.tourService.currentProjectName = self.currentProject.projectName;
              if (angular.equals(self.currentProject.projectName.substr(0,
                      self.tourService.sparkProjectPrefix.length),
                      self.tourService.sparkProjectPrefix)) {
                self.tourService.setActiveTour('spark');
              } else if (angular.equals(self.currentProject.projectName
                      .substr(0, self.tourService.kafkaProjectPrefix.length),
                      self.tourService.kafkaProjectPrefix)) {
                self.tourService.setActiveTour('kafka');
              } else if (angular.equals(self.currentProject.projectName
                      .substr(0, self.tourService.deepLearningProjectPrefix.length),
                      self.tourService.deepLearningProjectPrefix)) {
                self.tourService.setActiveTour('deep_learning');
              } else if (angular.equals(self.currentProject.projectName
                      .substr(0, self.tourService.featurestoreProjectPrefix.length),
                  self.tourService.featurestoreProjectPrefix)) {
                  self.tourService.setActiveTour('featurestore');
              }

              // Angular adds '#' symbol to the url when click on the home logo
              // which messes with the tours
              self.loc = $location.url().split('#')[0];
              if (self.loc === "/project/" + self.projectId) {
                self.tourService.currentStep_TourTwo = 0;
              } else if (self.loc === "/project/" + self.projectId + "/" + "jobs" || self.loc === "/project/" + self.projectId + "/" + "jupyter") {
                if (self.tourService.currentStep_TourThree === -1) {
                  self.tourService.currentStep_TourThree = 0;
                }
              } else if (self.loc === "/project/" + self.projectId + "/" + "newjob") {
                self.tourService.currentStep_TourFour = 0;
              }
            };

            self.tourService.init(null);

            var getCurrentProject = function () {
              ProjectService.get({}, {'id': self.projectId}).$promise.then(
                      function (success) {
                        self.currentProject = success;
                        self.projectFile.id = self.currentProject.inodeid;
                        self.projectFile.name = self.currentProject.projectName;
                        self.projectFile.parentId = self.currentProject.projectTeam[0].project.inode.inodePK.parentId;
                        self.projectFile.path = "/Projects/" + self.currentProject.projectName;
                        self.projectFile.description = self.currentProject.description;
                        self.projectFile.retentionPeriod = self.currentProject.retentionPeriod;
                        self.projectFile.quotas = self.currentProject.quotas;
                        if (angular.equals(self.currentProject.projectName.substr(0, 5), 'demo_')) {
                          self.initTour();
                        } else {
                          self.tourService.resetTours();
                        }
                        $rootScope.$broadcast('setMetadata', {file:
                                  {id: self.projectFile.id,
                                    name: self.projectFile.name,
                                    parentId: self.projectFile.parentId,
                                    path: self.projectFile.path}});

                        self.projectMembers = self.currentProject.projectTeam;
                        self.alreadyChoosenServices = [];
                        self.currentProject.services.forEach(function (entry) {
                          self.alreadyChoosenServices.push(entry);
                        });

                        // Remove already choosen services from the service selection
                        self.alreadyChoosenServices.forEach(function (entry) {
                          var index = self.projectTypes.indexOf(entry.toUpperCase());
                          if (index >= 0) {
                            self.projectTypes.splice(index, 1);
                          }
                        });


                        $cookies.put("projectID", self.projectId);
                        //set the project name under which the search is performed
                        UtilsService.setProjectName(self.currentProject.projectName);
                        self.getRole();
                        self.loadedProjectData = true
                      }, function (error) {
                      self.loadedProjectData = true
                  }
              );

            };

            self.pageSize = 8;
            self.curentPage = 1;
            var getAllActivities = function (offset) {
              ActivityService.getByProjectId(self.projectId, self.pageSize, offset).then(function (success) {
                self.activities = success.data.items;                
                self.totalItems = success.data.count;
              }, function (error) {
                  if (typeof error.data.usrMsg !== 'undefined') {
                      growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                  } else {
                      growl.error("", {title: error.data.errorMsg, ttl: 5000});
                  }
              });
            };
            
            self.getActivitiesNextPage = function () {
              var offset = self.pageSize * (self.curentPage - 1);
              if (self.totalItems > offset) {
                getAllActivities(offset);
              }
            };

            //we only need to load the activities if the path is project (endswith pId).
            var locationPath = $location.path();
            if (locationPath.substring(locationPath.length - self.projectId.length, locationPath.length) === self.projectId) {
              getAllActivities(0);
            }            

            getCurrentProject();


            // Check if the service exists and otherwise add it or remove it depending on the previous choice
            self.exists = function (projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              if (idx > -1) {
                self.selectionProjectTypes.splice(idx, 1);
              } else {
                self.selectionProjectTypes.push(projectType);
              }
            };


            self.allServicesSelected = function () {
              return self.projectTypes.length > 0;
            };

            self.projectSettingModal = function () {
              ModalService.projectSettings('md', self.pId).then(
                      function (success) {
                        getAllActivities();
                        getCurrentProject();

                      });
            };


            self.membersModal = function () {
              ModalService.projectMembers('lg', self.projectId).then(
                      function (success) {
                      }, function (error) {
              });
            };

            self.saveProject = function () {
              self.working = true;
              $scope.newProject = {
                'projectName': self.currentProject.projectName,
                'description': self.currentProject.description,
                'services': self.selectionProjectTypes,
                'retentionPeriod': self.currentProject.retentionPeriod,
              };

              ProjectService.update({id: self.currentProject.projectId}, $scope.newProject)
                      .$promise.then(
                              function (success) {
                                self.working = false;
                                growl.success("Success: " + success.successMessage, {title: 'Success', ttl: 5000});
                                if (success.errorMsg) {
                                  ModalService.alert('sm', 'Warning!', success.errorMsg).then(
                                          function (success) {
                                            $route.reload();
                                          }, function (error) {
                                    $route.reload();
                                  });
                                } else {
                                  $route.reload();
                                }
                              }, function (error) {
                        self.working = false;
                        growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                      }
                      );
            };

            $scope.showHamburger = $location.path().indexOf("project") > -1;

            // Show the searchbar if you are (1) within a project on datasets or (2) on the landing page
//            $scope.showDatasets = ($location.path().indexOf("datasets") > -1) || ($location.path().length < ($location.path().length + 3));


            self.goToHopsworksInstance = function (endpoint, serviceName) {
              $scope.activeService = serviceName;
              $location.path('http://' + endpoint + '/project/' + self.projectId + '/' + serviceName);
            };


            self.goToUrl = function (serviceName) {
              $scope.activeService = serviceName;
              $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.goToDatasets = function () {
              self.goToUrl('datasets');
            };

            self.goToRStudio = function () {
              self.goToUrl('rstudio');
            };

            self.goToDagComposer = function() {
              self.goToUrl('airflow/dagcomposer');
            };

            self.goToAirflow = function () {
              self.goToUrl('airflow');
            };

            self.goToJobs = function () {
              self.toggleKibanaNavBar();
              self.goToUrl('jobs');
              if (self.tourService.currentStep_TourTwo > -1) {
                self.tourService.resetTours();
              }
              if (self.tourService.currentStep_TourFive > -1) {
                self.tourService.currentStep_TourSix = 0;
              }
            };

            self.goToJupyter = function () {
              // Check which instance of Hopsworks is running Jupyter
              // If that instance is running, URL redirect to that instance
              // If not running, start a new instance

//              http://localhost:8080/hopsworks/#!/project/1/settings
              if (self.tourService.currentStep_TourTwo > -1) {
                  self.tourService.resetTours();
              }

              self.enabling = true;
              PythonService.enabled(self.projectId).then(
                  function (success) {
                      var version = success.data.count > 0? success.data.items[0].pythonVersion : "0.0";
                      // Check if jupyter is installed
                      PythonService.getLibrary(self.projectId, version,"hdfscontents").then(
                          function(success) {
                              self.goToUrl('jupyter');
                          },
                          function(error) {
                              ModalService.confirm('sm', 'Install Jupyter first', 'Make sure Jupyter is installed in your project environment')
                              .then(function (success) {
                                  self.goToUrl('python');
                              }, function (error) {
                                  self.goToUrl('jupyter');
                              });
                          }
                      );
                  }, function (error) {
                      if (self.currentProject.projectName.startsWith("demo_deep_learning")) {
                          self.goToUrl('jupyter');
                      } else {
                          ModalService.confirm('sm', 'Enable Anaconda First', 'You need to enable Anaconda before running Jupyter!')
                              .then(function (success) {
                                  self.goToUrl('python');
                              }, function (error) {
                                  self.goToUrl('jupyter');
                              });
                      }
                  });
            };


            self.goToWorklows = function () {
              self.goToUrl('workflows');
            };

            self.goToServing = function () {
              self.toggleKibanaNavBar();
              self.goToUrl('serving');
            };

            self.goToFeaturestore = function () {
                self.goToUrl('featurestore');
            };

            self.goToPython = function () {
              self.toggleKibanaNavBar();
              self.goToUrl('python');
            };

            self.goToExperiments = function () {
              self.toggleKibanaNavBar();
              self.goToUrl('experiments');
            };

            self.goToModels = function () {
              self.toggleKibanaNavBar();
              self.goToUrl('models');
            };

            self.goToKafka = function () {
              self.goToUrl('kafka');
              if (self.tourService.currentStep_TourTwo > -1) {
                self.tourService.resetTours();
              }
            };

            self.goToSettings = function () {
              self.goToUrl('settings');
              if (self.tourService.currentStep_TourTwo > -1) {
                self.tourService.resetTours();
              }
            };

            self.goToService = function (service) {
              self.goToUrl(service.toLowerCase());
            };

            self.goToSpecificDataset = function (name) {
              $location.path($location.path() + '/' + name);
            };

            self.goToMetadataDesigner = function () {
              self.goToUrl('metadata');
            };

            /**
             * Checks if the file has been accepted before opening.
             * @param dataset
             */
            self.browseDataset = function (dataset) {

              if (dataset.status === true) {
                UtilsService.setDatasetName(dataset.name);
                $rootScope.parentDS = dataset;
                $location.path($location.path() + '/' + dataset.name + '/');
              } else {
                ModalService.confirmShare('sm', 'Accept Shared Dataset?', 'Do you want to accept this dataset and add it to this project?')
                        .then(function (success) {
                          DataSetService(self.projectId).acceptDataset(dataset.id).then(
                                  function (success) {
                                    $location.path($location.path() + '/' + dataset.name + '/');
                                  }, function (error) {
                            growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                          });
                        }, function (error) {
                          if (error === 'reject') {
                            DataSetService(self.projectId).rejectDataset(dataset.id).then(
                                    function (success) {
                                      $location.path($location.path() + '/');
                                      growl.success("Success: " + success.data.successMessage, {title: 'Success', ttl: 5000});
                                    }, function (error) {

                              growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                            });
                          }
                        });
              }

            };

            self.sizeOnDisk = function (fileSizeInBytes) {
              return convertSize(fileSizeInBytes);
            };

            self.showJupyter = function () {
              return showService("Jupyter");
            };

            self.showJobs = function () {
              return showService("Jobs");
            };

            self.showExperiments = function () {
              return (showService("Jobs") || showService("Jupyter"));
            };

            self.showModels = function () {
              return (showService("Jobs") || showService("Jupyter") || showService("Serving"));
            };

            self.showSsh = function () {
              return showService("Ssh");
            };

            self.showKafka = function () {
              return showService("Kafka");
            };

            self.showDela = function () {
              if (!$rootScope.isDelaEnabled) {
                return false;
              }
              return showService("Dela");
            };

            self.showAirflow = function () {
              return showService("Airflow");
            };

            self.showRStudio = function () {
              return false;
//              return showService("RStudio");
            };

            self.showServing = function () {
                return showService("Serving");
            };

            self.showFeaturestore = function () {
                return showService("Featurestore");
            };

            self.showWorkflows = function () {
              return showService("Workflows");
            };

            self.getRole = function () {
              UserService.getRole(self.projectId).then(
                      function (success) {
                        self.role = success.data.role;
                      }, function (error) {
                self.role = "";
              });
            };

            var showService = function (serviceName) {
              var len = self.alreadyChoosenServices.length;
              for (var i = 0; i < len; i++) {
                if (self.alreadyChoosenServices[i] === serviceName) {
                  return true;
                }
              }
              return false;
            };

            self.hdfsUsage = function () {
              if (self.projectFile.quotas !== null) {
                return convertSize(self.projectFile.quotas.hdfsUsageInBytes);
              }
              return null;
            };

            self.hdfsQuota = function () {
              if (self.projectFile.quotas !== null) {
                return convertSize(self.projectFile.quotas.hdfsQuotaInBytes);
              }
              return null;
            };

            self.hdfsNsCount = function () {
              if (self.projectFile.quotas !== null) {
                return self.projectFile.quotas.hdfsNsCount;
              }
              return null;
            };

            self.hdfsNsQuota = function () {
              if (self.projectFile.quotas !== null) {
                return self.projectFile.quotas.hdfsNsQuota;
              }
              return null;
            };

            self.hiveHdfsUsage = function () {
              if (self.projectFile.quotas !== null) {
                return convertSize(self.projectFile.quotas.hiveHdfsUsageInBytes);
              }
              return null;
            };

            self.hiveHdfsQuota = function () {
              if (self.projectFile.quotas !== null) {
                return convertSize(self.projectFile.quotas.hiveHdfsQuotaInBytes);
              }
              return null;
            };

            self.hiveHdfsNsCount = function () {
              if (self.projectFile.quotas !== null) {
                return self.projectFile.quotas.hiveHdfsNsCount;
              }
              return null;
            };

            self.hiveHdfsNsQuota = function () {
              if (self.projectFile.quotas !== null) {
                return self.projectFile.quotas.hiveHdfsNsQuota;
              }
              return null;
            };

            self.featurestoreHdfsUsage = function () {
                if (self.projectFile.quotas !== null) {
                    return convertSize(self.projectFile.quotas.featurestoreHdfsUsageInBytes);
                }
                return null;
            };

            self.featurestoreHdfsQuota = function () {
                if (self.projectFile.quotas !== null) {
                    return convertSize(self.projectFile.quotas.featurestoreHdfsQuotaInBytes);
                }
                return null;
            };

            self.featurestoreHdfsNsCount = function () {
                if (self.projectFile.quotas !== null) {
                    return self.projectFile.quotas.featurestoreHdfsNsCount;
                }
                return null;
            };

            self.featurestoreHdfsNsQuota = function () {
                if (self.projectFile.quotas !== null) {
                    return self.projectFile.quotas.featurestoreHdfsNsQuota;
                }
                return null;
            };

            /**
             * Converts and returns quota to hours.
             * @returns {Window.projectFile.quotas.yarnQuotaInSecs|projectL#10.projectFile.quotas.yarnQuotaInSecs}
             */
            self.yarnQuota = function () {
              if (self.projectFile.quotas != null) {
                return self.roundTo(self.projectFile.quotas.yarnQuotaInSecs / 60 / 60, 2);
              }
              return null;
            };


            self.saveAllowed = function () {
              if (self.currentProject.projectName.length === 0) {
                return true;
              }
            };

            $scope.data = [
              [65, 59, 90, 81, 56, 55, 40],
              [28, 48, 40, 19, 96, 27, 100]
            ];

            $scope.labels2 = ["January", "February", "March", "April", "May", "June", "July"];
            $scope.series = ['Fileoperations', 'Querys'];
            $scope.data2 = [
              [65, 59, 80, 81, 56, 55, 40],
              [28, 48, 40, 19, 86, 27, 90]
            ];
            $scope.onClick = function (points, evt) {
              console.log(points, evt);
            };

            /**
             * http://stackoverflow.com/questions/10015027/javascript-tofixed-not-rounding/32605063#32605063
             * @param {type} n
             * @param {type} digits
             * @returns {Number}
             */
            self.roundTo = function (n, digits) {
              if (digits === undefined) {
                digits = 0;
              }

              var multiplicator = Math.pow(10, digits);
              n = parseFloat((n * multiplicator).toFixed(11));
              return Math.round(n) / multiplicator;
            };

            self.tourDone = function (tour) {
              StorageService.store("hopsworks-tourdone-" + tour, true);
            };

            self.isTourDone = function (tour) {
              var isDone = StorageService.get("hopsworks-tourdone-" + tour);
            };

            self.getCerts = function () {
              UserService.profile().then(
                function (success) {
                  var user = success.data;
                  downloadCerts(user.accountType);
                }, function (error) {
                  downloadCerts();
              });
            };
            
            var downloadCerts = function (accountType) {
              if (accountType === 'KRB') {
                CertService.downloadProjectCertKrb(self.currentProject.projectId)
                    .then(function (success) {
                        var certs = success.data;
                        download(atob(certs.kStore), 'keyStore.' + certs.fileExtension);
                        download(atob(certs.tStore), 'trustStore.' + certs.fileExtension);
                    }, function (error) {
                        var errorMsg = (typeof error.data.usrMsg !== 'undefined') ? error.data.usrMsg : "";
                        growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000});
                    });
              } else if (accountType === 'OAUTH2') {
                CertService.downloadProjectCertOAuth(self.currentProject.projectId)
                    .then(function (success) {
                        var certs = success.data;
                        download(atob(certs.kStore), 'keyStore.' + certs.fileExtension);
                        download(atob(certs.tStore), 'trustStore.' + certs.fileExtension);
                    }, function (error) {
                        var errorMsg = (typeof error.data.usrMsg !== 'undefined') ? error.data.usrMsg : "";
                        growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000});
                    });

              } else {
                ModalService.certs('sm', 'Certificates Download', 'Please type your password', self.projectId)
                  .then(function (successPwd) {
                    if (accountType === 'LDAP') {
                      CertService.downloadProjectCertLdap(self.currentProject.projectId, successPwd)
                        .then(function (success) {
                          var certs = success.data;
                          download(atob(certs.kStore), 'keyStore.' + certs.fileExtension);
                          download(atob(certs.tStore), 'trustStore.' + certs.fileExtension);
                        }, function (error) {
                          var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                          growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000});
                        });
                    } else {
                      CertService.downloadProjectCert(self.currentProject.projectId, successPwd)
                        .then(function (success) {
                          var certs = success.data;
                          download(atob(certs.kStore), 'keyStore.' + certs.fileExtension);
                          download(atob(certs.tStore), 'trustStore.' + certs.fileExtension);
                        }, function (error) {
                          var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "";
                          growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000});
                        });
                    }
                  }, function (error) {

                  });
              }
            };

            var download = function (text, fileName) {
              var bytes = toByteArray(text);
              var data = new Blob([bytes], {type: 'application/octet-binary'});
              FileSaver.saveAs(data, fileName);
            };

            var toByteArray = function (text) {
              var l = text.length;
              var bytes = new Uint8Array(l);
              for (var i = 0; i < l; i++) {
                bytes[i] = text.charCodeAt(i);
              }
              return bytes;
            };

            self.isServiceEnabled = function (service) {
              var idx = self.projectTypes.indexOf(service);
              return idx === -1;
            };

            self.openWindow = function () {
              $window.open(self.ui, '_blank');
            };

            self.connectToAirflow = function () {
              AirflowService.storeAirflowJWT(self.projectId).then(
                function (success) {
                  // Open airlfow
                  var newTab = $window.open('about:blank', '_blank');
                  $http.get(getApiLocationBase() + "/airflow").then ( function (response) {
                    newTab.location.href = getApiLocationBase() + "/airflow/admin";
                  })
                }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                }
              )
            };

            var kibanaNavVarInitKey = "hopsworks.kibana.navbar.set";
            self.toggleKibanaNavBar = function () {
                var kibanaNavBarInit = StorageService.get(kibanaNavVarInitKey);
                if(kibanaNavBarInit === false){
                    StorageService.store(kibanaNavVarInitKey, true);
                    StorageService.store("kibana.isGlobalNavOpen", false);
                }
            };

          }]);
