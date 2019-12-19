/*
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
 */

/**
 * Controller for the serving page.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('servingCtrl', ['$scope', '$routeParams', 'growl', 'ServingService', 'UtilsService', '$location',
        'PythonService', 'ModalService', '$interval', 'StorageService', '$mdSidenav', 'DataSetService',
        'KafkaService', 'JobService','ElasticService', 'ModelService',
        function ($scope, $routeParams, growl, ServingService, UtilsService, $location, PythonService,
                  ModalService, $interval, StorageService, $mdSidenav, DataSetService, KafkaService, JobService,
                  ElasticService, ModelService) {

            var self = this;

            // Instance number slider setting
            self.sliderOptions = {
                value: 1,
                options: {
                    floor: 1,
                    ceil: 1,
                    step: 1
                }
            };

            self.projectId = $routeParams.projectID;

            self.showLogs = false;
            self.kibanaUI = "";

            self.selectedModel;
            self.models = [];

            self.projectKafkaTopics = [];
            self.kafkaDefaultNumPartitions = 1;
            self.kafkaDefaultNumReplicas = 1;
            self.kafkaMaxNumReplicas = 1;

            self.servings = [];
            self.loaded = false;
            self.loading = false;
            self.loadingText = "";

            self.editServing = {};
            self.editServing.batchingEnabled = false;

            // Configuration to create a new Kafka topic for the serving
            self.createKafkaTopicDTO = {};
            self.createKafkaTopicDTO.name = "CREATE";
            self.createKafkaTopicDTO.numOfPartitions = self.kafkaDefaultNumPartitions;
            self.createKafkaTopicDTO.numOfReplicas = self.kafkaDefaultNumReplicas;

            // Configuration to not log incoming inference requests
            self.noneKafkaTopicDTO = {};
            self.noneKafkaTopicDTO.name = "NONE";
            self.noneKafkaTopicDTO.numOfPartitions = 0;
            self.noneKafkaTopicDTO.numOfReplicas = 0;

            self.activeTab = 'serving';
            self.showCreateNewServingForm = false;
            self.showAdvancedForm = false;

            self.sendingRequest = false;

            self.pageSize = 10;
            self.sortKey = 'created';
            self.reverse = true;

            self.sort = function(sortKey) {
                self.reverse = (self.sortKey=== sortKey) ? !self.reverse : false;
                self.sortKey = sortKey;
            };

            var datasetService = DataSetService(self.projectId);

            self.ignorePoll = false;
            self.createNewServingMode = false;

            self.editServing.servingType = "TENSORFLOW"
            self.sklearnScriptRegex = /(.py|.ipynb)\b/
            self.sklearnSelectScriptErrorMsg = "Please select a .py or .ipynb file."
            self.nameRegex = /^[a-zA-Z0-9]+$/;

            this.selectFile = function () {
                if (self.editServing.servingType === "TENSORFLOW") {
                    ModalService.selectDir('lg', self.projectId, '*', '').then(
                        function (success) {
                            self.onDirSelected(success);
                        },
                        function (error) {
                            // Users changed their minds.
                        });
                }
                if (self.editServing.servingType == "SKLEARN") {
                    ModalService.selectFile('lg', self.projectId, self.sklearnScriptRegex, self.sklearnSelectScriptErrorMsg,
                        false).then(
                        function (success) {
                            self.onFileSelected(success);
                        }, function (error) {
                            //The user changed their mind.
                        });
                }
            };

            /**
             * Check that the directory selected follows the required structure.
             * In particular, check that the children are integers (version numbers)
             *
             * @param artifactPath path to the model
             * @param name name of the serving
             */
            self.validateTfModelPath = function (artifactPath, name) {
                // /Projects/project_name/RelativePath
                var artifactPathSplits = artifactPath.split("/");
                var relativePath = artifactPathSplits.slice(3).join("/");
                if (typeof name === 'undefined') {
                    name = artifactPathSplits[artifactPathSplits.length - 1];
                }

                datasetService.getAllDatasets(relativePath).then(
                    function (success) {
                        var versions = [];
                        var files = success.data.items;
                        for (var version in files) {
                            if (!isNaN(files[version].attributes.name)) {
                                versions.push(files[version].attributes.name);
                            } else {
                                growl.error("Directory doesn't respect the required structure", {
                                    title: 'Error',
                                    ttl: 15000
                                });
                                return;
                            }
                        }

                        if (versions.length === 0) {
                            growl.error("The selected model doesn't have any servable version", {
                                title: 'Error',
                                ttl: 15000
                            });
                            return;
                        }

                        // If the validation phase has passed, update the UI
                        self.editServing.artifactPath = artifactPath;
                        self.editServing.name = name;
                        self.editServing.versions = versions.sort().reverse();

                        if (self.editServing.modelVersion == null) {
                            self.editServing.modelVersion = self.editServing.versions[0];
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            /**
             * Callback when the user selects a model containing a TfServing Model
             *
             * @param artifactPath the directory HDFS path
             */
            self.onDirSelected = function (artifactPath) {
                if (self.editServing.servingType === "TENSORFLOW") {
                    self.validateTfModelPath(artifactPath, undefined);
                }
            };

            /**
             * Validates that the user-provided path points to a python script
             *
             * @param artifactPath the hdfs path
             */
            self.validateSkLearnScriptPath = function (artifactPath, name) {
                var filename = getFileName(artifactPath);
                if (!filename.toLowerCase().endsWith(".py")) {
                    growl.error("You need to select a python script with file ending .py", {
                        title: 'Error - Invalid sklearn serving python script.',
                        ttl: 15000
                    });
                    return
                }
                if (typeof name === 'undefined' || name === null || name == "") {
                    name = filename.replace(".py", "");
                    name = name.replace(/_/g, "")
                }
                self.editServing.artifactPath = artifactPath;
                self.editServing.name = name;
                self.editServing.modelVersion = 1;
            }

            /**
             * Callback when the user selects a python script for Sklearn serving in the UI
             *
             * @param path the selected path
             */
            self.onFileSelected = function (path) {
                if (self.editServing.servingType === "SKLEARN") {
                    self.validateSkLearnScriptPath(path, self.editServing.name)
                }
            }


            self.showAdvanced = function () {
                self.showAdvancedForm = !self.showAdvancedForm;
            };


            self.hideCreateServingForm = function () {
                self.showCreateNewServingForm = false;
                self.createNewServingMode = false;
                self.editServing = {};
                self.editServing.kafkaTopicDTO = {};
                self.editServing.kafkaTopicDTO.name = "CREATE";
                self.editServing.kafkaTopicDTO.numOfPartitions = self.kafkaDefaultNumPartitions;
                self.editServing.kafkaTopicDTO.numOfReplicas = self.kafkaDefaultNumReplicas;
                self.editServing.batchingEnabled = false;
                self.editServing.servingType = "TENSORFLOW";
                self.versions = [];
                self.sliderOptions.value = 1;
                self.showAdvancedForm = false;
            };

            self.getAllServings = function () {
                if(!self.loaded) {
                    startLoading('Loading Servings...')
                }
                ServingService.getAllServings(self.projectId).then(
                    function (success) {
                        stopLoading();
                        if (!self.ignorePoll) {
                            self.servings = success.data;
                            self.loaded = true;
                        }
                    },
                    function (error) {
                        stopLoading();
                        self.loaded = true;
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            var startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };

            self.filterTopics = function (topic) {
                return topic.name === self.editServing.kafkaTopicDTO.name;
            };

            self.updateKafkaDetails = function () {
                if (self.editServing.kafkaTopicDTO.name === 'CREATE' ||
                    self.editServing.kafkaTopicDTO.name === 'NONE') {
                    return;
                }

                KafkaService.getTopicDetails(self.projectId, self.editServing.kafkaTopicDTO.name).then(
                    function (success) {
                        self.editServing.kafkaTopicDTO.numOfPartitions = success.data.length;
                        if (success.data.length > 0) {
                            self.editServing.kafkaTopicDTO.numOfReplicas = success.data[0].replicas.length
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            self.updateKafkaTopics = function () {
                KafkaService.getTopics(self.projectId).then(
                    function (success) {
                        self.projectKafkaTopics = [];
                        self.projectKafkaTopics.push(self.createKafkaTopicDTO);
                        self.projectKafkaTopics.push(self.noneKafkaTopicDTO);

                        for (var topic in success.data) {
                            if (success.data[topic].schemaName === self.kafkaSchemaName) {
                                self.projectKafkaTopics.push(success.data[topic]);
                            }
                        }

                        if (typeof self.editServing.kafkaTopicDTO !== "undefined") {
                            topic = self.projectKafkaTopics.filter(self.filterTopics);
                            self.editServing.kafkaTopicDTO = topic[0];
                            self.updateKafkaDetails();
                        } else {
                            self.editServing.kafkaTopicDTO = self.projectKafkaTopics[0];
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            self.showCreateServingForm = function () {
                self.showCreateNewServingForm = true;
                self.createNewServingMode = true;
                self.updateKafkaTopics();
            };

            self.containsServingStatus = function (status) {

                for (var j = 0; j < self.servings.length; j++) {
                    if (self.servings[j].status === status) {
                        return true;
                    }
                }

                return false;
            };

            self.getAllServings()

            /**
             * Create or update serving
             *
             * 1. Validate user input
             * 2. Send REST call to Hopsworks for creating the serving instance
             */
            self.createOrUpdate = function () {
                self.editServing.requestedInstances = self.sliderOptions.value;

                // Check that all the fields are populated
                if (self.editServing.artifactPath === "" || self.editServing.artifactPath == null ||
                    self.editServing.name === "" || self.editServing.name == null ||
                    self.editServing.modelVersion === "" || self.editServing.modelVersion == null) {
                    growl.error("Please fill out all the fields", {
                        title: 'Error',
                        ttl: 15000
                    });
                    return;
                }
                // Check that name is valid according to regex so that it can be used as a REST endpoint for inference
                if (!self.nameRegex.test(self.editServing.name)) {
                    growl.error("Serving/Model name must be valid according to regex: " + self.nameRegex, {
                        title: 'Error',
                        ttl: 15000
                    });
                    return
                }

                // Check that python kernel is enabled if it is a sklearn serving, as the flask serving will be launched
                // inside the project anaconda environment
                if (self.editServing.servingType === "SKLEARN") {
                    PythonService.enabled(self.projectId).then(
                        function (success) {
                            self.doCreateOrUpdate()
                        },
                        function (error) {
                            growl.error("You need to enable Python in your project before creating a SkLearn serving" +
                                " instance.", {
                                title: 'Error - Python not' +
                                    ' enabled yet.', ttl: 15000
                            });

                        });
                } else {
                    self.doCreateOrUpdate()
                }
            };

            self.doCreateOrUpdate = function () {
                self.sendingRequest = true;
                // Send REST call to Hopsworks with the data
                ServingService.createOrUpdate(self.projectId, self.editServing).then(
                    function (success) {
                        self.getAllServings();
                        self.hideCreateServingForm();
                        self.sendingRequest = false;
                    },
                    function (error) {
                        if (error.data !== undefined) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        }
                        self.sendingRequest = false;
                    });
            }

            /**
             * Function called when the user press the "Edit" button
             *
             * @param serving the serving to edit
             */
            self.updateServing = function (serving) {
                angular.copy(serving, self.editServing);
                self.editServing.modelVersion = self.editServing.modelVersion.toString();
                self.sliderOptions.value = serving.requestedInstances;
                if (self.editServing.servingType === "TENSORFLOW") {
                    self.validateTfModelPath(serving.artifactPath, serving.name);
                }
                self.showCreateServingForm();
            };

            /**
             * Called when the user press the "Delete" button
             *
             * @param serving the serving to delete
             */
            self.deleteServing = function (serving) {

                ServingService.deleteServing(self.projectId, serving.id).then(
                    function (success) {
                        self.getAllServings();
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            /**
             * Called when the user press the "Start/Stop" button
             *
             * @param serving the serving to start (if stopped) or stop (if already started)
             * @param action the action
             */
            self.startOrStopServing = function (serving, action) {
                if (action === 'START') {
                    self.ignorePoll = true;
                    serving.status = 'Starting';
                } else {
                    self.ignorePoll;
                    serving.status = 'Stopping';
                }

                ServingService.startOrStop(self.projectId, serving.id, action).then(
                    function (success) {
                        self.ignorePoll = false;
                    },
                    function (error) {
                        self.ignorePoll = false;
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 10000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 10000});
                        }
                    });

            };

            self.fetchAsync = function (modelNameLike) {
                self.loadingModels = false;
                var query = '';
                if(modelNameLike) {
                    query = '?filter_by=name_like:' + modelNameLike;
                }
                ModelService.getAll(self.projectId, query).then(
                    function(success) {
                        if(success.data.items) {
                            var modelNames = new Set();
                            for(var i = 0; i < success.data.items.length; i < i++) {
                                modelNames.add(success.data.items[i].name)
                            }
                            console.log(modelNames)
                            self.models = Array.from(modelNames);
                            console.log(self.models)
                        } else {
                            self.models = [];
                        }
                        self.loadingModels = true;
                    },
                    function(error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                        self.loadingModels = true;
                    });
            };

            /**
             * Function called when the user is switching between the form for creating a TFServing vs a SkLearnServing
             *
             * @param servingType the selected sereving type
             */
            self.setServingType = function (servingType) {
                self.editServing.artifactPath = ""
                self.editServing.name = ""
                self.editServing.modelVersion = ""
                self.editServing.servingType = servingType
            }

            self.setFullModelPath = function() {
                var projectName = UtilsService.getProjectName();
                self.editServing.artifactPath = '/Projects/' + projectName + '/Models/' + self.editServing.artifactPath;
                self.validateTfModelPath(self.editServing.artifactPath, undefined);
            };

            /**
             * Called when the user press the "Logs" button
             *
             * @param serving the serving to show the logs for
             */
            self.showServingLogs = function (serving) {
             var projectName = UtilsService.getProjectName();
             ElasticService.getJwtToken(self.projectId).then(
                 function (success) {
                    var kibanaUrl = success.data.kibanaUrl;
                     self.kibanaUI = kibanaUrl + "projectId=" + self.projectId +
                                        "#/discover?_g=()&_a=(columns:!(modelname,host,log_message,'@timestamp')," +
                                        "index:'" + projectName.toLowerCase() + "_serving-*',interval:auto," +
                                        "query:(language:lucene,query:'modelname:" + serving.name + "'),sort:!('@timestamp',desc))";
                     self.showLogs = true;

                 }, function (error) {
                     if (typeof error.data.usrMsg !== 'undefined') {
                         growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
             });
            };

            self.showDetailedInformation = function (serving) {
                ModalService.viewServingInfo('lg', self.projectId, serving).then(
                    function (success) {
                    }, function (error) {
                    });
            };

            self.showMainUI = function () {
                self.showLogs = false;
            };

            $scope.$on('$destroy', function () {
                $interval.cancel(self.poller);
            });

            self.poller = $interval(function () {
                self.getAllServings();
            }, 5000);

            /**
             * Called when the serving UI is loaded
             */
            self.init = function () {
                ServingService.getConfiguration().then(
                    function (success) {
                        self.sliderOptions.options.ceil = success.data.maxNumInstances;
                        self.kafkaSchemaName = success.data.kafkaTopicSchema;
                        self.kafkaSchemaVersion = success.data.kafkaTopicSchemaVersion;
                    },
                    function (error) {
                        self.ignorePoll = false;
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    }
                );

                KafkaService.defaultTopicValues().then(
                    function (success) {
                        self.kafkaDefaultNumPartitions = success.data.numOfPartitions;
                        self.kafkaDefaultNumReplicas = success.data.numOfReplicas;
                        self.kafkaMaxNumReplicas = success.data.maxNumOfReplicas;
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    }
                )
            };
            self.init();
        }
    ]);
