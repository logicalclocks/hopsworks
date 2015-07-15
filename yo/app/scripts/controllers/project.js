/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectCtrl', ['$scope', '$modalStack', '$location', '$routeParams', 'UtilsService',
          'growl', 'ProjectService', 'ModalService', 'ActivityService','$cookies',
          function ($scope, $modalStack, $location, $routeParams, UtilsService, growl, ProjectService, ModalService, ActivityService, $cookies) {

            UtilsService.setIndex("child");

            var self = this;
            self.currentProject = [];
            self.activities = [];
            self.currentPage = 1;

            self.card = {};
            self.cards = [];
            self.projectMembers = [];

            // We could instead implement a service to get all the available types but this will do it for now
            self.projectTypes = ['CUNEIFORM', 'SPARK', 'ADAM', 'MAPREDUCE', 'YARN', 'ZEPPELIN'];
            self.alreadyChoosenServices = [];
            self.selectionProjectTypes = [];
            self.pId = $routeParams.projectID;


            var getCurrentProject = function () {
              ProjectService.get({}, {'id': self.pId}).$promise.then(
                      function (success) {
                        self.currentProject = success;
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

              });
            };

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

              $scope.newProject = {
                'projectName': self.currentProject.projectName,
                'description': self.currentProject.description,
                'services': self.selectionProjectTypes
              };

              ProjectService.update({id: self.currentProject.projectId}, $scope.newProject)
                      .$promise.then(
                              function (success) {
                                growl.success("Success: " + success.successMessage, {title: 'Success', ttl: 5000});
                                if (success.errorMsg) {
                                  growl.warning(success.errorMsg, {title: 'Error', ttl: 15000});
                                }

                                $modalStack.getTop().key.close();
                              }, function (error) {
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

            self.goToService = function (service) {
              if(service === "Zeppelin"){
                window.open("http://localhost:8080/hopsworks/zeppelin");
              }else{
                $location.path('project/' + self.pId + '/' + service.toLowerCase());
              }
              
            };

            self.goToSpecificDataset = function (name) {
              $location.path($location.path() + '/' + name);
            };


            self.saveAllowed = function () {
              if (self.currentProject.projectName.length == 0) {
                return true;
              }
            };

            // Dummy data
            $scope.labels = ["Adam", "Spark", "Yarn", "MapReduce", "Zeppelin", "Cuneiform"];


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