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
        '$timeout', '$window', '$sce', 'PythonService', 'TourService', 'UserService', 'VariablesService', 'DataSetService', 'ElasticService', 'ProjectService',
        function($scope, $routeParams, $route, growl, ModalService, $interval, JupyterService,
            StorageService, $location, $timeout, $window, $sce, PythonService, TourService, UserService, VariablesService, DataSetService, ElasticService, ProjectService) {

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
            $scope.sessions = null;
            self.jupyterSettings = {};
            $scope.tgState = true;
            self.config = {};
            self.ongoingCondaOps = 0;
            self.opsStatus = {};
            self.pythonVersion;

            self.projectDefaultSparkConfig;
            self.showProjectDefaultSparkConfigButton = false;
            self.projectDefaultPythonConfig;
            self.showProjectDefaultPythonConfigButton = false;

            var dataSetService = DataSetService(self.projectId);

            self.hasDockerMemory = false;
            self.maxDockerMemory = 1024;
            self.hasDockerCores = false;
            self.maxDockerCores = 1;

            self.shutdownLevelSelected;
            self.timeLeftInMinutes = 0;
            self.addShutdownHours;
            self.notebookAttachedConfigView = false;

            self.jupyterConflicts = false;

            self.showConflicts = function () {
                ModalService.conflicts('lg', 'Environment conflicts', self.projectId, self.pythonVersion, '?filter_by=service:JUPYTER')
                            .then(function (success) {}, function (error) {});
            };

            self.init = function () {
                ProjectService.get({}, {'id': self.projectId}).$promise.then(
                    function (success) {
                        self.projectName = success.projectName;
                        self.loadSettings();
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the name of the project',
                            ttl: 15000
                        });
                });
            };

            self.job = {
                'type': '',
                'name': '',
                'id': '',
                'project': {
                    'name': '',
                    'id': self.projectId
                }
            };

            //Validation of spark executor memory
            //just set some default
            self.sparkExecutorMemory = {minExecutorMemory:1024, hasEnoughMemory:true};

            $scope.$watch('attachedJupyterConfigInfo', function (jupyterSettings, oldConfig) {
                if (jupyterSettings) {
                    self.notebookAttachedConfigView = true
                    self.initJupyterSettings(jupyterSettings)
                }
            }, true);

            self.getJobConfiguration = function (jobType) {
                ProjectService.getDefaultJobConfig({id: self.projectId, type: jobType.toLowerCase()})
                  .$promise.then(function (success) {
                      if(jobType === "SPARK") {
                        self.projectDefaultSparkConfig = success.config;
                        for(var key in self.projectDefaultSparkConfig) {
                          if(!(key in $scope.jobConfig) || self.projectDefaultSparkConfig[key] != $scope.jobConfig[key]) {
                            self.showProjectDefaultSparkConfigButton = true;
                            return;
                          }
                        }
                      } else if(jobType === "PYTHON") {
                        self.projectDefaultPythonConfig = success.config;
                        for(var key in self.projectDefaultPythonConfig) {
                          if(!(key in self.jupyterSettings.dockerConfig) || self.projectDefaultPythonConfig[key] != self.jupyterSettings.dockerConfig[key]) {
                            self.showProjectDefaultPythonConfigButton = true;
                            return;
                          }
                        }
                      }
                    }, function (error) {});
            };

            self.useProjectDefaultSparkConfig = function () {
              self.jupyterSettings.jobConfig = self.projectDefaultSparkConfig;
              $scope.jobConfig = self.jupyterSettings.jobConfig;
              self.jupyterSettings.pythonKernel = false;
              self.showProjectDefaultSparkConfigButton = false;
            }

            self.useProjectDefaultPythonConfig = function () {
              self.jupyterSettings.dockerConfig = self.projectDefaultPythonConfig;
              self.jupyterSettings.pythonKernel = true;
              self.showProjectDefaultPythonConfigButton = false;
            }

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
                VariablesService.getVariable('kube_docker_max_gpus_allocation')
                  .then(function (success) {
                    self.hasDockerGpus = true;
                    self.maxDockerGpus = parseInt(success.data.successMessage);
                  }, function (error) {
                    self.hasDockerGpus = false;
                    self.maxDockerGpus = -1;
                });
            };

            self.range = function (min, max) {
                var input = [];
                for (var i = min; i <= max; i++) {
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
                if(self.selected.name.includes('::')) {
                    var datasetSplit = self.selected.name.split('::');
                    self.jupyterSettings.baseDir = '/Projects/' + datasetSplit[0] + '/' + datasetSplit[1];
                } else {
                    self.jupyterSettings.baseDir = '/Projects/' + self.projectName + self.selected.name;
                }
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

            var getCondaCommands = function() {
                PythonService.getEnvironments(self.projectId, '').then(
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
                        self.ongoingCondaOps = opsStatusList.length;
                    }, function (error) {

                    });
            };

            getCondaCommands();

            var checkJupyterInstalled = function() {

            PythonService.enabled(self.projectId).then(
                function(success) {
                    self.pythonVersion = success.data.count > 0? success.data.items[0].pythonVersion : "0.0";
                    self.condaEnabled = true;
                },
                function(error) {
                    self.condaEnabled = false;
                });

                PythonService.getEnvironments(self.projectId, '?expand=commands').then(
                    function (success) {
                        self.pythonVersion = success.data.count > 0? success.data.items[0].pythonVersion: "0.0";
                        PythonService.getEnvironmentConflicts(self.projectId, self.pythonVersion, "?filter_by=service:JUPYTER").then(
                            function (success) {
                                if(success.data.items) {
                                  self.jupyterConflicts = true;
                                }
                            },
                            function (error) {

                        });
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

            self.initJupyterSettings = function (jupyterSettings) {
                self.jupyterSettings = jupyterSettings;
                if(self.jupyterSettings.dockerConfig) {
                    self.getJobConfiguration("PYTHON");
                    self.getDockerMaxAllocation();
                }
                self.getJobConfiguration("SPARK");
                $scope.settings = self.jupyterSettings;
                $scope.jobConfig = self.jupyterSettings.jobConfig;

                dataSetService.getAllDatasets(undefined, 0, 1000, ['name:asc'], ['shared:true', 'accepted:true'], "DATASET")
                    .then(function (success) {
                        self.dirs = [{
                            id: 1,
                            name: '/',
                            warning: 'You can only create notebooks inside a dataset - not in the / (root) directory.'
                        }, {
                            id: 2,
                            name: '/Jupyter'
                        }];

                        if(success.data.items) {
                            for(var i = 0; i < success.data.items.length; i++) {
                                var sharedDataset = {id: 3 + i, name: success.data.items[i].name};
                                self.dirs.push({id: 3 + i, name: success.data.items[i].name});
                            }
                        }
                        if(self.jupyterSettings.baseDir === '/Projects/' + self.projectName + '/') {
                            //If started Jupyter from Project root folder
                            self.selected = self.dirs[0];
                        } else if(self.jupyterSettings.baseDir === '/Projects/' + self.projectName + '/Jupyter') {
                            //If started Jupyter from Project Jupyter folder
                            self.selected = self.dirs[1];
                        } else {
                            //If started Jupyter in a dataset shared with Project
                            var foundSharedDataset = false;
                            for(var y = 2; y < self.dirs.length; y++) {
                                var datasetSplit = self.dirs[y].name.split('::');
                                if(self.jupyterSettings.baseDir === '/Projects/' + datasetSplit[0] + '/' + datasetSplit[1]) {
                                    self.selected = self.dirs[y];
                                    foundSharedDataset = true;
                                    break;
                                }
                            }
                            //If previously selected shared dataset is no longer shared with the project default to Jupyter dataset
                            if(!foundSharedDataset) {
                                self.selected = self.dirs[1];
                                self.changeBaseDir();
                            }
                        }
                    }, function (error) {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                    });


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
            };

            self.loadSettings = function() {
                JupyterService.running(self.projectId).then(
                    function(success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        timeToShutdown();
                    },
                    function(error) {
                        self.tourService.currentStep_TourEight = 0;
                        // nothing to do
                    }
                );
                //When viewing the notebook attached configuration use the one attached
                if(typeof $scope.notebookAttachedJupyterConfigInfoView === 'undefined') {
                    JupyterService.settings(self.projectId).then(
                        function(success) {
                            self.initJupyterSettings(success.data)
                        },
                        function(error) {
                            growl.error("Could not get Jupyter Notebook Server Settings.", {title: error.data.errorMsg, ttl: 10000});
                        }
                    );
                }
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

            self.init();

            self.navigateToPython = function() {
                $location.path('project/' + self.projectId + '/python');
            };

            $scope.startFromXattrConfig = function (config, mode) {
                self.jupyterSettings = config;
                self.start(mode)
            }

            self.start = function(mode) {
                // do not allow starting jupyter enterprise if invalid memory picked
                if (self.jupyterSettings.pythonKernel === true &&
                 self.hasDockerMemory === true &&
                 self.pythonConfigForm &&
                 !self.pythonConfigForm.$valid) {
                    return;
                }

                // do not allow starting jupyter server if executor memory is less than the minimum required
                if(!self.sparkExecutorMemory.hasEnoughMemory) {
                    growl.warning("Executor memory should not be less than " + self.sparkExecutorMemory.minExecutorMemory + " MB");
                    return;
                }

                startLoading("Connecting to Jupyter...");
                $scope.tgState = true;

                self.jupyterSettings.mode = mode;

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

            self.showLogs = function(){
                ElasticService.getJwtToken(self.projectId).then(
                    function (success) {
                        var projectName = success.data.projectName;
                        var kibanaUrl = success.data.kibanaUrl;
                        UserService.profile().then(
                            function (success) {
                                var username = success.data.username;
                                self.logsUI = kibanaUrl + "projectId=" + self.projectId +
                                    "#/discover?_g=(filters:!())&_a=(columns:!('@timestamp',log_message),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'" + projectName.toLowerCase()+"_logs-*',key:application,negate:!f,params:(query:" + username + "),type:phrase,value:" + username + "),query:(match:(application:(query:" + username + ",type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'" + projectName.toLowerCase()+"_logs-*',key:jobname,negate:!f,params:(query:nbserver),type:phrase,value:nbserver),query:(match:(jobname:(query:nbserver,type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'" + projectName.toLowerCase()+"_logs-*',key:project,negate:!f,params:(query:" + projectName + "),type:phrase,value:" + projectName + "),query:(match:(project:(query:" + projectName + ",type:phrase))))),index:'" + projectName.toLowerCase()+"_logs-*',interval:auto,query:(language:lucene,query:''),sort:!('@timestamp',desc))";
                                $window.open(self.logsUI, '_blank');
                            }, function (error) {
                                if (typeof error.data.usrMsg !== 'undefined') {
                                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                                } else {
                                    growl.error("", {title: error.data.errorMsg, ttl: 8000});
                                }
                            });

                    }, function (error) {

                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                    });


            };

            $scope.executorMemoryState = function (exMemory) {
                self.sparkExecutorMemory = exMemory;
            };

            self.closeNotebookAttachedConfigView = function () {
                $scope.closeNotebookAttachedConfigView()
            }
        }
    ]);
