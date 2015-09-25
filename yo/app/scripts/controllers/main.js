/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('MainCtrl', ['$cookies', '$location', 'AuthService', 'UtilsService', 'ElasticService', 'md5', 'ModalService', 'ProjectService', 'growl', '$routeParams',
          function ($cookies, $location, AuthService, UtilsService, ElasticService, md5, ModalService, ProjectService, growl, $routeParams) {

            var self = this;
            self.email = $cookies['email'];
            self.emailHash = md5.createHash(self.email || '');
            var elasticService = ElasticService();

            if(!angular.isUndefined($routeParams.datasetName)){
              self.searchType = "datasetCentric";
            }
            else if(!angular.isUndefined($routeParams.projectID)){
              self.searchType = "projectCentric";
            }
            else {
              self.searchType = "global";
            }
            
            self.logout = function () {
              AuthService.logout(self.user).then(
                      function (success) {
                        $location.url('/login');
                        delete $cookies.email;
                        localStorage.removeItem("SESSIONID");
                        sessionStorage.removeItem("SESSIONID");
                      }, function (error) {
                self.errorMessage = error.data.msg;
              });
            };

            self.profileModal = function () {
              ModalService.profile('md');
            };

            self.sshKeysModal = function () {
              ModalService.sshKeys('lg');
            };

            self.getHostname = function () {
              return $location.host();
            };

            self.getUser = function () {
              return self.email.substring(0, self.email.indexOf("@"));
            };

            self.view = function (name, id, dataType) {

              if (dataType === 'project') {
                ProjectService.getProjectInfo({projectName: name}).$promise.then(
                        function (success) {

                          ModalService.viewSearchResult('md', success, dataType)
                                  .then(function (success) {
                                    growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                  }, function (error) {

                                  });
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                });
              } else if (dataType === 'dataset') {
                //fetch the dataset
                ProjectService.getDatasetInfo({inodeId: id}).$promise.then(
                        function (response) {
                          var projects;

                          //fetch the projects to pass them in the modal. Fixes empty projects array on ui-select initialization
                          ProjectService.query().$promise.then(
                                  function (success) {
                                    projects = success;

                                    //show dataset
                                    ModalService.viewSearchResult('md', response, dataType, projects)
                                            .then(function (success) {
                                              growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                            }, function (error) {

                                            });
                                  }, function (error) {
                            console.log('Error: ' + error);
                          });

                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                });
              }
            };

            self.searchTerm = "";
            self.searchReturned = "";
            self.searchResult = [];
            self.resultPages = 0;
            self.resultItems = 0;
            self.currentPage = 1;
            self.pageSize = 5;

            self.hitEnter = function (evt) {
              if (angular.equals(evt.keyCode, 13)) {
                self.search();
              }
            };

            self.keyTyped = function (evt) {

              if (self.searchTerm.length > 3) {
                self.search();
              }
              else {
                self.searchResult = [];
                self.searchReturned = "";
              }
            };

            self.search = function () {
              //ask for the project name when it is time to search
              self.projectName = UtilsService.getProjectName();
              self.currentPage = 1;
              self.pageSize = 5;
              self.searchResult = [];
              self.searchReturned = "";

              if (self.searchType === "global") {
                //triggering a global search
                elasticService.globalSearch(self.searchTerm)
                        .then(function (response) {

                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE " + JSON.stringify(response));
                          if (searchHits.length > 0) {
                            self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            self.searchResult = searchHits;
                          } else {
                            self.searchResult = [];
                            self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                          }
                          self.resultPages = Math.ceil(self.searchResult.length / self.pageSize);
                          self.resultItems = self.searchResult.length;

                        }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
              } else if (self.searchType === "projectCentric") {
                elasticService.projectSearch(UtilsService.getProjectName(), self.searchTerm)
                        .then(function (response) {
                  
                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE " + JSON.stringify(response));
                          if (searchHits.length > 0) {
                            self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            self.searchResult = searchHits;
                          } else {
                            self.searchResult = [];
                            self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                          }
                          self.resultPages = Math.ceil(self.searchResult.length / self.pageSize);
                          self.resultItems = self.searchResult.length;
                          
                        }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
              }else if (self.searchType === "datasetCentric"){
                elasticService.datasetSearch(UtilsService.getDatasetName(), self.searchTerm)
                        .then(function (response) {
                  
                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE " + JSON.stringify(response));
                          if (searchHits.length > 0) {
                            self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            self.searchResult = searchHits;
                          } else {
                            self.searchResult = [];
                            self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                          }
                          self.resultPages = Math.ceil(self.searchResult.length / self.pageSize);
                          self.resultItems = self.searchResult.length;
                          
                        }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
              }
              
              datePicker();// this will load the function so that the date picker can call it.
            };
            var datePicker = function () {
              $(function () {
                $('#datetimepicker1').datetimepicker();
              });
            };
          }]);