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
        'growl', 'ModalService', '$interval', 'JupyterService', 'SparkService', 'StorageService', '$location',
        '$timeout', '$window', '$sce', 'PythonDepsService', 'TourService',
        function($scope, $routeParams, $route, growl, ModalService, $interval, JupyterService,
            SparkService, StorageService, $location, $timeout, $window, $sce, PythonDepsService, TourService) {

            var self = this;
            self.connectedStatus = false;
            self.loading = false;
            self.advanced = true;
            self.details = true;
            self.loadingText = "";
            self.jupyterServer;
            self.projectName;
            self.toggleValue = false;
            self.tourService = TourService;
            self.tourService.currentStep_TourEight = -1;
            self.projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            self.ui = "";
            self.sparkStatic = false;
            self.sparkDynamic = false;
            self.experiment = false;
            self.condaEnabled = true;
            $scope.sessions = null;
            self.val = {};
            $scope.tgState = true;
            self.config = {};
            self.numNotEnabledEnvs = 0;
            self.opsStatus = {};
            self.dirs = [{
                id: 1,
                name: '/'
            }, {
                id: 2,
                name: '/Jupyter/'
            }];
            self.selected = self.dirs[1];

            self.log_levels = [{
                id: 1,
                name: 'FINE'
            }, {
                id: 2,
                name: 'DEBUG'
            }, {
                id: 3,
                name: 'INFO'
            }, {
                id: 4,
                name: 'WARN'
            }, {
                id: 5,
                name: 'ERROR'
            }];
            self.logLevelSelected;

            self.distribution_strategies = [{
                id: 1,
                name: 'MirroredStrategy'
            }, {
                id: 2,
                name: 'CollectiveAllReduceStrategy'
            }, {
                id: 3,
                name: 'ParameterServerStrategy'
            }];

            self.distributionStrategySelected;

            self.shutdown_levels = [{
                id: 1,
                name: '1'
            }, {
                id: 2,
                name: '6'
            }, {
                id: 3,
                name: '12'
            }, {
                id: 4,
                name: '24'
            }, {
                id: 5,
                name: '72'
            }, {
                id: 6,
                name: '168'
            }, {
                id: 7,
                name: '1000'
            }];
            self.shutdownLevelSelected;
            self.timeLeftInMinutes = 0;
            self.addShutdownHours;

            //  (Group/World readable, not writable)
            //  (Group readable and writable)
            //  (Not Group readable or writable)
            self.umasks = [{
                id: 1,
                name: '022'
            }, {
                id: 2,
                name: '007'
            }, {
                id: 3,
                name: '077'
            }];
            self.umask = self.umasks[1];

            self.availableLibs = [{
                'maven': 'Azure:mmlspark:0.13',
                'pip': '' // mmlspark is a .whl file that is already installed in the base conda env
            }, {
                'maven': 'ch.cern.sparkmeasure:spark-measure_2.11:0.13',
                'pip': 'sparkmeasure:0.13.4'
            }];
            self.libs = [];

            self.job = {
                'type': '',
                'name': '',
                'id': '',
                'project': {
                    'name': '',
                    'id': self.projectId
                }
            };

            self.changeLogLevel = function() {
                self.val.logLevel = self.logLevelSelected.name;
            };

            self.changeDistributionStrategy = function() {
                self.val.distributionStrategy = self.distributionStrategySelected.name;
            };

            self.changeShutdownLevel = function() {
                self.val.shutdownLevel = self.shutdownLevelSelected.name;
            };

            self.updateShutdownLevel = function() {
                var currentHours = self.val.shutdownLevel;

                self.val.shutdownLevel = Number(currentHours) + Number(self.shutdownLevelSelected.name);

                self.loadingText = "Updating Jupyter Shutdown Time";
                JupyterService.update(self.projectId, self.val).then(
                    function(success) {
                        self.val.shutdownLevel = success.data.shutdownLevel;
                        growl.info("Updated... notebook will close automatically in " + self.val.shutdownLevel + " hours.", {
                            title: 'Info',
                            ttl: 3000
                        });
                        timeToShutdown();
                    },
                    function(error) {
                        growl.error("Could not update shutdown time for Jupyter notebook. If this problem persists please contact your system administrator.");
                    }
                );
            };

            var timeToShutdown = function() {
                if ('lastAccessed' in self.config) {
                    if ('shutdownLevel' in self.val) {
                        var d = new Date();
                        var currentTimeMs = d.getTime();
                        var lastTimeMs = new Date(self.config.lastAccessed);
                        var timeSinceLastAccess = currentTimeMs - lastTimeMs.valueOf();
                        if (timeSinceLastAccess < 0) {
                            timeSinceLastAccess = 0;
                        }
                        console.log("lastAccessed " + self.config.lastAccessed);
                        console.log("lastAccessed " + lastTimeMs);
                        console.log("timeSinceLast " + timeSinceLastAccess);
                        console.log("currentTimeMs " + currentTimeMs);
                        console.log("shutdownLevel " + self.val.shutdownLevel);
                        var timeLeft = (((self.val.shutdownLevel * 60 * 60 * 1000) - timeSinceLastAccess) / (60 * 1000)).toFixed(1);
                        if(timeLeft < 0) {
                            //Assuming the JupyterNotebookCleaner is configured to run every 1 hr
                            self.timeLeftInMinutes = "less than 1 hour"
                        } else {
                            self.timeLeftInMinutes = timeLeft + ' minutes'
                        }
                    }
                }
            };


            self.changeUmask = function() {
                self.val.umask = self.umask.name;
            };

            self.changeBaseDir = function() {
                self.val.baseDir = self.selected.name;
            };

            self.deselect = function() {};

            window.onfocus = function() {
                self.livySessions(self.projectId);
            };


            $scope.autoExpand = function(e) {
                var element = typeof e === 'object' ? e.target : document.getElementById(e);
                var scrollHeight = element.scrollHeight; // replace 60 by the sum of padding-top and padding-bottom
                element.style.height = scrollHeight + "px";
            };

            function expand() {
                $scope.autoExpand('TextArea');
            }

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

            self.setNormalSpark = function() {
                self.val.executorMemory = 2048;
                self.val.appmasterMemory = 1024;
                self.val.numExecutorGpus = 0;
                self.val.numDriverGpus = 0;
            }

            self.setMultiExecutor = function() {
                // If leaving TF Driver mode, change default executor memory to 4096
                self.val.executorMemory = 4096;
                self.val.appmasterMemory = 1024;
                self.val.numExecutorGpus = 0;
                self.val.numDriverGpus = 0;
                self.val.numExecutors = 2;
            };

            self.setSingleExecutor = function() {
                // For TF in Driver mode, we set the minimal number of executors and
                // minimal amount of memory and fix a single core for the Driver.
                self.val.appmasterMemory = 1024;
                self.val.appmasterCores = 1;
                self.val.numExecutorCores = 1;
                self.val.numExecutorGpus = 0;
                self.val.numDriverGpus = 0;
                self.val.numExecutors = 1;
                // 128MB is hard-coded in hops-hadoop/templates/../yarn-site.xml
                self.val.executorMemory = 4096;
            };
            self.showLivyUI = function(appId) {
                self.job.type = "TENSORFLOW";
                self.job.appId = appId;
                StorageService.store(self.projectId + "_jobui_TENSORFLOW", self.job);
                $location.path('project/' + self.projectId + '/jobMonitor-app/' + appId + "/true/jupyter");

            };

            self.checkCondaEnabled = function() {
                PythonDepsService.enabled(self.projectId).then(
                    function(success) {
                        self.condaEnabled = true;
                    },
                    function(error) {
                        self.condaEnabled = false;
                    });
            };

            var getCondaCommands = function() {
                PythonDepsService.status(self.projectId).then(
                    function(success) {
                        self.opsStatus = success.data;
                        self.tempEnvs = 0;
                        var i = 0;
                        for (i = 0; i < self.opsStatus.length; i++) {
                            if ((self.opsStatus[i].op === "CREATE" || self.opsStatus[i].op === "YML")
                            && (self.opsStatus[i].status === "NEW" || self.opsStatus[i].status === "ONGOING")) {
                                self.tempEnvs += 1;
                                break;
                            }
                        }
                        self.checkCondaEnabled()
                        self.numNotEnabledEnvs = self.tempEnvs;

                    },
                    function(error) {

                    }
                );
            };

            getCondaCommands();

            var startPolling = function() {
                self.poller = $interval(function() {
                    getCondaCommands();
                }, 5000);
            };
            startPolling();

            self.sliderVisible = false;

            self.setInitExecs = function() {
                if (self.sliderOptions.min >
                    self.val.dynamicInitialExecutors) {
                    self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.min);
                } else if (self.sliderOptions.max <
                    self.val.dynamicInitialExecutors) {
                    self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.max);
                }
                self.val.dynamicMinExecutors = self.sliderOptions.min;
                self.val.dynamicMaxExecutors = self.sliderOptions.max;
            };

            self.dynExecChangeListener = function() {
                self.setInitExecs();
            };

            self.sliderOptions = {
                min: 1,
                max: 10,
                options: {
                    floor: 0,
                    ceil: 1000,
                    onChange: self.dynExecChangeListener,
                    getPointerColor: function(value) {
                        return '#4b91ea';
                    }
                }
            };

            self.refreshSlider = function() {
                $timeout(function() {
                    $scope.$broadcast('rzSliderForceRender');
                });
            };

            self.toggleSlider = function() {
                self.sliderVisible = !self.sliderVisible;
                if (self.sliderVisible)
                    self.refreshSlider();
            };

            //Set some (semi-)constants
            self.selectFileRegexes = {
                "JAR": /.jar\b/,
                "PY": /.py\b/,
                "FILES": /[^]*/,
                "ZIP": /.zip\b/,
                "TGZ": /.zip\b/
            };
            self.selectFileErrorMsgs = {
                "JAR": "Please select a JAR file.",
                "PY": "Please select a Python file.",
                "ZIP": "Please select a zip file.",
                "TGZ": "Please select a tgz file.",
                "FILES": "Please select a file."
            };


            this.selectFile = function(reason) {

                ModalService.selectFile('lg', self.selectFileRegexes[reason.toUpperCase()],
                    self.selectFileErrorMsgs[reason.toUpperCase()]).then(
                    function(success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                    },
                    function(error) {
                        //The user changed their mind.
                    });
            };

            /**
             * Callback for when the user selected a file.
             * @param {String} reason
             * @param {String} path
             * @returns {undefined}
             */
            self.onFileSelected = function(reason, path) {
                var re = /(?:\.([^.]+))?$/;
                var extension = re.exec(path)[1];
                var file = path.replace(/^.*[\\\/]/, '')
                var fileName = file.substr(0, file.lastIndexOf('.'))
                switch (reason.toUpperCase()) {
                    case "PYFILES":
                        if (extension.toUpperCase() === "PY" ||
                            extension.toUpperCase() === "ZIP" ||
                            extension.toUpperCase() === "EGG") {
                            if (self.val.pyFiles === "") {
                                self.val.pyFiles = path;
                            } else {
                                self.val.pyFiles = self.val.pyFiles.concat(",").concat(path);
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .py, .zip or .egg - Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "JARS":
                        if (extension.toUpperCase() === "JAR") {
                            if (self.val.jars === "") {
                                self.val.jars = path;
                            } else {
                                self.val.jars = self.val.jars.concat(",").concat(path);
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .jar - Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "ARCHIVES":
                        if (extension.toUpperCase() === "ZIP" || extension.toUpperCase() === "TGZ") {
                            path = path + "#" + fileName
                            if (self.val.archives === "") {
                                self.val.archives = path;
                            } else {
                                self.val.archives = self.val.archives.concat(",").concat(path);
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .zip Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "FILES":
                        path = path + "#" + file
                        if (self.val.files === "") {
                            self.val.files = path;
                        } else {
                            self.val.files = self.val.files.concat(",").concat(path);
                        }
                        break;
                    default:
                        growl.error("Invalid file type selected: " + reason, {
                            ttl: 10000
                        });
                        break;
                }
            };

            $window.uploadDone = function() {
                stopLoading();
            };

            $scope.trustSrc = function(src) {
                return $sce.trustAsResourceUrl(self.ui);
            };

            self.experiment = function() {
                $scope.mode = "experiment";
            };

            self.spark = function() {
                $scope.mode = "spark";
            };

            self.restart = function() {
                $location.path('/#!/project/' + self.projectId + '/jupyter');
            };

            var init = function() {
                JupyterService.running(self.projectId).then(
                    function(success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        self.toggleValue = true;
                        timeToShutdown();
                    },
                    function(error) {
                        self.val.shutdownLevel = 6;
                        self.tourService.currentStep_TourEight = 0;
                        // nothing to do
                    }
                );
                JupyterService.settings(self.projectId).then(
                    function(success) {
                        self.val = success.data;
                        self.projectName = self.val.project.name;
                        if (self.val.dynamicMinExecutors < 1) {
                            self.val.dynamicMinExecutors = 1;
                        }
                        self.sliderOptions.min = self.val.dynamicMinExecutors;
                        self.sliderOptions.max = self.val.dynamicMaxExecutors;
                        self.toggleValue = true;
                        if (self.val.project.name.startsWith("demo_deep_learning")) {
                            self.val.mode = "experiment";
                            self.experiment();
                            //Activate anaconda
                            PythonDepsService.enabled(self.projectId).then(
                                function(success) {},
                                function(error) {
                                    growl.info("Preparing Python Anaconda environment, please wait...", {
                                        ttl: 20000
                                    });
                                    PythonDepsService.enable(self.projectId, "2.7", "true").then(
                                        function(success) {

                                        },
                                        function(error) {
                                            growl.error("Could not enable Anaconda", {
                                                title: 'Error',
                                                ttl: 5000
                                            });
                                        });
                                });

                        } else {
                            self.val.mode = "sparkdynamic";
                        }
                        if (self.val.logLevel === "FINE") {
                            self.logLevelSelected = self.log_levels[0];
                        } else if (self.val.logLevel === "DEBUG") {
                            self.logLevelSelected = self.log_levels[1];
                        } else if (self.val.logLevel === "INFO") {
                            self.logLevelSelected = self.log_levels[2];
                        } else if (self.val.logLevel === "WARN") {
                            self.logLevelSelected = self.log_levels[3];
                        } else if (self.val.logLevel === "ERROR") {
                            self.logLevelSelected = self.log_levels[4];
                        } else {
                            self.logLevelSelected = self.log_levels[2];
                        }

                        self.distributionStrategySelected = self.distribution_strategies[0];
                        self.val.distributionStrategy = self.distributionStrategySelected.name;

                        if (self.val.shutdownLevel <= "1") {
                            self.shutdownLevelSelected = self.shutdown_levels[0];
                        } else if (self.val.shutdownLevel <= "6") {
                            self.shutdownLevelSelected = self.shutdown_levels[1];
                        } else if (self.val.shutdownLevel <= "12") {
                            self.shutdownLevelSelected = self.shutdown_levels[2];
                        } else if (self.val.shutdownLevel <= "24") {
                            self.shutdownLevelSelected = self.shutdown_levels[3];
                        } else if (self.val.shutdownLevel <= "72") {
                            self.shutdownLevelSelected = self.shutdown_levels[4];
                        } else if (self.val.shutdownLevel <= "168") {
                            self.shutdownLevelSelected = self.shutdown_levels[5];
                        } else {
                            self.shutdownLevelSelected = self.shutdown_levels[6];
                        }

                        if (self.val.umask === "022") {
                            self.umask = self.umasks[0];
                        } else if (self.val.umask === "007") {
                            self.umask = self.umasks[1];
                        } else if (self.val.umask === "077") {
                            self.umask = self.umasks[2];
                        } else {
                            self.umask = self.umasks[0];
                        }


                        if (self.val.libs === undefined || self.val.libs.length === 0) {
                            self.libs = [];
                        } else {
                            self.libs = self.val.libs;
                        }


                        timeToShutdown();

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
                if (self.tourService.currentStep_TourEight == 8) {
                    self.tourService.currentStep_TourEight = 9;
                }
            }



            var startLoading = function(label) {
                self.advanced = false;
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

                JupyterService.stop(self.projectId).then(
                    function(success) {
                        self.ui = "";
                        stopLoading();
                        self.mode = "dynamicSpark";
                        $scope.sessions = null;
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

            self.stopDataOwner = function(hdfsUsername) {
                startLoading("Stopping Jupyter...");
                JupyterService.stopDataOwner(self.projectId, hdfsUsername).then(
                    function(success) {
                        self.ui = ""
                        stopLoading();
                    },
                    function(error) {
                        growl.error("Could not stop the Jupyter Notebook Server.");
                        stopLoading();
                    }
                );
            };
            self.stopAdmin = function(hdfsUsername) {
                startLoading("Stopping Jupyter...");
                self.advanced = true;
                JupyterService.stopAdmin(self.projectId, hdfsUsername).then(
                    function(success) {
                        self.ui = ""
                        stopLoading();
                    },
                    function(error) {
                        growl.error("Could not stop the Jupyter Notebook Server.");
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

            self.start = function() {
                startLoading("Connecting to Jupyter...");
                $scope.tgState = true;
                self.setInitExecs();

                // if quick-select libraries have been added, we need to add them as
                // maven packages.
                var azureRepo = false;
                if (self.libs.length > 0) {
                    var packages = "";
                    var foundRepos = self.val.sparkParams.includes("spark.jars.repositories");
                    for (var i = 0; i < self.libs.length; i++) {
                        packages = packages + self.libs[i].maven;
                        if (i < self.libs.length - 1) {
                            packages = packages + ",";
                        }

                        if (self.libs[i].maven.includes("mmlspark")) {
                            if (self.val.sparkParams.includes("mmlspark") === false) {
                                azureRepo = true;
                            }
                        }
                    }
                    var existsPackages = true;
                    if (self.val.sparkParams.includes("spark.jars.packages") === false) {
                        existsPackages = false;
                        if (self.val.sparkParams) {
                            self.val.sparkParams = self.val.sparkParams.concat('\n' + "spark.jars.packages=");
                        } else {
                            self.val.sparkParams = "spark.jars.packages=";
                        }
                    }
                    if (azureRepo) {
                        var repo = "spark.jars.repositories=" + "http://dl.bintray.com/spark-packages/maven";
                        if (foundRepos) {
                            self.val.sparkParams = self.val.sparkParams.replace("spark.jars.repositories=", repo + ",");
                        } else {
                            self.val.sparkParams = self.val.sparkParams.concat('\n' + repo);
                        }
                    }

                    // First add all the maven coordinates to spark.jars.packages
                    for (var i = 0; i < self.libs.length; i++) {
                        var entry = "spark.jars.packages=" + self.libs[i].maven;
                        if (existsPackages) {
                            self.val.sparkParams = self.val.sparkParams.replace("spark.jars.packages=", entry + ",");
                        } else {
                            existsPackages = true;
                            self.val.sparkParams = self.val.sparkParams.replace("spark.jars.packages=", entry);
                        }
                    }


                    // If PySpark selected, add the pip libraries for preselected libraries
                    PythonDepsService.index(self.projectId).then(
                        function(success) {
                            if (success.status !== 204) { // if some libraries were found, continue
                                var installedPip = success.data;

                                for (var i = 0; i < self.libs.length; i++) {
                                    // Some selected packages dont have pip libraries.
                                    if (self.libs[i].pip !== undefined && self.libs[i].pip !== "") {
                                        var pipLibAlreadyInstalled = false;
                                        var splitPip = self.libs[i].pip.split(":");
                                        var pipLibName = splitPip[0];
                                        var pipLibVersion = splitPip[1];
                                        for (var j = 0; j < installedPip.length; j++) {
                                            if (installedPip[j].lib === pipLibName) {
                                                pipLibAlreadyInstalled = true;
                                                break;
                                            }
                                        }
                                        if (pipLibAlreadyInstalled === false) {
                                            var data = {
                                                "channelUrl": "PyPi",
                                                "installType": 'PIP',
                                                "machineType": "ALL",
                                                "lib": pipLibName,
                                                "version": pipLibVersion
                                            };
                                            PythonDepsService.install(self.projectId, data).then(
                                                function(success) {
                                                    growl.info("Installing library: " + self.libs[i].pip, {
                                                        title: "PIP",
                                                        ttl: 5000
                                                    });
                                                },
                                                function(error) {
                                                    growl.error(error.data.errorMsg, {
                                                        title: 'Error installing pip library: ' + self.libs[i].pip,
                                                        ttl: 5000
                                                    });
                                                });
                                        }
                                    }
                                }
                            }
                        },
                        function(error) {
                            // Don't need to print to the user
                        });
                }

                JupyterService.start(self.projectId, self.val).then(
                    function(success) {
                        self.toggleValue = true;
                        self.config = success.data;
                        growl.info("Started Notebook server! Will shut down the notebook server and any running applications in  " + self.shutdownLevelSelected.name + " hours.", {
                            title: 'Info',
                            ttl: 20000
                        });
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 5000);
                        timeToShutdown();
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
                        self.toggleValue = true;
                    }
                );

            };
        }
    ]);
