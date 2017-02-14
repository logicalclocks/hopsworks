/**
 * Controller for the python depedencies (settings page).
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('PythonDepsCtrl', ['$scope', '$routeParams', 'growl', '$location', 'PythonDepsService', '$interval', '$mdDialog',
          function ($scope, $routeParams, growl, $location, PythonDepsService, $interval, $mdDialog) {


            var self = this;
            self.projectId = $routeParams.projectID;

            self.active = 0;

            self.enabled = false;
            self.resultsMsgShowing = false;

            self.resultsMsg = "";


            $scope.sortType = 'preinstalled';

            self.searchText = "";
            self.searching = false;
            self.installing = {};
            self.uninstalling = {};
            self.updating = {};
            self.enabling = false;

            self.searchResults = [];
            self.installedLibs = [];
            self.installingStatus = [];
            self.selectedInstallStatus = {};
            self.numEnvsNotEnabled = 0;

//            https://repo.continuum.io/pkgs/free/linux-64/
            self.condaUrl = "default";
            self.selectedLibs = {};

            self.selectedLib = {"channelUrl": self.condaUrl,
              "lib": "", "version": ""};


            var getInstallationStatus = function () {
              PythonDepsService.status(self.projectId).then(
                      function (success) {
                        self.installingStatus = success.data;
                        if (self.installingStatus.length === 0) {
                          self.resultsMessageShowing = false;
                        }
                        self.numEnvsNotEnabled = 0;
                        var finished = {};
                        
                        for (var i = 0; i < self.installingStatus.length; i++) {
                          console.log(self.installingStatus[i]);
                          finished[self.installingStatus[i].lib] = false;
                          self.selectedInstallStatus[self.installingStatus[i].lib] =
                                  {"host": {"host": "none", "lib": "Not installed"}, "installing": false};
                          if (self.installingStatus[i].op === "CREATE" ||
                                  self.installingStatus[i].op === "REMOVE" ||
                                  self.installingStatus[i].op === "CLONE") {
                            self.numEnvsNotEnabled += 1;
                          }
                        }

// If all hosts have completed for a library, making it installing false.
                        for (var key in self.installing) {
                          if (finished[key] !== false) {
                            self.installing[key] = false;
                          }
                        }                        
                      
                      
                      
                      }, function (error) {
                growl.error("Could not get installation status for libs", {title: 'Error', ttl: 3000});
              });
            };

            //this might be a bit to frequent for refresh rate 
            var getInstallationStatusInterval = $interval(function () {
              getInstallationStatus();
            }, 5000);

            getInstallationStatus();

            self.getInstallationStatus = function () {
              getInstallationStatus();
            };
            $scope.$on("$destroy", function () {
              $interval.cancel(getInstallationStatusInterval);
            });


            self.init = function () {
              PythonDepsService.enabled(self.projectId).then(
                      function (success) {
                        self.enabled = true;
                      }, function (error) {
                self.enabled = false;
              });
            };
            self.init();

            self.enable = function () {
              self.enabling = true;
              PythonDepsService.enable(self.projectId).then(
                      function (success) {
                        self.enabled = true;
                        self.enabling = false;
                        self.getInstallationStatus();
                        growl.success("Anaconda initialized for this project.", {title: 'Done', ttl: 5000});
                      }, function (error) {
                self.enabling = false;
                growl.error("Could not enable Anaconda", {title: 'Error', ttl: 5000});
              });
            };


            self.getInstalled = function () {

              PythonDepsService.index(self.projectId).then(
                      function (success) {
                        self.installedLibs = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };


            self.search = function () {

              if (self.selectedLib.lib.length < 3) {
                return;
              }
              self.searching = true;
              self.resultsMsg = "Conda searching can take a good few seconds......bear with us."
              self.resultsMessageShowing = true;

              PythonDepsService.search(self.projectId, self.selectedLib).then(
                      function (success) {
                        self.searching = false;
                        self.searchResults = success.data;
                        if (self.searchResults.length === 0) {
                          self.resultsMsg = "No results found.";
                        } else {
                          self.resultsMessageShowing = false;
                        }
                        for (var i = 0; i < self.searchResults.length; i++) {
                          self.selectedLibs[self.searchResults[i].lib] = {"version": {"version": "none", "status": "Not installed"}, "installing": false};
                        }

                      }, function (error) {
                self.searching = false;
                if (error.status == 204) {
                  self.resultsMsg = "No results found.";
                } else {
                  self.resultsMessageShowing = false;
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
                }
              });
            };


            self.installOneHost = function (lib, version, host) {
              
              var data = {"channelUrl": self.condaUrl, "lib": lib, "version": version.version};

              PythonDepsService.installOneHost(self.projectId, host, data).then(
                      function (success) {
                        self.installing[lib] = false;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });

            };
            
            self.install = function (lib, version) {

              self.installing[lib] = true;

              var data = {"channelUrl": self.condaUrl, "lib": lib, "version": version.version};

              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        growl.success("Installation started. Click on the 'Ongoing Installation Status' tab for more info.", {title: 'Installing', ttl: 3000});
                        self.resultsMessageShowing = false;
                        self.searchResults = [];
//                        self.installing[lib] = false;

                      }, function (error) {
                self.installing[lib] = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.uninstall = function (condaUrl, lib, version) {
              self.uninstalling[lib] = true;

              var data = {"channelUrl": condaUrl, "lib": lib, "version": version};
              PythonDepsService.uninstall(self.projectId, data).then(
                      function (success) {
                        self.uninstalling = false;

                        growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                      }, function (error) {
                self.uninstalling = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.upgrade = function (condaUrl, lib, version) {
              self.upgrading[lib] = true;

              var data = {"channelUrl": condaUrl, "lib": lib, "version": version};
              PythonDepsService.upgrade(self.projectId, data).then(
                      function (success) {
                        self.upgrading = false;
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 3000});
                      }, function (error) {
                self.upgrading = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };



          }]);

