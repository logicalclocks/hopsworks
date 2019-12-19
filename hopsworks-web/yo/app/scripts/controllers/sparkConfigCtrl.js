/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

'use strict';
/**
 * Controller for configuring Spark jobs and notebooks.
 */
angular.module('hopsWorksApp')
    .controller('SparkConfigCtrl', ['$scope', '$routeParams', '$route',
        'growl', 'ModalService', '$interval', 'JupyterService', 'StorageService', '$location', '$timeout',
        function($scope, $routeParams, $route, growl, ModalService, $interval, JupyterService,
            StorageService, $location, $timeout) {

            var self = this;

            self.pyFiles = [];
            self.jars = [];
            self.archives = [];
            self.files = [];
            self.projectId = $routeParams.projectID;

            self.loc = $location.url().split('#')[0];

            self.isJupyter = self.loc.endsWith('jupyter');
            self.isJobs = self.loc.endsWith('newJob');
            self.uneditableMode = false;

            self.experimentType = '';
            $scope.indextab = 0;

            $scope.$watch('jobConfig', function (jobConfig, oldConfig) {
                if (jobConfig) {
                    self.jobConfig = jobConfig;
                    self.setConf();
                }
            }, true);

            $scope.$watch('sparkConfigCtrl.sparkType', function(sparkType, oldSparkType) {
                if (sparkType && typeof self.jobConfig !== "undefined") {
                    self.setMode(sparkType);
                }
            });

            $scope.$watch('sparkConfigCtrl.selectedType', function(newSelectedType, oldSelectedType) {
                if (newSelectedType && typeof self.jobConfig !== "undefined") {
                    self.setMode(newSelectedType);
                }
            });

            $scope.$watch('settings', function(settings, oldSettings) {
                if(settings) {
                      self.settings = settings;
                      self.setConf();
                }
            });

            self.setConf = function() {
                if(self.isJupyter && self.settings && self.settings.pythonKernel === true) {
                    $scope.indextab = 0;
                } else if (self.jobConfig.experimentType) {
                    self.jobConfig['spark.dynamicAllocation.enabled'] = true;
                    self.setMode(self.jobConfig.experimentType);
                    self.selectedType = self.jobConfig.experimentType;
                    $scope.indextab = 1;
                } else if (self.jobConfig['spark.dynamicAllocation.enabled'] && self.jobConfig['spark.dynamicAllocation.enabled'] === true) {
                    self.setMode('SPARK_DYNAMIC');
                    self.sparkType = 'SPARK_DYNAMIC';
                    self.selectedType = 'SPARK_DYNAMIC';
                    $scope.indextab = 2;
                } else {
                    self.setMode('SPARK_STATIC');
                    self.sparkType = 'SPARK_STATIC';
                    self.selectedType = 'SPARK_STATIC';
                    self.jobConfig['spark.dynamicAllocation.enabled'] = false;
                    $scope.indextab = 2;
                }

                if (self.jobConfig.distributionStrategy) {
                    self.distributionStrategy = self.jobConfig.distributionStrategy;
                    for(var i = 0; i < self.distribution_strategies.length; i++) {
                      if(self.distributionStrategy === self.distribution_strategies[i].name) {
                        self.distributionStrategySelected = self.distribution_strategies[i];
                      }
                    }
                } else {
                    self.distributionStrategySelected = self.distribution_strategies[1];
                    self.distributionStrategy = self.distributionStrategySelected.name;
                    self.jobConfig.distributionStrategy = self.distributionStrategy;
                }

                self.pyFiles = [];
                self.jars = [];
                self.archives = [];
                self.files = [];

                if (self.jobConfig['spark.yarn.dist.files']) {
                    var files = self.jobConfig['spark.yarn.dist.files'].split(',');
                    for (var i = 0; i < files.length; i++) {
                        if (files[i]) {
                          if(self.files.indexOf(files[i] === -1))
                            self.files.push(files[i]);
                        }
                    }
                }

                if (self.jobConfig['spark.yarn.dist.pyFiles']) {
                    var pyFiles = self.jobConfig['spark.yarn.dist.pyFiles'].split(',');
                    for (var i = 0; i < pyFiles.length; i++) {
                        if (pyFiles[i]) {
                            if(self.pyFiles.indexOf(pyFiles[i] === -1))
                            self.pyFiles.push(pyFiles[i]);
                        }
                    }
                }

                if (self.jobConfig['spark.yarn.dist.jars']) {
                    var jars = self.jobConfig['spark.yarn.dist.jars'].split(',');
                    for (var i = 0; i < jars.length; i++) {
                        if (jars[i]) {
                            if(self.jars.indexOf(jars[i] === -1))
                            self.jars.push(jars[i]);
                        }
                    }
                }

                if (self.jobConfig['spark.yarn.dist.archives']) {
                    var archives = self.jobConfig['spark.yarn.dist.archives'].split(',');
                    for (var i = 0; i < archives.length; i++) {
                        if (archives[i]) {
                          if(self.archives.indexOf(archives[i] === -1))
                            self.archives.push(archives[i]);
                        }
                    }
                }
            };

            var saveExperimentType = function () {
                if (typeof self.jobConfig.experimentType !== "undefined") {
                    self.experimentType = self.jobConfig.experimentType;
                } else {
                    self.experimentType = 'EXPERIMENT';
                }
            };

            self.changeTab = function (tab) {
                switch (tab) {
                    case 'PYTHON':
                        saveExperimentType();
                        break;
                    case 'EXPERIMENT':
                        if (typeof self.jobConfig.experimentType !== "undefined") {
                            saveExperimentType();
                        } else {
                            if (self.experimentType) {
                                self.setMode(self.experimentType);
                            } else {
                                self.setMode('EXPERIMENT');
                            }
                        }
                        break;
                    case 'SPARK':
                        saveExperimentType();
                        if (self.sparkType === '' && self.jobConfig['spark.dynamicAllocation.enabled']) {
                            self.setMode('SPARK_DYNAMIC');
                        } else if (self.sparkType === '') {
                            self.setMode('SPARK_STATIC');
                        } else {
                            self.setMode(self.sparkType);
                        }
                        if (typeof self.jobConfig.experimentType !== "undefined") {
                            delete self.jobConfig.experimentType;
                        }
                        break;
                }
            };

            self.setMode = function(mode) {
                if (mode === 'PARALLEL_EXPERIMENTS' || mode === 'DISTRIBUTED_TRAINING') {
                    self.jobConfig['spark.dynamicAllocation.initialExecutors'] = 0;
                    self.jobConfig['spark.dynamicAllocation.minExecutors'] = 0;
                }

                if (mode === 'EXPERIMENT') {
                    self.jobConfig['spark.dynamicAllocation.initialExecutors'] = 0;
                    self.jobConfig['spark.dynamicAllocation.minExecutors'] = 0;
                    self.jobConfig['spark.dynamicAllocation.maxExecutors'] = 1;
                }

                if (mode) {
                    if (mode === 'SPARK_STATIC' || mode === 'SPARK_DYNAMIC') {
                        self.jobConfig['spark.blacklist.enabled'] = false;
                        if (self.jobConfig.experimentType) {
                            delete self.jobConfig.experimentType;
                        }
                        self.sparkType = mode;
                    } else {
                        self.jobConfig.experimentType = mode;
                        //self.sparkType = '';
                    }
                }

                if (mode === 'SPARK_STATIC') {
                    self.jobConfig['spark.dynamicAllocation.enabled'] = false;
                } else {
                    self.jobConfig['spark.dynamicAllocation.enabled'] = true;
                }

                self.sliderOptions.min = self.jobConfig['spark.dynamicAllocation.minExecutors'];
                self.sliderOptions.max = self.jobConfig['spark.dynamicAllocation.maxExecutors'];
                if(self.jobConfig['spark.dynamicAllocation.maxExecutors'] < 100) {
                  self.sliderOptions.options.ceil = 100;
                } else {
                  self.sliderOptions.options.ceil = self.jobConfig['spark.dynamicAllocation.maxExecutors'];
                }
                self.sliderOptions.options.floor = 0;

            };

            self.setInitExecs = function() {
                if (self.sliderOptions.min >
                    self.jobConfig['spark.dynamicAllocation.initialExecutors']) {
                    self.jobConfig['spark.dynamicAllocation.initialExecutors'] =
                        parseInt(self.sliderOptions.min);
                } else if (self.sliderOptions.max <
                    self.jobConfig['spark.dynamicAllocation.initialExecutors']) {
                    self.jobConfig['spark.dynamicAllocation.initialExecutors'] =
                        parseInt(self.sliderOptions.max);
                }
                self.jobConfig['spark.dynamicAllocation.minExecutors'] = self.sliderOptions.min;
                self.jobConfig['spark.dynamicAllocation.maxExecutors'] = self.sliderOptions.max;
            };

            self.dynExecChangeListener = function() {
                self.setInitExecs();

                if (self.sliderOptions.options.ceil <= self.jobConfig['spark.dynamicAllocation.maxExecutors'] && self.sliderOptions.options.ceil < 3000) {
                    $timeout(function() {
                        self.sliderOptions.options.ceil = Math.floor(self.sliderOptions.options.ceil * 1.3);
                    }, 200);
                }

                var lowerTreshhold = self.sliderOptions.options.ceil * 0.05;

                if (lowerTreshhold > self.jobConfig['spark.dynamicAllocation.maxExecutors']) {
                    $timeout(function() {
                        self.sliderOptions.options.ceil = Math.floor(self.sliderOptions.options.ceil * 0.9);
                    }, 200);
                }

                if (self.sliderOptions.options.ceil > 3000) {
                    self.jobConfig['spark.dynamicAllocation.maxExecutors'] = 3000;
                    self.sliderOptions.options.ceil = 3000;
                }
            };

            self.sliderOptions = {
                min: 0,
                max: 2,
                options: {
                    floor: 0,
                    ceil: 100,
                    onChange: self.dynExecChangeListener,
                    getPointerColor: function(value) {
                        return '#6c757d';
                    },
                    getTickColor: function(value) {
                        return '#6c757d';
                    },
                    getSelectionBarColor: function(value) {
                        return '#6c757d';
                    }
                }
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

                ModalService.selectFile('lg', self.projectId, self.selectFileRegexes[reason.toUpperCase()],
                    self.selectFileErrorMsgs[reason.toUpperCase()], false).then(
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
                            if (self.pyFiles === []) {
                                self.pyFiles = [path];
                            } else {
                                if (self.pyFiles.indexOf(path) === -1) {
                                    self.pyFiles.push(path);
                                }
                            }
                            self.jobConfig['spark.yarn.dist.pyFiles'] = "";
                            for (var i = 0; i < self.pyFiles.length; i++) {
                                self.jobConfig['spark.yarn.dist.pyFiles'] = self.jobConfig['spark.yarn.dist.pyFiles'] + self.pyFiles[i] + ",";
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .py, .zip or .egg - Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "JARS":
                        if (extension.toUpperCase() === "JAR") {
                            if (self.jars === []) {
                                self.jars = [path];
                            } else {
                                if (self.jars.indexOf(path) === -1) {
                                    self.jars.push(path);
                                }
                            }
                            self.jobConfig['spark.yarn.dist.jars'] = "";
                            for (var i = 0; i < self.jars.length; i++) {
                                self.jobConfig['spark.yarn.dist.jars'] = self.jobConfig['spark.yarn.dist.jars'] + self.jars[i] + ",";
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .jar - Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "ARCHIVES":
                        if (extension.toUpperCase() === "ZIP" || extension.toUpperCase() === "TGZ") {
                            if (self.archives === []) {
                                self.archives = [path];
                            } else {
                                if (self.archives.indexOf(path) === -1) {
                                    self.archives.push(path);
                                }
                            }
                            self.jobConfig['spark.yarn.dist.archives'] = "";
                            for (var i = 0; i < self.archives.length; i++) {
                                self.jobConfig['spark.yarn.dist.archives'] = self.jobConfig['spark.yarn.dist.archives'] + self.archives[i] + ",";
                            }
                        } else {
                            growl.error("Invalid file type selected. Expecting .zip Found: " + extension, {
                                ttl: 10000
                            });
                        }
                        break;
                    case "FILES":
                        if (self.files === []) {
                            self.files = [path];
                        } else {
                        if (self.files.indexOf(path) === -1) {
                                self.files.push(path);
                                }
                        }
                        self.jobConfig['spark.yarn.dist.files'] = "";
                        for (var i = 0; i < self.files.length; i++) {
                            self.jobConfig['spark.yarn.dist.files'] = self.jobConfig['spark.yarn.dist.files'] + self.files[i] + ",";
                        }
                        break;
                    default:
                        growl.error("Invalid file type selected: " + reason, {
                            ttl: 10000
                        });
                        break;
                }
            };

            self.remove = function(index, type) {
                var arr = [];
                if(type === 'files') {
                self.files.splice(index, 1);
                arr = self.files;
                } else if(type === 'jars') {
                self.jars.splice(index, 1);
                arr = self.jars;

                } else if(type === 'archives') {
                self.archives.splice(index, 1);
                arr = self.archives;

                } else {
                self.pyFiles.splice(index, 1);
                 arr = self.pyFiles;
                }
                self.jobConfig['spark.yarn.dist.' + type] = "";
                for (var i = 0; i < arr.length; i++) {
                    self.jobConfig['spark.yarn.dist.' + type] = self.jobConfig['spark.yarn.dist.' + type] + arr[i] + ",";
                }
            };

            self.distribution_strategies = [{
                id: 1,
                name: 'MIRRORED',
                displayName: 'Mirrored'
            }, {
                id: 2,
                name: 'COLLECTIVE_ALL_REDUCE',
                displayName: 'CollectiveAllReduce'
            }, {
                id: 3,
                name: 'PARAMETER_SERVER',
                displayName: 'ParameterServer'
            }];

            self.distributionStrategySelected;

            self.changeDistributionStrategy = function() {
                self.jobConfig.distributionStrategy = self.distributionStrategySelected.name;
            };

            self.autoExpand = function(e) {
                  var element = typeof e === 'object' ? e.target : document.getElementById(e);
                  var scrollHeight = element.scrollHeight; // replace 60 by the sum of padding-top and padding-bottom
                  var currentElemHeight = element.style.height.slice(0, -2);
                  if(currentElemHeight < scrollHeight) {
                    element.style.height = scrollHeight + "px";
                  }
            };
        }
    ]);