/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('MainCtrl', ['$interval','$cookies', '$location','$scope', 'AuthService', 'UtilsService', 'ElasticService', 'md5', 'ModalService','ProjectService','growl','MessageService','$routeParams', '$window',
          function ($interval, $cookies, $location, $scope, AuthService, UtilsService, ElasticService, md5, ModalService, ProjectService, growl, MessageService, $routeParams, $window) {

            var self = this;
            self.email = $cookies.get('email');
            self.emailHash = md5.createHash(self.email || '');
            var elasticService = ElasticService();

            if (!angular.isUndefined($routeParams.datasetName)) {
              self.searchType = "datasetCentric";
            } else if (!angular.isUndefined($routeParams.projectID)) {
              self.searchType = "projectCentric";
            } else {
              self.searchType = "global";
            }
            
            self.isAdmin = function () {
              return $cookies.get('isAdmin');
            };
                              
            self.goToAdminPage = function () {
              $window.location.href = '/hopsworks/security/protected/admin/adminIndex.xhtml';
            };

            self.getEmailHash = function(email) {
              return md5.createHash(email || '');
            };
            
            self.logout = function () {
              AuthService.logout(self.user).then(
                      function (success) {
                        $location.url('/login');
                        $cookies.remove("email");
                        $cookies.remove("isAdmin");
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

            var getUnreadCount = function () {
              MessageService.getUnreadCount().then(
                      function (success) {
                        self.unreadMessages = success.data.data.value;
                      }, function (error) {
              });
            };
            
            var getMessages = function () {
              MessageService.getMessages().then(
                      function (success) {
                        self.messages = success.data;
                      }, function (error) {
              });
            };
            getUnreadCount();
            getMessages();
            //this might be a bit to frequent for refresh rate 
            var getUnreadCountInterval = $interval(function () {
              getUnreadCount();
            }, 10000);
            self.getMessages = function () {
              getMessages();
            };
            self.openMessageModal = function (selected) {
              if (selected !== undefined) {
                MessageService.markAsRead(selected.id);
              };
              ModalService.messages('lg', selected)
                      .then(function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000})
                      }, function (error) { });
            };

            self.searchTerm = "";
            self.searching = false;
            self.globalClusterBoundary = false;
            self.searchReturned = "";
            self.searchResult = [];
            self.resultPages = 0;
            self.resultItems = 0;
            self.currentPage = 1;
            self.pageSize = 16;
            self.hitEnter = function (evt) {
              if (angular.equals(evt.keyCode, 13)) {
                self.search();
              }
            };

            self.keyTyped = function (evt) {

              if (self.searchTerm.length > 3) {
                self.search();
              } else {
                self.searchResult = [];
                self.searchReturned = "";
              }
            };

            self.search = function () {
              //ask for the project name when it is time to search
              self.projectName = UtilsService.getProjectName();
              self.currentPage = 1;
              self.pageSize = 16;
              self.searchResult = [];
              self.searchReturned = "";

              if (self.searchTerm === undefined || self.searchTerm === "" || self.searchTerm === null) {
                return;
              }
              self.searching = true;
              if (self.searchType === "global") {
                //triggering a global search
                elasticService.globalSearch(self.searchTerm)
                        .then(function (response) {
                          self.searching = false;
                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE ", response);
                          if (searchHits.length > 0) {
                            if (self.globalClusterBoundary) {
                              self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            } else {
                              self.searchReturned = "Result for <b>" + self.searchTerm + "</b>";
                            }
                            self.searchResult = searchHits;
                          } else {
                            self.searchResult = [];
                            if (self.globalClusterBoundary) {
                              self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                            } else {
                              self.searchReturned = "No result found for <b>" + self.searchTerm + "</b>";
                            }
                          }
                          self.resultPages = Math.ceil(self.searchResult.length / self.pageSize);
                          self.resultItems = self.searchResult.length;

                        }, function (error) {
                  self.searching = false;
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
              } else if (self.searchType === "projectCentric") {
                elasticService.projectSearch(UtilsService.getProjectName(), self.searchTerm)
                        .then(function (response) {
                          self.searching = false;
                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE ", response);
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
                          self.searching = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
              } else if (self.searchType === "datasetCentric") {
                elasticService.datasetSearch($routeParams.projectID, UtilsService.getDatasetName(), self.searchTerm)
                        .then(function (response) {
                          self.searching = false;
                          var searchHits = response.data;
                          //console.log("RECEIVED RESPONSE ", response);
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
                          self.searching = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
              }
              datePicker();// this will load the function so that the date picker can call it.
            };
                
            var datePicker = function () {
              $(function () {
                $('[type="datepicker"]').datetimepicker({format: 'DD/MM/YYYY'});
                $("#datepicker1").on("dp.change", function (e) {
                  $('#datepicker2').data("DateTimePicker").minDate(e.date);
                });
                $("#datepicker2").on("dp.change", function (e) {
                  $('#datepicker1').data("DateTimePicker").maxDate(e.date);
                });
                $("#datepicker3").on("dp.change", function (e) {
                  $('#datepicker4').data("DateTimePicker").minDate(e.date);
                });
                $("#datepicker4").on("dp.change", function (e) {
                  $('#datepicker3').data("DateTimePicker").maxDate(e.date);
                });
              });
            };                       
            
            self.viewType = function (listView) {
              if (listView) {
                self.pageSize = 4;
              } else {
                self.pageSize = 16;
              }
            };

            $scope.$on("$destroy", function () {
              $interval.cancel(getUnreadCountInterval);
            });
                              
            self.incrementPage = function () {
              self.pageSize = self.pageSize + 1;
            };

            self.decrementPage = function () {
              if (self.pageSize < 2) {
                return;
              }
              self.pageSize = self.pageSize - 1;
            };
            
            self.viewDelail = function(result) {
              if (result.type === 'proj') {
                ProjectService.getProjectInfo({projectName: result.name}).$promise.then(
                  function (response) {
                    ModalService.viewSearchResult('lg', response, result);
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                });
              } else if (result.type === 'ds') {
                ProjectService.getDatasetInfo({inodeId: result.id}).$promise.then(
                  function (response) {
                    var projects;
                    console.log(response);
                    ProjectService.query().$promise.then(
                      function (success) {
                        projects = success;
                        ModalService.viewSearchResult('lg', response, result, projects);
                      }, function (error) {
                      growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                    });
                  });
              }
            };            
}]);
