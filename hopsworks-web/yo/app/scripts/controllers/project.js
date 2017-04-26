/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectCtrl', ['$scope', '$rootScope', '$location', '$routeParams', '$route', 'UtilsService',
          'growl', 'ProjectService', 'ModalService', 'ActivityService', '$cookies', 'DataSetService', 'EndpointService',
          'UserService', 'TourService',
          function ($scope, $rootScope, $location, $routeParams, $route, UtilsService, growl, ProjectService,
                  ModalService, ActivityService, $cookies, DataSetService, EndpointService, UserService, TourService) {

            var self = this;
            self.loadedView = false;
            self.working = false;
            self.currentProject = [];
            self.activities = [];
            self.currentPage = 1;
            self.card = {};
            self.cards = [];
            self.projectMembers = [];
            self.tourService = TourService;
            self.location = $location;
            self.cloak = true;
            self.isClosed = true;
	      
            self.role = "";

            self.endpoint = '...';

            // We could instead implement a service to get all the available types but this will do it for now
//              self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'WORKFLOWS'];
//              self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'TENSORFLOW'];
              self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA'];
            $scope.activeService = "home";

            self.alreadyChoosenServices = [];
            self.selectionProjectTypes = [];
            self.pId = $routeParams.projectID;

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


            var getEndpoint = function () {
              EndpointService.findEndpoint().then(
                      function (success) {
                        console.log(success);
                        self.endpoint = success.data.data.value;
                      }, function (error) {
                self.endpoint = '...';
              });
            };

            self.initTour = function () {
              if (angular.equals(self.currentProject.projectName.substr(0, 10),
                self.tourService.sparkProjectPrefix)) {
                self.tourService.setActiveTour('spark');
              } else if (angular.equals(self.currentProject.projectName
                .substr(0, 10), self.tourService.kafkaProjectPrefix)) {
                self.tourService.setActiveTour('kafka');
              }

              if ($location.url() === "/project/" + self.pId) {
                self.tourService.currentStep_TourTwo = 0;
              } else if ($location.url() === "/project/" + self.pId + "/" + "jobs") {
                if (self.tourService.currentStep_TourThree === -1) {
                  self.tourService.currentStep_TourThree = 0;
                }
              } else if ($location.url() === "/project/" + self.pId + "/" + "newjob") {
                self.tourService.currentStep_TourFour = 0;
              }


            };


            self.activeTensorflow = function() {
              if ($location.url().indexOf("") !== -1) {
              }
              return false;
            };

            getEndpoint();

            

            var getCurrentProject = function () {
              ProjectService.get({}, {'id': self.pId}).$promise.then(
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
                          self.projectTypes.splice(index, 1);
                        });

                        $cookies.put("projectID",self.pId);
                        //set the project name under which the search is performed
                        UtilsService.setProjectName(self.currentProject.projectName);
                        self.getRole();

                      }, function (error) {
                $location.path('/');
              }
              );
            };


            var getAllActivities = function () {
              ActivityService.getByProjectId(self.pId).then(function (success) {
                self.activities = success.data;
                self.pageSize = 8;
                self.totalPages = Math.floor(self.activities.length / self.pageSize);
                self.totalItems = self.activities.length;
              }, function (error) {
                growl.info("Error" + error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            //we only need to load the activities if the path is project (endswith pId).
            var locationPath = $location.path();
            if (locationPath.substring(locationPath.length - self.pId.length, locationPath.length) === self.pId) {
              getAllActivities();
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

            self.membersModal = function () {
              ModalService.projectMembers('lg', self.pId).then(
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
                'ethicalStatus': self.currentProject.ethicalStatus
              };

              ProjectService.update({id: self.currentProject.projectId}, $scope.newProject)
                      .$promise.then(
                              function (success) {
                                self.working = false;
                                growl.success("Success: " + success.successMessage, {title: 'Success', ttl: 5000});
                                if (success.errorMsg) {
                                  growl.warning(success.errorMsg, {title: 'Error', ttl: 15000});
                                }
                                $route.reload();
                              }, function (error) {
                        self.working = false;
                        growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                      }
                      );
            };

            $scope.showHamburger = $location.path().indexOf("project") > -1;
            
            self.goToUrl = function(serviceName) {
              $scope.activeService = serviceName;
              $location.path('project/' + self.pId + '/' + serviceName);              
            }

            self.goToDatasets = function () {
              self.goToUrl('datasets');
            };

            self.goToJobs = function () {
              ProjectService.enableLogs({id: self.currentProject.projectId}).$promise.then(
                        function (success) {
                            
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Could not enable logging services', ttl: 5000});
                });
              
              self.goToUrl('jobs');
              if (self.tourService.currentStep_TourTwo > -1) {
                self.tourService.resetTours();
              }
              if (self.tourService.currentStep_TourFive > -1) {
                self.tourService.currentStep_TourSix = 0;
              }
            };

            self.goToWorklows = function () {
               self.goToUrl('workflows');
            };
            
            self.goToTensorflow = function () {
               self.goToUrl('tensorflow');
            };

	      
            self.goToSsh = function () {
              self.goToUrl('ssh');
            };

            self.goToCharon = function () {
              self.goToUrl('charon');
            };

            self.goToBiobanking = function () {
              self.goToUrl('biobanking');
            };

            self.goToKafka = function () {
              self.goToUrl('kafka');
              if (self.tourService.currentStep_TourTwo > -1) {
                self.tourService.resetTours();
              }
            };

            self.goToSettings = function () {
              self.goToUrl('settings');
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
            
            self.goToHistory = function () {
                $location.path('history/' + self.pId + '/history');
            };

            /**
             * Checks if the file has been accepted before opening.
             * @param dataset
             */
            self.browseDataset = function (dataset) {

              if (dataset.status === true) {
                UtilsService.setDatasetName(dataset.name);
                $location.path($location.path() + '/' + dataset.name + '/');
              } else {
                ModalService.confirmShare('sm', 'Accept Shared Dataset?', 'Do you want to accept this dataset and add it to this project?')
                        .then(function (success) {
                          DataSetService(self.pId).acceptDataset(dataset.id).then(
                                  function (success) {
                                    $location.path($location.path() + '/' + dataset.name + '/');
                                  }, function (error) {
                            growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                          });
                        }, function (error) {
                          if (error === 'reject') {
                            DataSetService(self.pId).rejectDataset(dataset.id).then(
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

            self.showZeppelin = function () {
              return showService("Zeppelin");
            };

            self.showJobs = function () {
              return showService("Jobs");
            };

            self.showSsh = function () {
              return showService("Ssh");
            };

            self.showCharon = function () {
              return showService("Charon");
            };

            self.showBiobanking = function () {
              return showService("Biobanking");
            };

            self.showKafka = function () {
              return showService("Kafka");
            };
            
            self.showTensorflow = function () {
              return showService("Tensorflow");
            };
	      
            self.showWorkflows = function () {
              return showService("Workflows");
            };

            self.getRole = function () {
              UserService.getRole(self.pId).then(
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
              if(self.projectFile.quotas !== null){
                return convertSize(self.projectFile.quotas.hdfsUsageInBytes);
              }
              return null;
            };

            self.hdfsQuota = function () {
              if(self.projectFile.quotas !== null){
                return convertSize(self.projectFile.quotas.hdfsQuotaInBytes);
              }
              return null;
            };
            
            self.hdfsNsCount = function () {
              if(self.projectFile.quotas !== null){
                return self.projectFile.quotas.hdfsNsCount;
              }
              return null;
            };

            self.hdfsNsQuota = function () {
              if(self.projectFile.quotas !== null){
                return self.projectFile.quotas.hdfsNsQuota;
              }
              return null;
            };

            /**
             * Converts and returns quota to hours.
             * @returns {Window.projectFile.quotas.yarnQuotaInSecs|projectL#10.projectFile.quotas.yarnQuotaInSecs}
             */
            self.yarnQuota = function () {
              if(self.projectFile.quotas != null){
                return self.roundTo(self.projectFile.quotas.yarnQuotaInSecs/60/60, 2);
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
            self.roundTo = function(n, digits) {
               if (digits === undefined) {
                   digits = 0;
               }

               var multiplicator = Math.pow(10, digits);
               n = parseFloat((n * multiplicator).toFixed(11));
               return Math.round(n) / multiplicator;
           };

          }]);
