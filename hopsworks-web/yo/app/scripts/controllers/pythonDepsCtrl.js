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

            self.condaResultsMsgShowing = false;

            self.condaResultsMsg = "";

            self.pipResultsMsgShowing = false;

            self.pipResultsMsg = "";

            self.pythonVersionOpen = false;

            self.exporting = false;

            $scope.sortType = 'preinstalled';

            self.pipSearching = false;
            self.condaSearching = false;
            self.installing = {};
            self.uninstalling = {};
            self.upgrading = {};
            self.enabling = false;

            self.condaSearchResults = [];
            self.pipSearchResults = [];
            self.installedLibs = [];
            self.opsStatus = [];
            self.selectedInstallStatus = {};
            self.numEnvsNotEnabled = 0;
            self.numEnvs = 0;

            self.isRetryingFailedCondaOps = false;

            self.pythonKernelEnabled = "true";

            self.pythonVersion = "0.0";


            //            https://repo.continuum.io/pkgs/free/linux-64/
            self.condaChannel = "defaults";

            self.condaSelectedLibs = {};
            self.pipSelectedLibs = {};

            self.machineTypeALL = true;
            self.machineTypeCPU = false;
            self.machineTypeGPU = false;
            self.machineTypeVal = "ALL"

            self.environmentTypes = {};

            self.environmentYmlDef = {};

            self.pipSelectedLib = {
              "channelUrl": self.condaChannel,
              "installType": "PIP",
              "lib": "",
              "version": ""
            };
            self.condaSelectedLib = {
              "channelUrl": self.condaChannel,
              "installType": "CONDA",
              "lib": "",
              "version": ""
            };

            //bit ugly code but some custom behaviour was needed to fix the checkboxes:
            //1. unchecking GPU/CPU should default to ALL
            //2. it should not be possible to uncheck the ALL option
            self.select = function (machineType) {
              // you selected CPU only
              if (self.machineTypeCPU === false && machineType === "CPU") {
                self.machineTypeVal = "CPU";
                self.machineTypeCPU = true;
                self.machineTypeGPU = false;
                self.machineTypeALL = false;
                // you selected GPU only
              } else if (self.machineTypeGPU === false && machineType === "GPU") {
                self.machineTypeVal = "GPU";
                self.machineTypeCPU = false;
                self.machineTypeGPU = true;
                self.machineTypeALL = false;
                // you unchecked CPU
              } else if (self.machineTypeCPU === true && machineType === "CPU") {
                self.machineTypeVal = "ALL";
                self.machineTypeCPU = false;
                self.machineTypeGPU = false;
                self.machineTypeALL = true;
                // you unchecked GPU
              } else if (self.machineTypeGPU === true && machineType === "GPU") {
                self.machineTypeVal = "ALL";
                self.machineTypeCPU = false;
                self.machineTypeGPU = false;
                self.machineTypeALL = true;
              } else {
                self.machineTypeVal = "ALL";
                self.machineTypeCPU = false;
                self.machineTypeGPU = false;
                self.machineTypeALL = true;
              }
            };

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
                          self.selectedInstallStatus[self.opsStatus[i].lib] = {
                            "host": {
                              "host": "none",
                              "lib": "Not installed"
                            },
                            "installing": false
                          };
                          if (self.opsStatus[i].op === "CREATE" ||
                                  self.opsStatus[i].op === "YML" ||
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


                      },
                      function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            getInstallationStatus();

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
              PythonDepsService.installed(self.projectId).then(
                      function (success) {
                        self.condaChannel = success.data;
                        self.installed = true;
                      },
                      function (error) {
                        self.installed = false;
                      });
              PythonDepsService.enabled(self.projectId).then(
                      function (success) {
                        self.enabled = true;
                        self.pythonVersion = success.data;
                      },
                      function (error) {
                        self.enabled = false;
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
                        growl.success("Anaconda initialized for this project.", {
                          title: 'Done',
                          ttl: 5000
                        });
                      },
                      function (error) {
                        self.enabling = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            self.destroyAnaconda = function () {

              ModalService.confirm('sm', 'Remove Conda Environment?',
                      'WARNING: This will shutdown any running Jupyter notebooks. You can re-create your conda environment again later.')
                      .then(function (success) {
                        self.enabling = true;
                        growl.success("Removing Conda environment for this project.....", {
                          title: 'Done',
                          ttl: 2000
                        });
                        PythonDepsService.destroyAnaconda(self.projectId).then(
                                function (success) {
                                  self.enabled = true;
                                  self.enabling = false;
                                  self.getInstallationStatus();
                                  growl.success("Anaconda removed for this project.", {
                                    title: 'Done',
                                    ttl: 5000
                                  });
                                  $route.reload();
                                },
                                function (error) {
                                  self.enabling = false;
                                  if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                                  } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 5000});
                                  }
                                });
                      }, function (error) {

                      });
            }

            self.getInstalled = function () {

              PythonDepsService.index(self.projectId).then(
                      function (success) {
                        self.installedLibs = success.data;
                        console.log(self.installedLibs);
                      },
                      function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            self.failedHosts = function (opStatus) {

              var hostsJson = JSON.stringify(opStatus.hosts);

              if (hostsJson === '') {
                if (typeof error.data.usrMsg !== 'undefined') {
                  growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                } else {
                  growl.error("", {title: error.data.errorMsg, ttl: 5000});
                }
              } else {
                ModalService.viewJson('md', 'Problematic Hosts for Conda Operations',
                        hostsJson)
                        .then(function (success) {}, function (error) {

                        });
              }
            };

            self.exportEnvironment = function () {
              self.exporting = true;
              PythonDepsService.exportEnvironment(self.projectId).then(
                      function (success) {
                        self.exporting = false;
                        growl.success("Exporting environment completed successfully. Check your Resources dataset for the .yml file(s)", {
                          title: 'Done',
                          ttl: 20000
                        });
                      },
                      function (error) {
                        self.exporting = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            }

            //Set some (semi-)constants
            self.selectFileRegexes = {
              "yml": /.yml\b/
            };

            self.selectFileErrorMsgs = {
              "yml": "Please select a .yml file. It should have be processable by 'conda env create' command"
            };

            this.selectYmlFile = function () {
              ModalService.selectEnvironmentYml('lg', self.selectFileRegexes['yml'.toUpperCase()], self.selectFileErrorMsgs['yml'.toUpperCase()]).then(
                      function (success) {

                        self.environmentYmlDef = success;
                        self.enabling = true;

                        PythonDepsService.enableYml(self.projectId, self.environmentYmlDef).then(
                                function (success) {
                                  self.enabled = true;
                                  self.enabling = false;
                                  self.getInstallationStatus();
                                  self.pythonVersion = '0.0';
                                  growl.success("Anaconda initialized for this project.", {
                                    title: 'Done',
                                    ttl: 5000
                                  });
                                }, function (error) {
                          self.enabling = false;
                          if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                          } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 5000});
                          }
                        });

                      },
                      function (error) {
                        //The user changed their mind.
                      });
            };

            self.retryFailedCondaOps = function () {
              self.isRetryingFailedCondaOps = true;
              PythonDepsService.retryFailedCondaOps(self.projectId).then(
                      function (success) {
                        self.isRetryingFailedCondaOps = false;
                        self.getInstallationStatus();
                        growl.success("Retried failed conda ops for this project.", {
                          title: 'Done',
                          ttl: 3000
                        });
                      },
                      function (error) {
                        self.isRetryingFailedCondaOps = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            self.search = function (type) {

              if (type === "PIP") {

                if (self.pipSelectedLib.lib.length < 3) {
                  return;
                }
                self.pipSelectedLib.channelUrl = self.condaChannel;
                self.pipSelectedLib.installType = "PIP"
                self.pipSearching = true;
                self.pipResultsMsg = "Pip search can take a good few seconds... bear with us.";
                self.pipResultsMessageShowing = true;

                PythonDepsService.search(self.projectId, self.pipSelectedLib).then(
                        function (success) {
                          self.pipSearching = false;
                          self.pipSearchResults = success.data;
                          self.pipSelectedLibs = {};
                          if (self.pipSearchResults.length === 0) {
                            self.pipResultsMsg = "No results found.";
                          } else {
                            self.pipResultsMessageShowing = false;
                          }
                          for (var i = 0; i < self.pipSearchResults.length; i++) {
                            self.pipSelectedLibs[self.pipSearchResults[i].lib] = {
                              "version": {
                                "version": self.pipSearchResults[i].versions[0].version,
                                "status": self.pipSearchResults[i].versions[0].status
                              },
                              "installing": false
                            };
                          }

                        },
                        function (error) {
                          self.pipSearching = false;
                          if (error.status == 204) {
                            self.pipResultsMsg = "No results found.";
                          } else {
                            self.pipResultsMessageShowing = false;
                            if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                            } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 5000});
                            }
                          }
                        });
              } else if (type === "CONDA") {
                if (self.condaSelectedLib.lib.length < 3) {
                  return;
                }
                self.condaSelectedLib.channelUrl = self.condaChannel;
                self.condaSelectedLib.installType = "CONDA"
                self.condaSearching = true;
                self.condaResultsMsg = "Conda search can take a good few seconds... bear with us.";
                self.condaResultsMessageShowing = true;

                PythonDepsService.search(self.projectId, self.condaSelectedLib).then(
                        function (success) {
                          self.condaSearching = false;
                          self.condaSearchResults = success.data;
                          self.condaSelectedLibs = {};
                          if (self.condaSearchResults.length === 0) {
                            self.condaResultsMsg = "No results found.";
                          } else {
                            self.condaResultsMessageShowing = false;
                          }
                          for (var i = 0; i < self.condaSearchResults.length; i++) {
                            self.condaSelectedLibs[self.condaSearchResults[i].lib] = {
                              "version": {
                                "version": self.condaSearchResults[i].versions[0].version,
                                "status": self.condaSearchResults[i].versions[0].status
                              },
                              "installing": false
                            };
                          }

                        },
                        function (error) {
                          self.condaSearching = false;
                          if (error.status == 204) {
                            self.condaResultsMsg = "No results found.";
                          } else {
                            self.condaResultsMessageShowing = false;
                            if (typeof error.data.usrMsg !== 'undefined') {
                              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                            } else {
                              growl.error("", {title: error.data.errorMsg, ttl: 5000});
                            }
                          }
                        });

              }

            };


            self.installOneHost = function (lib, version, host) {

              if (version.version === undefined || version.version === null || version.version.toUpperCase() === "NONE") {
                growl.error("Select a version to install from the dropdown list", {
                  title: 'Error',
                  ttl: 3000
                });
                return;
              }


              self.installing[host][lib] = true;

              var data = {
                "channelUrl": self.condaChannel,
                "lib": lib,
                "version": version.version
              };

              PythonDepsService.installOneHost(self.projectId, host, data).then(
                      function (success) {
                        self.installing[host][lib] = false;
                      },
                      function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                        self.installing[host][lib] = false;
                      });

            };



            self.install = function (lib, installType, machineType, version) {

              if (version.version === undefined || version.version === null || version.version.toUpperCase() === "NONE") {
                growl.error("Select a version to install from the dropdown list", {
                  title: 'Error',
                  ttl: 3000
                });
                return;
              }
              self.installing[lib] = true;
              if (installType === "CONDA") {
                var data = {
                  "channelUrl": self.condaChannel,
                  "installType": installType,
                  "machineType": machineType,
                  "lib": lib,
                  "version": version.version
                };
              } else {
                var data = {
                  "channelUrl": "PyPi",
                  "installType": installType,
                  "machineType": machineType,
                  "lib": lib,
                  "version": version.version
                };
              }

              PythonDepsService.install(self.projectId, data).then(
                      function (success) {
                        growl.success("Click on the 'Installed Python Libraries' tab for more info.", {
                          title: 'Installing',
                          ttl: 5000
                        });
                        self.pipResultsMessageShowing = false;
                        self.condaResultsMessageShowing = false;
                        self.pipSearchResults = [];
                        self.condaSearchResults = [];
                        self.getInstalled();
                        $scope.activeForm = 2;
                        var msg = success.data;
                        if (msg !== undefined && msg !== null && msg.length > 0) {
                          growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                        }
                      },
                      function (error) {
                        self.installing[lib] = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            self.uninstall = function (condaChannel, machineType, installType, lib, version) {


              self.uninstalling[lib] = true;
              var data = {
                "channelUrl": condaChannel,
                "installType": installType,
                "machineType": machineType,
                "lib": lib,
                "version": version
              };

              PythonDepsService.clearCondaOps(self.projectId, data).then(
                      function (success) {
                        self.getInstalled();
                        growl.info("Clearing conda operations", {
                          title: 'Clearing Conda Commands and Uninstalling Library',
                          ttl: 3000
                        });
                        PythonDepsService.uninstall(self.projectId, data).then(
                                function (success) {
                                  self.getInstalled();
                                  self.uninstalling[lib] = false;
                                  var msg = success.data;
                                  if (msg !== undefined && msg !== null && msg.length > 0) {
                                    growl.info(msg, {title: "Creating a new Conda Env", ttl: 10000});
                                  }
                                },
                                function (error) {
                                  self.uninstalling[lib] = false;
                                  if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                                  } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 5000});
                                  }
                                });
                      },
                      function (error) {
                        self.uninstalling[lib] = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };


            self.upgrade = function (condaChannel, machineType, installType, lib, version) {
              self.upgrading[lib] = true;
              growl.success("VAL: " + machineType, {
                title: 'ERROR',
                ttl: 3000
              });
              var data = {
                "channelUrl": condaChannel,
                "installType": installType,
                "machineType": machineType,
                "lib": lib,
                "version": version
              };
              PythonDepsService.upgrade(self.projectId, data).then(
                      function (success) {
                        growl.success("Sending command to update: " + lib, {
                          title: 'Updating',
                          ttl: 3000
                        });
                        self.getInstalled();
                        self.upgrading[lib] = false;
                      },
                      function (error) {
                        self.upgrading[lib] = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                        }
                      });
            };

            PythonDepsService.environmentTypes(self.projectId).then(
                    function (success) {
                      self.environmentTypes = success.data;
                    },
                    function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                      } else {
                        growl.error("", {title: error.data.errorMsg, ttl: 5000});
                      }
                    });
          }
        ]);