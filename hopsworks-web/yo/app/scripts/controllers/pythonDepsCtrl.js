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
            self.installed = false;

            $scope.activeForm;

            self.resultsMsgShowing = false;

            self.resultsMsg = "";

            self.pythonVersionOpen = false;

            $scope.sortType = 'preinstalled';

            self.searchText = "";
            self.searching = false;
            self.installing = {};
            self.uninstalling = {};
            self.upgrading = {};
            self.enabling = false;

            self.searchResults = [];
            self.installedLibs = [];
            self.opsStatus = [];
            self.selectedInstallStatus = {};
            self.numEnvsNotEnabled = 0;
            self.numEnvs = 0;

            self.pythonKernelEnabled = "true";


//            https://repo.continuum.io/pkgs/free/linux-64/
            self.condaChannel = "default";
            self.condaUrl = "default";
            self.selectedLibs = {};

            self.selectedLib = {"channelUrl": self.condaChannel,
              "lib": "", "version": ""};

            self.progress = function () {
              var percent = ((self.numEnvs - self.numEnvsNotEnabled) / self.numEnvs) * 100;
              if (percent === 0) {
                return 5;
              }
              return percent;
            };

            var getInstallationStatus = function () {
              PythonDepsService.status(self.projectId).then(
                      function (success) {
                        self.opsStatus = success.data;

                        if (self.opsStatus.length === 0) {
                          if (self.resultsMessageShowing === true) {
                            // If there were operations outstanding, but now there are no outstanding ops, 
                            // then refresh installed libraries
                            self.getInstalled();
                          }
                          self.resultsMessageShowing = false;
                        }

                        var firstRun = false;
                        if (self.numEnvsNotEnabled === 0) {
                          firstRun = true;
                        }
                        self.numEnvsNotEnabled = 0;
                        var finished = {};
                        self.installing = {};

                        for (var i = 0; i < self.opsStatus.length; i++) {
                          console.log(self.opsStatus[i]);
                          if (self.opsStatus[i].status === "Installed") {
                            if (self.opsStatus[i].op === "INSTALL") {
                              self.installing[self.opsStatus[i].lib] = false;
                            } else if (self.opsStatus[i].op === "UNINSTALL") {
                              self.uninstalling[self.opsStatus[i].lib] = false;
                            } else if (self.opsStatus[i].op === "UPGRADE") {
                              self.upgrading[self.opsStatus[i].lib] = false;
                            }
                          }
                          if (self.opsStatus[i].status === "Installed") {
                            if (self.opsStatus[i].op === "INSTALL") {
                              finished[self.installing[i].lib] = false;
                            } else if (self.opsStatus[i].op === "UNINSTALL") {
                              finished[self.uninstalling[i].lib] = false;
                            } else if (self.opsStatus[i].op === "UPGRADE") {
                              finished[self.upgrading[i].lib] = false;
                            }
                          }
                          self.selectedInstallStatus[self.opsStatus[i].lib] =
                                  {"host": {"host": "none", "lib": "Not installed"}, "installing": false};
                          if (self.opsStatus[i].op === "CREATE" ||
                                  self.opsStatus[i].op === "REMOVE" ||
                                  self.opsStatus[i].op === "CLONE") {
                            self.numEnvsNotEnabled += 1;
                          }
                        }
                        if (firstRun === true) {
                          self.numEnvs = self.numEnvsNotEnabled;
                        }

// If all hosts have completed for a library, making it installing false.
                        var installed = true;
                        var uninstalled = true;
                        var upgraded = true;
                        for (var key in self.installing) {
                          if (finished[key] !== false) {
                            if (self.opsStatus[i].op === "INSTALL") {
                              installed = false;
                            } else if (self.opsStatus[i].op === "UNINSTALL") {
                              uninstalled = false;
                            } else if (self.opsStatus[i].op === "UPGRADE") {
                              upgraded = false;
                            }
                          }
                        }
                        self.installing[key] = installed;
                        self.uninstalling[key] = uninstalled;
                        self.upgrading[key] = upgraded;


                      }, function (error) {
                growl.error("Could not get installation status for libs", {title: 'Error', ttl: 3000});
              });
            };

            //this might be a bit to frequent for refresh rate 
            var getInstallationStatusInterval = $interval(function () {
              getInstallationStatus();
            }, 5000);

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
              PythonDepsService.installed(self.projectId).then(
                      function (success) {
                        self.installed = true;
                      }, function (error) {
                self.installed = false;
              });
            };
            self.init();

            self.enable = function (version) {
              self.enabling = true;
            PythonDepsService.enable(self.projectId, version, self.pythonKernelEnabled).then(
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
              self.selectedLib.channelUrl = self.condaChannel;
              self.searching = true;
              self.resultsMsg = "Conda searching can take a good few seconds......bear with us.";
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
                          self.selectedLibs[self.searchResults[i].lib] = {"version": {"version": self.searchResults[i].versions[0].version,
                              "status": self.searchResults[i].versions[0].status}, "installing": false};
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

              if (version.version === undefined || version.version === null || version.version.toUpperCase() === "NONE") {
                growl.error("Select a version to install from the dropdown list", {title: 'Error', ttl: 3000});
                return;
              }


              self.installing[host][lib] = true;

              var data = {"channelUrl": self.condaChannel, "lib": lib, "version": version.version};

              PythonDepsService.installOneHost(self.projectId, host, data).then(
                      function (success) {
                        self.installing[host][lib] = false;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
                self.installing[host][lib] = false;
              });

            };

            self.install = function (lib, version) {

              if (version.version === undefined || version.version === null || version.version.toUpperCase() === "NONE") {
                growl.error("Select a version to install from the dropdown list", {title: 'Error', ttl: 3000});
                return;
              }
              self.installing[lib] = true;

              var data = {"channelUrl": self.condaChannel, "lib": lib, "version": version.version};

              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        growl.success("Click on the 'Installed Python Libraries' tab for more info.", {title: 'Installing', ttl: 5000});
                        self.resultsMessageShowing = false;
                        self.searchResults = [];
                        self.getInstalled();
                        $scope.activeForm = 2;
                      }, function (error) {
                self.installing[lib] = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.uninstall = function (condaChannel, lib, version) {
              self.uninstalling[lib] = true;

              var data = {"channelUrl": condaChannel, "lib": lib, "version": version};
              PythonDepsService.uninstall(self.projectId, data).then(
                      function (success) {
                        self.getInstalled();
                        self.uninstalling[lib] = false;
                      }, function (error) {
                self.uninstalling[lib] = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.upgrade = function (condaChannel, lib, version) {
              self.upgrading[lib] = true;

              var data = {"channelUrl": condaChannel, "lib": lib, "version": version};
              PythonDepsService.upgrade(self.projectId, data).then(
                      function (success) {
                        growl.success("Sending command to update: " + lib, {title: 'Updating', ttl: 3000});
                        self.getInstalled();
                        self.upgrading[lib] = false;
                      }, function (error) {
                self.upgrading[lib] = false;
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };



          }]);

