/*
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
 *
 */

/**
 * Controller for the python depedencies (settings page).
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('PythonDepsCtrl', ['$scope', '$route', '$routeParams', 'growl', '$location', 'PythonDepsService',
          'ModalService', '$interval', '$mdDialog',
          function ($scope, $route, $routeParams, growl, $location, PythonDepsService, ModalService, $interval, $mdDialog) {


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

            self.isRetryingFailedCondaOps = false;

            self.pythonKernelEnabled = "true";

            self.pythonVersion = "";


//            https://repo.continuum.io/pkgs/free/linux-64/
            self.condaChannel = "defaults";
            self.condaUrl = "defaults";
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
                        self.pythonVersion = success.data;
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
                        self.pythonVersion = version;
                        growl.success("Anaconda initialized for this project.", {title: 'Done', ttl: 5000});
                      }, function (error) {
                self.enabling = false;
                growl.error("Could not enable Anaconda", {title: 'Error', ttl: 5000});
              });
            };


            self.destroyAnaconda = function () {

              ModalService.confirm('sm', 'Remove Conda Environment?',
                      'You can re-create it again later.')
                      .then(function (success) {
                        self.enabling = true;
                        growl.success("Removing Anaconda for this project.....", {title: 'Done', ttl: 2000});
                        PythonDepsService.destroyAnaconda(self.projectId).then(
                                function (success) {
                                  self.enabled = true;
                                  self.enabling = false;
                                  self.getInstallationStatus();
                                  growl.success("Anaconda removed for this project.", {title: 'Done', ttl: 5000});
                                  $route.reload();
                                }, function (error) {
                          self.enabling = false;
                          growl.error("Could not remove Anaconda", {title: 'Error', ttl: 5000});
                        });
                      }, function (error) {

                      });
            }

            self.getInstalled = function () {

              PythonDepsService.index(self.projectId).then(
                      function (success) {
                        self.installedLibs = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
              });
            };

            self.failedHosts = function (opStatus) {

              var hostsJson = JSON.stringify(opStatus.hosts);

              if (hostsJson === '') {
                growl.error("Cant find any problemtic conda ops on the hosts.", {title: 'Error', ttl: 3000});
              } else {
                ModalService.viewJson('md', 'Problematic Hosts for Conda Operations',
                        hostsJson)
                        .then(function (success) {
                        }, function (error) {

                        });
              }
            };


            self.retryFailedCondaOps = function () {
              self.isRetryingFailedCondaOps = true;
              PythonDepsService.retryFailedCondaOps(self.projectId).then(
                      function (success) {
                        self.isRetryingFailedCondaOps = false;
                        self.getInstallationStatus();
                        growl.success("Retried failed conda ops for this project.", {title: 'Done', ttl: 3000});
                      }, function (error) {
                self.isRetryingFailedCondaOps = false;
                growl.error("Could not retry failed conda ops for this project.", {title: 'Error', ttl: 5000});
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

              PythonDepsService.clearCondaOps(self.projectId, data).then(
                      function (success) {
                        self.getInstalled();
                        growl.info("Clearing conda operations", {title: 'Clearing Conda Commands and Uninstalling Library', ttl: 3000});
                        PythonDepsService.uninstall(self.projectId, data).then(
                                function (success) {
//                                  self.getInstalled();
                                  self.uninstalling[lib] = false;
//                                  growl.info("Uninstalling conda library", {title: 'Uninstalling', ttl: 2000});
                                  PythonDepsService.clearCondaOps(self.projectId, data).then(
                                          function (success) {
                                            self.getInstalled();
                                          }, function (error) {
                                    // do nothing
                                  });
                                }, function (error) {
                          self.uninstalling[lib] = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 3000});
                        });
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

