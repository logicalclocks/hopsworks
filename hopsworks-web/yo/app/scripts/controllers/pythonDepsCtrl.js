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


            $scope.sortType = 'lib';

            self.searchText = "";
            self.searching = false;
            self.installing = false;
            self.uninstalling = false;
            self.updating = false;
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
//                          self.resultsMsg = "All libraries appear to be fully installed.";
                        }
                        self.numEnvsNotEnabled = 0;
                        for (var i = 0; i < self.installingStatus.length; i++) {
                          self.selectedInstallStatus[self.installingStatus[i].lib] =
                                  {"host": {"host": "none", "lib": "Not installed"}, "installing": false};
                          if (self.installingStatus[i].op === "CREATE" ||
                                  self.installingStatus[i].op === "REMOVE" ||
                                  self.installingStatus[i].op === "CLONE") {
                            self.numEnvsNotEnabled += 1;
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


//            self.libStatus = function (ev, lib, version) {
//              
//              PythonDepsService.status(self.projectId, lib, version).then(
//                      function (success) {
//                        self.installedLibs = success.data;
//              // ask install status of this library for all nodes
//              // In the success handler, draw the dialog
//              $mdDialog.show(
//                      $mdDialog.alert()
//                      .parent(angular.element(document.querySelector('#popupContainer')))
//                      .clickOutsideToClose(true)
//                      .title('Pre-installed Python Libraries')
//                      .textContent('')
//                      .ok('Ack!')
//                      .targetEvent(ev)
//                      );
//                      }, function (error) {
//                self.enabled = false;
//              });
//            };

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

            self.install = function (lib, version) {

              self.installing = true;
              self.selectedLibs[lib].installing = true;

              var data = {"channelUrl": self.condaUrl, "lib": lib, "version": version.version};

              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        self.installing = false;
                        growl.success("Installation done.", {title: 'Success', ttl: 3000});
                        self.resultsMessageShowing = true;
                        self.resultsMsg = "Successfully installed: " + lib + " version: " + version;
                        self.searchResults = [];
                        self.selectedLibs[lib].installing = false;
//                        for (var i = 0; i < self.searchResults.length; i++) {
//                          if (self.searchResults[i].lib === data.lib) {
//                            self.searchResults[i].installed = "Installed";
//                          }
//                        }

                      }, function (error) {
                self.installing = false;
                self.selectedLibs[lib].installing = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.uninstall = function (condaUrl, lib, version) {
              self.uninstalling = true;

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
              self.upgrading = true;

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

