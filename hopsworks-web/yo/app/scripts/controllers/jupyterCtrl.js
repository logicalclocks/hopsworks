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
'use strict';

angular.module('hopsWorksApp')
    .controller('JupyterCtrl', ['$scope', '$routeParams', '$route',
        'growl', 'ModalService', '$interval', 'JupyterService', 'StorageService', '$location',
        '$timeout', '$window', '$sce', 'PythonService', 'TourService', 'UserService', 'VariablesService',
        function($scope, $routeParams, $route, growl, ModalService, $interval, JupyterService,
            StorageService, $location, $timeout, $window, $sce, PythonService, TourService, UserService, VariablesService) {

            var self = this;
            self.loading = false;
            self.loadingText = "";
            self.jupyterServer;
            self.projectName;
            self.tourService = TourService;
            self.tourService.currentStep_TourEight = 0;
            self.projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            self.ui = "";
            self.condaEnabled = true;
            self.jupyterInstalled = true;
            $scope.sessions = null;
            self.jupyterSettings = {};
            $scope.tgState = true;
            self.config = {};
            self.numNotEnabledEnvs = 0;
            self.opsStatus = {};
            self.pythonVersion;

            self.hasDockerMemory = false;
            self.maxDockerMemory = 1024;
            self.hasDockerCores = false;
            self.maxDockerCores = 1;

            self.dirs = [{
                id: 1,
                name: '/'
            }, {
                id: 2,
                name: '/Jupyter/'
            }];
            self.selected = self.dirs[1];


            self.shutdownLevelSelected;
            self.timeLeftInMinutes = 0;
            self.addShutdownHours;

            self.job = {
                'type': '',
                'name': '',
                'id': '',
                'project': {
                    'name': '',
                    'id': self.projectId
                }
            };

            self.third_party_api_keys = [];
            self.git_api_key;
            self.gitWorking = false;
            self.gitNewHeadBranchPressed = false;
            
            self.gitRepoStatus = {
                status: 'UNKNOWN',
                modifiedFiles: -1,
                branch: 'UNKNOWN',
                repository: 'UNKNOWN'
            };

            self.getDockerMaxAllocation = function () {
              VariablesService.getVariable('kube_docker_max_memory_allocation')
                .then(function (success) {
                  self.hasDockerMemory = true;
                  self.maxDockerMemory = parseInt(success.data.successMessage);
                }, function (error) {
                  self.hasDockerMemory = false;
                  self.maxDockerMemory = -1;
              });
              VariablesService.getVariable('kube_docker_max_cores_allocation')
                .then(function (success) {
                  self.hasDockerCores = true;
                  self.maxDockerCores = parseInt(success.data.successMessage);
                }, function (error) {
                  self.hasDockerCores = false;
                  self.maxDockerCores = -1;
              });
            };

            self.range = function (max) {
                var input = [];
                for (var i = 1; i <= max; i++) {
                    input.push(i);
                }
                return input;
            };

            self.changeShutdownLevel = function() {
                self.jupyterSettings.shutdownLevel = self.shutdownLevelSelected.name;
            };

            self.updateShutdownLevel = function() {
                var currentHours = self.jupyterSettings.shutdownLevel;

                self.jupyterSettings.shutdownLevel = Number(self.shutdownLevelSelected.name);

                self.loadingText = "Updating Jupyter Shutdown Time";
                JupyterService.update(self.projectId, self.jupyterSettings).then(
                    function(success) {
                        JupyterService.running(self.projectId).then(
                            function(success) {
                                self.config = success.data;
                                timeToShutdown();
                            },
                            function(error) {
                            }
                        );
                        self.jupyterSettings.shutdownLevel = success.data.shutdownLevel;
                    },
                    function(error) {
                        growl.error("Could not update shutdown time for Jupyter notebook. If this problem persists please contact your system administrator.");
                    }
                );
            };

            var timeToShutdown = function() {
                var timeLeft = self.config.minutesUntilExpiration;
                if(timeLeft < 0) {
                    //Assuming the JupyterNotebookCleaner is configured to run every 1 hr
                    self.timeLeftInMinutes = "less than 1 hour"
                } else {
                    self.timeLeftInMinutes = timeLeft + ' minutes'
                }
            };


            self.changeBaseDir = function() {
                self.jupyterSettings.baseDir = self.selected.name;
            };

            window.onfocus = function() {
                self.livySessions(self.projectId);
            };

            self.livySessions = function(projectId) {
                JupyterService.livySessions(projectId).then(
                    function(success) {
                        $scope.sessions = success.data;
                    },
                    function(error) {
                        $scope.sessions = null;
                    }
                );

            };
            self.showLivyUI = function(appId) {
                self.job.type = "TENSORFLOW";
                self.job.appId = appId;
                StorageService.store(self.projectId + "_jobui_TENSORFLOW", self.job);
                $location.path('project/' + self.projectId + '/jobMonitor-app/' + appId + "/true/jupyter");

            };

            self.checkCondaEnabled = function() {
                PythonService.enabled(self.projectId).then(
                    function(success) {
                        self.pythonVersion = success.data.count > 0? success.data.items[0].pythonVersion : "0.0";
                        self.condaEnabled = true;
                    },
                    function(error) {
                        self.condaEnabled = false;
                    });
            };

            var getCondaCommands = function() {
                PythonService.getEnvironments(self.projectId).then(
                    function (success) {
                        var envs = success.data.items;
                        var count = success.data.count;
                        var opsStatusList = [];
                        for (var i = 0; i < count; i++) {
                            if (typeof envs[i].commands !== 'undefined' && envs[i].commands.count > 0) {
                                opsStatusList.push(envs[i]);
                            }
                        }
                        self.pythonVersion = count > 0? envs[0].pythonVersion : "0.0";
                        self.opsStatus = opsStatusList;
                        self.numNotEnabledEnvs = opsStatusList.length;
                    }, function (error) {

                    });
            };

            getCondaCommands();

            var checkJupyterInstalled = function() {
                // Use hdfscontents as a proxy to now if jupyter has been installed correctly or not
                PythonService.getEnvironments(self.projectId).then(
                    function (success) {
                        self.pythonVersion = success.data.count > 0? success.data.items[0].pythonVersion : "0.0";
                        PythonService.getLibrary(self.projectId, self.pythonVersion, "hdfscontents").then(
                            function(success) {
                                self.jupyterInstalled = true;
                            },
                            function(error) {
                                self.jupyterInstalled = false;
                            }
                        );
                    }, function (error) {

                    });
            };

            checkJupyterInstalled();

            var condaCommandsPoller = function() {
                self.condaPoller = $interval(function() {
                    getCondaCommands();
                }, 5000);
            };
            condaCommandsPoller();

            var jupyterNotebookPoller = function() {
                self.notebookPoller = $interval(function() {
                JupyterService.running(self.projectId).then(
                    function(success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        timeToShutdown();
                        self.livySessions(self.projectId);
                    },
                    function(error) {
                        self.ui = '';
                    }
                );
                }, 20000);
            };
            jupyterNotebookPoller();

            $scope.$on('$destroy', function () {
              $interval.cancel(self.condaPoller);
              $interval.cancel(self.notebookPoller);
              if (typeof self.gitRepositoryPoller !== 'undefined') {
                $interval.cancel(self.gitRepositoryPoller);
              }
            });

            self.shutdown_levels = [
              {
                  id: 1,
                  name: '6'
              }, {
                  id: 2,
                  name: '12'
              }, {
                  id: 3,
                  name: '24'
              }, {
                  id: 4,
                  name: '72'
              }, {
                  id: 5,
                  name: '168'
              }, {
                  id: 6,
                  name: '1000'
            }];


            $window.uploadDone = function() {
                stopLoading();
            };

            $scope.trustSrc = function(src) {
                return $sce.trustAsResourceUrl(self.ui);
            };

            self.restart = function() {
                $location.path('/#!/project/' + self.projectId + '/jupyter');
            };

            self.loadThirdPartyApiKeys = function() {
                    UserService.load_secrets().then(
                        function (success) {
                            self.third_party_api_keys = success.data.items
                        }, function (error) {
                            self.errorMsg = (typeof error.data.usrMsg !== 'undefined') ? error.data.usrMsg : "";
                            growl.error(self.errorMsg, { title: error.data.errorMsg, ttl: 5000, referenceId: 1 });
                        }
                    );
                for (var i = 0; i < self.third_party_api_keys.length; i++) {
                    var api_key = self.third_party_api_keys[i]
                    if (api_key.name === self.jupyterSettings.gitConfig.apiKeyName) {
                        self.git_api_key = api_key
                    }
                }
                var repoConf = {
                    remoteURI: self.jupyterSettings.gitConfig.remoteGitURL,
                    keyName: self.jupyterSettings.gitConfig.apiKeyName
                };
                JupyterService.getGitRemoteBranches(self.projectId, repoConf).then(
                    function (success) {
                        self.jupyterSettings.gitConfig.branches = success.data.branches;
                    }, function (error) {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                    }
                );
            }

            var init = function() {
                JupyterService.running(self.projectId).then(
                    function(success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        timeToShutdown();
                        if (typeof self.gitRepositoryPoller === 'undefined') {
                            gitRepositoryStatusPoller();
                        }
                    },
                    function(error) {
                        self.tourService.currentStep_TourEight = 0;
                        // nothing to do
                    }
                );
                JupyterService.settings(self.projectId).then(
                    function(success) {
                        self.jupyterSettings = success.data;
                        if(self.jupyterSettings.dockerConfig) {
                            self.getDockerMaxAllocation();
                        }
                        $scope.settings = self.jupyterSettings;
                        $scope.jobConfig = self.jupyterSettings.jobConfig;
                        self.projectName = self.jupyterSettings.project.name;
                        for(var i = 0; i < self.dirs.length; i++) {
                          if(self.jupyterSettings.baseDir === self.dirs[i].name) {
                            self.selected = self.dirs[i];
                          }
                        }

                        if (self.jupyterSettings.project.name.startsWith("demo_deep_learning")) {
                            //Activate anaconda
                            PythonService.enabled(self.projectId).then(
                                function(success) {},
                                function(error) {
                                    growl.warning("Anaconda environment is not enabled for the project", {
                                        ttl: 10000
                                    });
                                    PythonService.createEnvironmentFromVersion(self.projectId, "3.6", "true").then(
                                        function(success) {
                                            checkJupyterInstalled();
                                        },
                                        function(error) {
                                            growl.error("Could not enable Anaconda", {title: 'Error', ttl: 5000});
                                        });
                                });

                        }

                        if (self.jupyterSettings.shutdownLevel <= "6") {
                            self.shutdownLevelSelected = self.shutdown_levels[0];
                        } else if (self.jupyterSettings.shutdownLevel <= "12") {
                            self.shutdownLevelSelected = self.shutdown_levels[1];
                        } else if (self.jupyterSettings.shutdownLevel <= "24") {
                            self.shutdownLevelSelected = self.shutdown_levels[2];
                        } else if (self.jupyterSettings.shutdownLevel <= "72") {
                            self.shutdownLevelSelected = self.shutdown_levels[3];
                        } else if (self.jupyterSettings.shutdownLevel <= "168") {
                            self.shutdownLevelSelected = self.shutdown_levels[4];
                        } else {
                            self.shutdownLevelSelected = self.shutdown_levels[0];
                        }

                        timeToShutdown();

                        // Loading of API keys happen when you click on Git backend
                        // but if it is already selected, user won't need to click it again
                        if (self.jupyterSettings.gitAvailable) {
                            if (typeof self.jupyterSettings.gitConfig === 'undefined') {
                                self.jupyterSettings.gitConfig = {}
                                self.jupyterSettings.gitConfig.startupAutoPull = true;
                                self.jupyterSettings.gitConfig.shutdownAutoPush = true;
                            }
                            if (self.jupyterSettings.gitBackend) {
                                UserService.load_secrets().then(
                                    function (success) {
                                        self.third_party_api_keys = success.data.items;
                                        for (var i = 0; i < self.third_party_api_keys.length; i++) {
                                            var api_key = self.third_party_api_keys[i]
                                            if (api_key.name === self.jupyterSettings.gitConfig.apiKeyName) {
                                                self.git_api_key = api_key
                                            }
                                        }
                                        var repoConf = {
                                            remoteURI: self.jupyterSettings.gitConfig.remoteGitURL,
                                            keyName: self.jupyterSettings.gitConfig.apiKeyName
                                        };
                                        JupyterService.getGitRemoteBranches(self.projectId, repoConf).then(
                                            function (success) {
                                                self.jupyterSettings.gitConfig.branches = success.data.branches;
                                            }, function (error) {
                                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                                            });
                                    }, function (error) {
                                        self.errorMsg = (typeof error.data.usrMsg !== 'undefined') ? error.data.usrMsg : "";
                                        growl.error(self.errorMsg, { title: error.data.errorMsg, ttl: 5000, referenceId: 1 });
                                    }
                                );
                            }
                        }
                    },
                    function(error) {
                        growl.error("Could not get Jupyter Notebook Server Settings.");
                    }
                );
                self.livySessions(self.projectId);
            };

            self.openWindow = function() {
                $window.open(self.ui, '_blank');
                timeToShutdown();
            };

            var startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };

            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };

            self.goBack = function() {
                $window.history.back();
            };

            self.stop = function() {
                self.tourService.currentStep_TourEight = 0;
                startLoading("Stopping Jupyter...");

                $scope.tgState = false;

                JupyterService.stop(self.projectId).then(
                    function(success) {
                        self.ui = "";
                        stopLoading();
                        self.mode = "dynamicSpark";
                        $scope.sessions = null;
                        self.jupyterSettings.shutdownLevel = self.shutdown_levels[0].name;
                        self.shutdownLevelSelected = self.shutdown_levels[0];
                        if (typeof self.gitRepositoryPoller !== 'undefined') {
                            $interval.cancel(self.gitRepositoryPoller);
                        }
                    },
                    function(error) {
                        growl.error("Could not stop the Jupyter Notebook Server.");
                        stopLoading();
                    }
                );

            };

            self.stopLivySession = function(session, index) {
                startLoading("Stopping Application...");
                JupyterService.stopLivySession(self.projectId, session.appId).then(
                    function(success) {
                        self.livySessions(self.projectId);
                        stopLoading();
                    },
                    function(error) {
                        growl.error("Could not stop the Application.");
                        stopLoading();
                    }
                );
            };

            var load = function() {
                $scope.tgState = true;
            };

            init();

            var navigateToPython = function() {
                $location.path('/#!/project/' + self.projectId + '/python');
            };

            self.git_error = "";
            self.getRemoteGitBranches = function() {
                if(self.git_api_key && self.git_api_key.name) {
                    self.jupyterSettings.gitConfig.apiKeyName = self.git_api_key.name
                    var repoConf = {
                        remoteURI: self.jupyterSettings.gitConfig.remoteGitURL,
                        keyName: self.jupyterSettings.gitConfig.apiKeyName
                    };
                    self.gitWorking = true;
                    self.git_error = "";
                    JupyterService.getGitRemoteBranches(self.projectId, repoConf).then(
                        function (success) {
                            self.jupyterSettings.gitConfig.branches = success.data.branches;
                            self.jupyterSettings.gitConfig.baseBranch = self.jupyterSettings.gitConfig.branches[0];
                            self.jupyterSettings.gitConfig.headBranch = self.jupyterSettings.gitConfig.branches[0];
                            self.gitWorking = false;
                        }, function (error) {
                            self.gitWorking = false;
                            if (error.data.usrMsg.indexOf("Could not parse remote") !== -1) {
                                // Could not parse remote Git URL
                                self.git_error = "URI_SYNTAX_ERROR";
                            } else if (error.data.usrMsg.indexOf("Invalid API key") !== -1) {
                                self.git_error = "API_KEY_ERROR";
                            }
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                        }
                    );
                }
            }
            
            self.gitStatus = function () {
                if (!self.jupyterSettings.gitAvailable || !self.jupyterSettings.gitBackend) {
                    growl.error("Git backend is either not enabled or unavailable",
                        {title: "Git operation could not complete", ttl: 5000, referenceId: 10});
                    return;
                }
                self.gitWorking = true;
                JupyterService.gitStatus(self.projectId).then(
                    function (success) {
                        self.gitRepoStatus = success.data;
                        self.gitWorking = false;
                    }, function (error) {
                        console.error("Could not get Git status, " + error.data.usrMsg);
                        self.gitWorking = false;
                    }
                );
            }

            var gitRepositoryStatusPoller = function() {
                if (self.jupyterSettings.gitAvailable && self.jupyterSettings.gitBackend) {
                    self.gitRepositoryPoller = $interval(function() {
                        self.gitWorking = true;
                        JupyterService.gitStatus(self.projectId).then(
                            function (success) {
                                self.gitRepoStatus = success.data;
                                self.gitWorking = false;
                            }, function (error) {
                                console.error("Could not reload Jupyter Git status, " + error.data.usrMsg);
                                self.gitWorking = false;
                            }
                        )
                    }, 30000);
                }
            };

            self.start = function(mode) {

                // do not allow starting jupyter enterprise if invalid memory picked
                if (self.jupyterSettings.pythonKernel === true &&
                 self.hasDockerMemory === true &&
                 self.pythonConfigForm &&
                 !self.pythonConfigForm.$valid) {
                    return;
                }

                startLoading("Connecting to Jupyter...");
                $scope.tgState = true;

                self.jupyterSettings.mode = mode;
                
                if (!self.jupyterSettings.gitBackend) {
                    delete self.jupyterSettings.gitConfig
                }
                if (self.jupyterSettings.gitAvailable && self.jupyterSettings.gitBackend) {
                    // If user didn't select a branch, use the configured default - always the first
                    // in the list of remote branches
                    if (typeof self.jupyterSettings.gitConfig.baseBranch === 'undefined') {
                        self.jupyterSettings.gitConfig.baseBranch = self.jupyterSettings.gitConfig.branches[0];
                    }
                    if (typeof self.jupyterSettings.gitConfig.headBranch === 'undefined') {
                        self.jupyterSettings.gitConfig.headBranch = self.jupyterSettings.gitConfig.branches[0];
                    }
                }
                JupyterService.start(self.projectId, self.jupyterSettings).then(
                    function(success) {
                        self.config = success.data;
                        growl.info("Started Notebook server! Will shut down the notebook server and any running applications in  " + self.shutdownLevelSelected.name + " hours.", {
                            title: 'Info',
                            ttl: 20000
                        });
                        timeToShutdown();
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 5000);
                        if (typeof self.gitRepositoryPoller === 'undefined') {
                            gitRepositoryStatusPoller();
                        }
                        if (self.tourService.currentStep_TourEight == 6 || self.tourService.currentStep_TourEight == 7) {
                            self.tourService.currentStep_TourEight = 8;
                        } else {
                            self.tourService.currentStep_TourEight = -1;
                        }
                    },
                    function(error) {
                        if (self.tourService.currentStep_TourEight == 6 || self.tourService.currentStep_TourEight == 7) {
                            self.tourService.currentStep_TourEight = 6;
                        } else {
                            self.tourService.currentStep_TourEight = -1;
                        }
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                        stopLoading();
                    }
                );

            };
        }
    ]);
