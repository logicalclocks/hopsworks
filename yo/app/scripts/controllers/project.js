/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .controller('ProjectCtrl', ['$scope', '$rootScope','$modalStack', '$location', '$routeParams', 'UtilsService',
      'growl', 'ProjectService', 'ModalService', 'ActivityService', '$cookies', 'DataSetService', 'EndpointService',
      function ($scope, $rootScope,$modalStack, $location, $routeParams, UtilsService, growl, ProjectService, ModalService, ActivityService, $cookies, DataSetService, EndpointService) {

        var self = this;
        self.working = false;
        self.currentProject = [];
        self.activities = [];
        self.currentPage = 1;
        self.card = {};
        self.cards = [];
        self.projectMembers = [];

        self.endpoint = '...';

        // We could instead implement a service to get all the available types but this will do it for now
//        self.projectTypes = ['JOBS', 'ZEPPELIN', 'BIOBANKING', 'CHARON'];
        self.projectTypes = ['JOBS', 'ZEPPELIN'];
        self.alreadyChoosenServices = [];
        self.selectionProjectTypes = [];
        self.pId = $routeParams.projectID;

        self.projectFile = {
          description : null,
          id : null,
          name : null,
          parentId : null,
          path : null,
        };

        var getEndpoint = function () {
          EndpointService.findEndpoint().then(
              function (success) {
                console.log(success);
                self.endpoint = success.data.data.value;
              }, function (error) {
            self.endpoint = '...';
          });
        };

        getEndpoint();

        var getCurrentProject = function () {
          ProjectService.get({}, {'id': self.pId}).$promise.then(
              function (success) {
                self.currentProject = success;
                self.projectFile.id = self.currentProject.inodeid;
                self.projectFile.name = self.currentProject.projectName;
                self.projectFile.parentId = self.currentProject.projectTeam[0].project.inode.inodePK.parentId;
                self.projectFile.path = "/Projects/"+self.currentProject.projectName;
                self.projectFile.description = self.currentProject.description;
                self.projectFile.retentionPeriod = self.currentProject.retentionPeriod;
                self.projectFile.hdfsQuotaInGBs = self.currentProject.hdfsQuotaInGBs;
                self.projectFile.yarnQuotaInMins = self.currentProject.yarnQuotaInMins;
                $rootScope.$broadcast('setMetadata', { file:
                   {id : self.projectFile.id,
                    name : self.projectFile.name,
                    parentId : self.projectFile.parentId,
                    path : self.projectFile.path} });

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

                $cookies.projectID = self.pId;
                //set the project name under which the search is performed
                UtilsService.setProjectName(self.currentProject.projectName);

              }, function (error) {
            $location.path('/');
          }
          );
        };


        var getAllActivities = function () {
          ActivityService.getByProjectId(self.pId).then(function (success) {
            self.activities = success.data;
            self.pageSize = 10;
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


        self.projectSettingModal = function () {
          ModalService.projectSettings('md').then(
              function (success) {
                getAllActivities();
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
              });
        };

        self.projectSettingModal = function () {
          ModalService.projectSettings('md').then(
              function (success) {
                getAllActivities();
                getCurrentProject();
              }, function (error) {
            growl.info("You closed without saving.", {title: 'Info', ttl: 5000});
          });
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
                    $modalStack.getTop().key.close();
                  }, function (error) {
                self.working = false;
                growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
              }
              );
        };

        self.close = function () {
          $modalStack.getTop().key.dismiss();
        };

        $scope.showHamburger = $location.path().indexOf("project") > -1;

        self.goToDatasets = function () {
          $location.path('project/' + self.pId + '/datasets');
        };

        self.goToJobs = function () {
          $location.path('project/' + self.pId + '/jobs');
        };

        self.goToSsh = function () {
          $location.path('project/' + self.pId + '/ssh');
        };

        self.goToCharon = function () {
          $location.path('project/' + self.pId + '/charon');
        };

        self.goToBiobanking = function () {
          $location.path('project/' + self.pId + '/biobanking');
        };

        self.goToService = function (service) {
          $location.path('project/' + self.pId + '/' + service.toLowerCase());
        };

        self.goToSpecificDataset = function (name) {
          $location.path($location.path() + '/' + name);
        };

        self.goToMetadataDesigner = function () {
          $location.path('project/' + self.pId + '/metadata');
        };

        /**
         * Checks if the file has been accepted before opening.
         * @param dataset
         */
        self.browseDataset = function (dataset) {

          if (dataset.status === true) {
            UtilsService.setDatasetName(dataset.name);
            $location.path($location.path() + '/' + dataset.name);
          } else {
            ModalService.confirmShare('sm', 'Confirm', 'Do you want to accept this dataset, and add it to this project?')
                .then(function (success) {
                  DataSetService(self.pId).acceptDataset(dataset.id).then(
                      function (success) {
                        $location.path($location.path() + '/' + dataset.name);
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

        var showService = function (serviceName) {
          var len = self.alreadyChoosenServices.length;
          for (var i = 0; i < len; i++) {
            if (self.alreadyChoosenServices[i] === serviceName) {
              return true;
            }
          }
          return false;
        };


        self.hdfsQuota = function () {
          return self.projectFile.hdfsQuotaInGB;
        };

        self.yarnQuota = function () {
          return self.projectFile.yarnQuotaInMins;
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


      }]);