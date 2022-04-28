/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
        'ModalService', '$interval', 'StorageService', '$mdSidenav', 'DataSetService', 'KafkaService', 'JobService',
        'ElasticService', 'ModelService', 'VariablesService', '$mdToast',
        function ($scope, $routeParams, growl, ServingService, UtilsService, $location, ModalService, $interval,
                  StorageService, $mdSidenav, DataSetService, KafkaService, JobService, ElasticService, ModelService,
                  VariablesService, $mdToast) {

            var self = this;

            // Instance number slider setting
            self.predictorSliderOptions = {
                value: 1,
                options: {
                    floor: 1,
                    ceil: 1,
                    step: 1
                }
            };
            self.transformerSliderOptions = {
                value: 0,
                options: {
                    floor: 0,
                    ceil: 1,
                    step: 1,
                    disabled: true
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

            self.sort = function (sortKey) {
                self.reverse = (self.sortKey === sortKey) ? !self.reverse : false;
                self.sortKey = sortKey;
            };

            var datasetService = DataSetService(self.projectId);

            self.ignorePoll = false;
            self.createNewServingMode = false;

            self.pythonScriptRegex = /(.py|.ipynb)\b/
            self.pythonSelectScriptErrorMsg = "Please select a .py or .ipynb file."
            self.nameRegex = /^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$/;

            self.kfserving = false;

            self.editServing = {};
            self.editServing.batchingEnabled = false;
            self.editServing.modelServer = "TENSORFLOW_SERVING";
            self.editServing.servingTool = "DEFAULT";
            if (self.defaultDockerConfig) {
                self.editServing.predictorResourceConfig = JSON.parse(JSON.stringify(self.defaultDockerConfig.resourceConfig))
            }

            self.artifactsDirName = "Artifacts";
            self.artifactCreate = "CREATE";
            self.artifactModelOnly = "MODEL-ONLY"
            self.modelServerPython = "PYTHON";
            self.modelServerTensorflow = "TENSORFLOW_SERVING";
            self.servingToolDefault = "DEFAULT";
            self.servingToolKFServing = "KFSERVING";

            self.inferenceLoggingDefaultModelInputs = true;
            self.inferenceLoggingDefaultPredictions = true;
            self.inferenceLoggingModelInputs = self.inferenceLoggingDefaultModelInputs;
            self.inferenceLoggingPredictions = self.inferenceLoggingDefaultPredictions;


            /* REST API requests */

            self.getAllServings = function () {
                if (!self.loaded) {
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

            /**
             * Create or update serving
             *
             * 1. Validate user input
             * 2. Send REST call to Hopsworks for creating the serving instance
             */
            self.createOrUpdate = function () {
                self.editServing.requestedInstances = self.predictorSliderOptions.value;
                self.editServing.requestedTransformerInstances = self.editServing.transformer != null
                    ? self.transformerSliderOptions.value
                    : null;

                // Check that all the fields are populated
                if (self.editServing.modelPath === "" || self.editServing.modelPath == null ||
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

                // Set inference logging mode
                if (self.inferenceLoggingModelInputs) {
                    if (self.inferenceLoggingPredictions) {
                        self.editServing.inferenceLogging = "ALL";
                    } else {
                        self.editServing.inferenceLogging = "MODEL_INPUTS";
                    }
                } else if (self.inferenceLoggingPredictions) {
                    self.editServing.inferenceLogging = "PREDICTIONS";
                } else {
                    self.editServing.inferenceLogging = undefined;
                }

                self.doCreateOrUpdate()
            };

            self.updateKafkaDetails = function (updateMode) {
                if (updateMode) {
                    self.inferenceLoggingPredictions = self.inferenceLoggingModelInputs = self.editServing.kafkaTopicDTO.name !== 'NONE';
                }
                if (self.editServing.kafkaTopicDTO.name === 'NONE' || self.editServing.kafkaTopicDTO.name === 'CREATE') {
                    return; // noop
                }

                KafkaService.getTopicDetails(self.projectId, self.editServing.kafkaTopicDTO.name).then(
                    function (success) {
                        self.editServing.kafkaTopicDTO.numOfPartitions = success.data.items.length;
                        if (success.data.items.length > 0) {
                            self.editServing.kafkaTopicDTO.numOfReplicas = success.data.items[0].replicas.length
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            };

            self.updateKafkaTopics = function (updateMode) {
                KafkaService.getTopics(self.projectId).then(
                    function (success) {
                        self.projectKafkaTopics = [];
                        self.projectKafkaTopics.push(self.createKafkaTopicDTO);
                        self.projectKafkaTopics.push(self.noneKafkaTopicDTO);

                        var topics = success.data["items"]
                        for (var idx in topics) {
                            var topic = topics[idx]
                            if (topic.schemaName === self.kafkaSchemaName) {
                                self.projectKafkaTopics.push(topic);
                            }
                        }

                        if (typeof self.editServing.kafkaTopicDTO !== "undefined") {
                            topics = self.projectKafkaTopics.filter(self.filterTopics);
                            self.editServing.kafkaTopicDTO = topics[0];
                            self.updateKafkaDetails(updateMode);
                        } else {
                            if (self.editServing.id) {
                                topics = self.projectKafkaTopics.filter(function (t) {
                                    return t.name === "NONE"
                                });
                                self.editServing.kafkaTopicDTO = topics[0];
                            } else {
                                self.editServing.kafkaTopicDTO = self.projectKafkaTopics[0];
                            }
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
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
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 15000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 15000});
                        }
                        self.sendingRequest = false;
                    });
            };

            /**
             * Function called when the user press the "Edit" button
             *
             * @param serving the serving to edit
             */
            self.updateServing = function (serving) {
                angular.copy(serving, self.editServing);
                self.editServing.modelVersion = self.editServing.modelVersion.toString();
                self.editServing.artifactVersion = self.editServing.artifactVersion ? self.editServing.artifactVersion.toString() : undefined;
                self.predictorSliderOptions.value = serving.requestedInstances;
                if (self.editServing.transformer != null) {
                    self.transformerSliderOptions.value = serving.requestedTransformerInstances;
                }
                self.kfserving = self.editServing.servingTool === self.servingToolKFServing;
                self.validateModelPath(serving.modelPath, serving.name)

                if (serving.predictorResourceConfig) {
                    self.editServing.predictorResourceConfig = serving.predictorResourceConfig;
                }

                self.showCreateServingForm(false);
                self.setKFServing();
                self.setInferenceLogging(self.editServing.inferenceLogging);
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
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 15000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 15000});
                        }
                    });
            };

            /**
             * Called when the user press the "Start/Stop" button
             *
             * @param serving the serving to start (if stopped) or stop (if already started)
             * @param action the action
             */
            self.startOrStopServing = function (serving, action) {
                self.ignorePoll = true;
                if (action === 'START') {
                    serving.status = 'Starting';
                } else if (serving.servingTool === 'DEFAULT') {
                    serving.status = 'Stopping';
                }

                ServingService.startOrStop(self.projectId, serving.id, action).then(
                    function (success) {
                        self.ignorePoll = false;
                        if (action === 'STOP' && serving.servingTool !== 'DEFAULT') {
                            serving.status = 'Stopping';
                        }
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
                if (modelNameLike) {
                    query = '?filter_by=name_like:' + modelNameLike;
                }
                ModelService.getAll(self.projectId, query).then(
                    function (success) {
                        if (success.data.items) {
                            var modelNames = new Set();
                            for (var i = 0; i < success.data.items.length; i < i++) {
                                modelNames.add(success.data.items[i].name)
                            }
                            self.models = Array.from(modelNames);
                        } else {
                            self.models = [];
                        }
                        self.loadingModels = true;
                    },
                    function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 8000});
                        }
                        self.loadingModels = true;
                    });
            };

            /**
             * Check that the model directory selected follows the required structure.
             * In particular, check that the children are integers (version numbers)
             *
             * @param modelPath path to the model
             * @param name name of the serving
             */
            self.validateModelPath = function(modelPath, name) {
                // /Projects/project_name/Models/model_name
                var pattern = /\/Projects\/\w+\/Models\/\w+/g;
                if (!pattern.test(modelPath)) {
                    growl.error("Please, select a model folder from Models dataset", {
                        title: 'Error - Invalid model folder.',
                        ttl: 15000
                    });
                    return
                }

                var modelPathSplits = modelPath.split("/");
                var relativePath = modelPathSplits.slice(3).join("/");

                // Get model versions
                datasetService.getAllDatasets(relativePath).then(
                    function (success) {
                        var versions = [];
                        var files = success.data.items;
                        for (var idx in files) {
                            var version = files[idx].attributes.name
                            if (!isNaN(parseInt(version))) {
                                versions.push(version);
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

                        self.setModelName(modelPathSplits[modelPathSplits.length - 1])
                        self.setModelPath(modelPath, versions)
                        self.setServingName(name)
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                        });
                    });
            }

            /**
             * Validates that the user-provided path points to a python script
             *
             * @param path the hdfs path
             */
            self.validatePythonScriptPath = function (path, name, component, extensions) {
                // Check extension
                if (extensions.filter(function (ext) {
                    return path.endsWith(ext);
                }).length <= 0) {
                    growl.error("Please, select a python script ending with " + extensions.join(" or "), {
                        title: 'Error - Invalid python script.',
                        ttl: 15000
                    });
                    return
                }

                if (component === "predictor") {
                    self.editServing.predictor = path;
                    self.validatePredictor();
                } else {
                    self.editServing.transformer = path;
                    self.validateTransformer();
                }
            };

            self.getDockerConfiguration = function () {
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
                JobService.getConfiguration(self.projectId, "docker").then(
                    function (success) {
                        self.defaultDockerConfig = success.data;
                    }, function (error) {
                    });
            };

            self.getDockerConfiguration();

            self.range = function (min, max) {
                var input = [];
                for (var i = min; i <= max; i++) {
                    input.push(i);
                }
                return input;
            };

            /* Redirects */

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
                            "#/discover?_g=()&_a=(columns:!(serving_name,log_message,'@timestamp')," +
                            "index:'" + projectName.toLowerCase() + "_serving-*',interval:auto," +
                            "query:(language:lucene,query:'serving_name:" + serving.name + "'),sort:!('@timestamp',desc))";
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
                ModalService.viewServingInfo('lg', self.projectId, serving, self.isKubernetes, self.knativeDomainName).then(
                    function (success) {
                    }, function (error) {
                    });
            };

            self.showMainUI = function () {
                self.showLogs = false;
            };

            /* UI handlers */

            // Select script for predictor or transformer
            this.selectFile = function (component) {
                if (component === "model") {
                    ModalService.selectDir('lg', self.projectId, '*', '').then(
                        function (success) {
                            self.onDirSelected(success);
                        },
                        function (error) {
                            // Users changed their minds.
                        });
                } else if (component === "predictor") {
                    if (self.editServing.modelServer === self.modelServerPython) {
                        ModalService.selectFile('lg', self.projectId, self.pythonScriptRegex,
                            self.pythonSelectScriptErrorMsg, false).then(
                            function (success) {
                                self.onFileSelected(component, success);
                            }, function (error) {
                                //The user changed their mind.
                            });
                    }
                } else if (component === "transformer") {
                    if (self.kfserving) {
                        ModalService.selectFile('lg', self.projectId, self.pythonScriptRegex,
                            self.pythonSelectScriptErrorMsg, false).then(
                            function (success) {
                                self.onFileSelected(component, success);
                            }, function (error) {
                                //The user changed their mind.
                            });
                    }
                }
            };

            /**
             * Callback when the user selects a model path containing a model
             *
             * @param modelPath the directory HDFS path
             */
            self.onDirSelected = function (modelPath) {
                self.validateModelPath(modelPath, undefined)
            };

            /**
             * Callback when the user selects a python script for serving in the UI
             *
             * @param path the selected path
             */
            self.onFileSelected = function (component, path) {
                if (component === "predictor") {
                    if (self.editServing.modelServer === self.modelServerPython) {
                        self.validatePythonScriptPath(path, self.editServing.name, component, [".py"])
                    }
                } else if (component === "transformer") {
                    self.validatePythonScriptPath(path, self.editServing.name, component, [".py", ".ipynb"])
                }
            };

            self.showAdvanced = function () {
                self.showAdvancedForm = !self.showAdvancedForm;
            };

            self.hideCreateServingForm = function () {
                self.showCreateNewServingForm = false;
                self.createNewServingMode = false;
                self.versions = [];
                self.predictorSliderOptions.value = 1;
                self.transformerSliderOptions.value = 0;
                self.showAdvancedForm = false;
                self.kfserving = false;

                self.editServing = {};
                self.editServing.modelPath = null;
                self.editServing.kafkaTopicDTO = self.projectKafkaTopics[0];
                self.editServing.kafkaTopicDTO.numOfPartitions = self.kafkaDefaultNumPartitions;
                self.editServing.kafkaTopicDTO.numOfReplicas = self.kafkaDefaultNumReplicas;
                self.editServing.batchingEnabled = false;
                self.editServing.modelServer = self.modelServerTensorflow;
                self.editServing.servingTool = self.servingToolDefault;
                self.editServing.transformer = null;
                self.versions = [];
                self.artifactVersions = [];
                self.artifactVersion = null;
                self.inferenceLoggingModelInputs = self.inferenceLoggingDefaultModelInputs;
                self.inferenceLoggingPredictions = self.inferenceLoggingDefaultPredictions;
                self.showAdvancedForm = false;
            };

            var startLoading = function (label) {
                self.loading = true;
                self.loadingText = label;
            };
            var stopLoading = function () {
                self.loading = false;
                self.loadingText = "";
            };

            var showToast = function (text) {
                var toast = $mdToast.simple()
                    .textContent(text)
                    .action('Close')
                    .position('bottom right')
                    .hideDelay(0);

                $mdToast.show(toast).then(function (response) {
                    if (response == 'ok') {
                        $mdToast.hide();
                    }
                });
            };

            var closeToast = function () {
                $mdToast.hide();
            };

            self.filterTopics = function (topic) {
                return topic.name === self.editServing.kafkaTopicDTO.name;
            };

            self.showCreateServingForm = function (isNewServing) {
                self.showCreateNewServingForm = true;
                self.createNewServingMode = true;
                self.updateKafkaTopics(isNewServing);
                if (!self.editServing.predictorResourceConfig && self.defaultDockerConfig) {
                    self.editServing.predictorResourceConfig = JSON.parse(JSON.stringify(self.defaultDockerConfig.resourceConfig));
                }
            };

            self.containsServingStatus = function (status) {
                for (var j = 0; j < self.servings.length; j++) {
                    if (self.servings[j].status === status) {
                        return true;
                    }
                }
                return false;
            };

            self.setFullModelPath = function () {
                var projectName = UtilsService.getProjectName();
                self.editServing.modelPath = '/Projects/' + projectName + '/Models/' + self.editServing.modelName;
                self.validateModelPath(self.editServing.modelPath, undefined);
            };

            self.setServingName = function (name) {
                if (!self.editServing.name) {
                    if (name) {
                        self.editServing.name = name;
                    } else {
                        const modelPathSplits = self.editServing.modelPath.split("/");
                        //When a model is selected from the filesystem browser, only keep alphanumeric characters to conform to serving regex
                        self.editServing.name = modelPathSplits[modelPathSplits.length - 1].replace(/[^a-zA-Z0-9]/g, '');
                    }
                }
            }

            self.setModelName = function (modelName) {
                self.editServing.modelName = modelName;
            }

            self.setModelPath = function (modelPath, modelVersions) {
                self.editServing.modelPath = modelPath;
                self.setModelVersion(null, modelVersions);
            };

            self.setModelVersion = function (modelVersion, modelVersions) {
                if (modelVersions) {
                    self.modelVersions = modelVersions.sort().reverse();
                }
                if (modelVersion) {
                    self.editServing.modelVersion = modelVersion;
                } else if (self.editServing.modelVersion == null) {
                    self.editServing.modelVersion = self.modelVersions[0];
                }
                self.setArtifactVersion()
            };

            self.setArtifactVersion = function (artifactVersion) {
                if (!self.isKubernetes) {
                    self.editServing.artifactVersion = self.artifactVersion = undefined;
                    return;
                }

                if (self.editServing.modelVersion == null || artifactVersion === null) {
                    return; /* Ignore change */
                }

                if (artifactVersion) {
                    const version = self.findArtifactVersion(artifactVersion.key, null);
                    if (version) {
                        self.artifactVersion = version;
                        self.editServing.artifactVersion = version.key;
                        self.setPredictor();
                        self.setTransformer();
                        return;
                    }
                }

                // Build relative path
                const slices = self.editServing.modelPath.split("/").slice(3);
                slices.push(self.editServing.modelVersion, self.artifactsDirName)
                const path = slices.join("/");

                self.artifactVersions = [{key: "-1", value: self.artifactCreate}]

                // Get artifact versions
                datasetService.getAllDatasets(path).then(
                    function (success) {
                        const files = success.data.items;
                        for (var idx in files) {
                            const name = files[idx].attributes.name;
                            if (!isNaN(parseInt(name))) {
                                self.artifactVersions.push({
                                    key: name,
                                    value: name === '0' ? self.artifactModelOnly : name
                                })
                            } else {
                                growl.error("Directory doesn't respect the required structure", {
                                    title: 'Error',
                                    ttl: 15000
                                });
                                return;
                            }
                        }
                        self.artifactVersions = self.artifactVersions.sort(function (a1, a2) {
                            return (a1.key > a2.key) ? 1 : -1
                        });
                        if (artifactVersion) {
                            const version = self.findArtifactVersion(artifactVersion.key, null);
                            if (version) {
                                self.artifactVersion = version;
                                self.editServing.artifactVersion = version.key;
                                return;
                            } else {
                                growl.error("Artifact version not found", {
                                    title: 'Error',
                                    ttl: 15000
                                });
                                return;
                            }
                        }
                        if (self.editServing.artifactVersion == null) {
                            self.artifactVersion = self.artifactVersions.slice(-1)[0];
                            self.editServing.artifactVersion = self.artifactVersion.key;
                        } else {
                            const version = self.findArtifactVersion(self.editServing.artifactVersion, self.artifactVersions.slice(-1)[0]);
                            self.artifactVersion = version;
                            self.editServing.artifactVersion = self.artifactVersion.key;
                        }

                        self.setPredictor();
                        self.setTransformer();
                    },
                    function (error) {
                        if (error.data.errorCode == 110018) {
                            // If artifacts folder is not found
                            self.artifactVersion = self.artifactVersions.slice(-1)[0];
                            self.editServing.artifactVersion = self.artifactVersion.key;
                            self.setPredictor();
                            self.setTransformer();
                        } else {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        }
                    });
            };

            self.findArtifactVersion = function (artifactVersion, defaultVersion) {
                var version = defaultVersion;
                for (var idx in self.artifactVersions) {
                    if (self.artifactVersions[idx].key === artifactVersion) {
                        version = self.artifactVersions[idx];
                    }
                }
                return version;
            };

            self.setPredictor = function(predictor) {
                if (self.artifactVersion == null) {
                    return; /* Ignore change */
                }

                if (self.editServing.modelServer === self.modelServerTensorflow) {
                    self.editServing.predictor = null;
                    return; // noop
                }

                if (predictor || predictor === "") {
                    self.editServing.predictor = predictor;
                    self.validatePredictor();
                    return;
                }

                var version = self.artifactVersion.value;
                if (version === self.artifactModelOnly || version === self.artifactCreate) {
                    self.editServing.predictor = null;
                    self.validatePredictor();
                } else {
                    var slices = self.editServing.modelPath.split("/").slice(3);
                    slices.push(self.editServing.modelVersion, self.artifactsDirName, version)
                    var path = slices.join("/");
                    datasetService.getAllDatasets(path).then(
                        function (success) {
                            var files = success.data.items;
                            var predictor;
                            for (var idx in files) {
                                var name = files[idx].attributes.name;
                                var prefix = "predictor-";
                                if (name.startsWith(prefix)) {
                                    predictor = name.slice(prefix.length);
                                    break
                                }
                            }
                            if (predictor) {
                                self.editServing.predictor = predictor;
                                self.validatePredictor();
                            }
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        });
                }
            }

            self.setTransformer = function (transformer) {
                if (self.artifactVersion == null) {
                    return; /* Ignore change */
                }

                if (transformer || transformer === "") {
                    self.editServing.transformer = transformer;
                    self.validateTransformer();
                    return;
                }

                var version = self.artifactVersion.value;
                if (version === self.artifactModelOnly || version === self.artifactCreate) {
                    self.editServing.transformer = null;
                    self.validateTransformer();
                } else {
                    var slices = self.editServing.modelPath.split("/").slice(3);
                    slices.push(self.editServing.modelVersion, self.artifactsDirName, version)
                    var path = slices.join("/");
                    datasetService.getAllDatasets(path).then(
                        function (success) {
                            var files = success.data.items;
                            var transformer;
                            for (var idx in files) {
                                var name = files[idx].attributes.name;
                                var prefix = "transformer-";
                                if (name.startsWith(prefix)) {
                                    transformer = name.slice(prefix.length);
                                    break
                                }
                            }
                            if (transformer) {
                                self.editServing.transformer = transformer;
                                self.validateTransformer();
                            }
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                            });
                        });
                }
            };

            self.setInferenceLogging = function (loggingMode) {
                self.inferenceLoggingModelInputs = self.inferenceLoggingPredictions = false;

                if (!loggingMode) {
                    return;
                }
                switch (loggingMode) {
                    case "PREDICTIONS":
                        self.inferenceLoggingPredictions = true;
                        break;
                    case "MODEL_INPUTS":
                        self.inferenceLoggingModelInputs = true;
                        break;
                    case "ALL":
                        self.inferenceLoggingModelInputs = self.inferenceLoggingPredictions = true;
                        break;
                }
            }

            self.validatePredictor = function () {
                if (self.editServing.modelServer === self.modelServerTensorflow) {
                    return; // noop
                }

                if (self.editServing.predictor == null) {
                    self.editServing.kfserving = true;
                    self.setKFServing();
                } else {
                    if (self.editServing.predictor.includes("/")) {
                        // If user selected a predictor, enable KFServing and set new artifact
                        if (self.editServing.artifactVersion !== self.artifactCreate) {
                            self.artifactVersion = self.artifactVersions[0]; // CREATE
                            self.editServing.artifactVersion = self.artifactVersion.key;
                        }
                    }
                }
            }

            self.validateTransformer = function () {
                if (self.editServing.transformer == null) {
                    self.transformerSliderOptions.options.disabled = true;
                } else {
                    if (self.editServing.transformer.includes("/")) {
                        // If user selected a transformer, enable KFServing and set new artifact
                        if (self.editServing.artifactVersion !== self.artifactCreate) {
                            self.artifactVersion = self.artifactVersions[0]; // CREATE
                            self.editServing.artifactVersion = self.artifactVersion.key;
                        }
                    }
                    self.transformerSliderOptions.options.disabled = false;
                    self.kfserving = true;
                    self.setKFServing();
                }
            };

            /**
             * Function called when the user is switching between the form for creating a TF Serving or Python
             *
             * @param modelServer the selected model server
             */
            self.setModelServer = function (modelServer) {
                self.editServing.modelPath = undefined;
                self.editServing.name = undefined;
                self.editServing.modelName = undefined;
                self.editServing.modelVersion = undefined;
                self.editServing.artifactVersion = undefined;
                self.editServing.predictor = undefined;
                self.editServing.transformer = undefined;
                self.editServing.modelServer = modelServer;

                self.artifactVersions = [];
                self.artifactVersion = null;

                self.kfserving = false;

                if (modelServer === self.modelServerPython) {
                    // request batching is not supported with Python
                    self.editServing.batchingEnabled = false;
                }

                // refresh kfserving settings
                self.setKFServing();
            };

            self.setKFServing = function (kfserving) {
                if (typeof kfserving !== 'undefined') {
                    self.kfserving = kfserving;
                }

                if (self.kfserving) {
                    self.editServing.servingTool = self.servingToolKFServing;
                    self.editServing.batchingEnabled = false;
                    self.predictorSliderOptions.options.floor = 0;
                    if (self.editServing.modelServer === self.modelServerTensorflow) {
                        self.editServing.predictor = null;
                    }
                } else {
                    self.editServing.servingTool = self.servingToolDefault;
                    self.editServing.transformer = null;
                    self.predictorSliderOptions.options.floor = 1;
                    if (self.predictorSliderOptions.value === 0) {
                        self.predictorSliderOptions.value = 1;
                    }
                    self.transformerSliderOptions.options.disabled = true;
                    self.transformerSliderOptions.value = 0;
                    self.validateTransformer();
                    if (self.artifactVersions && self.artifactVersions.length > 0) {
                        self.artifactVersion = self.artifactVersions[0];
                        if (!self.editServing.predictor) {
                            for (var idx in self.artifactVersions) {
                                if (self.artifactVersions[idx].value === self.artifactModelOnly) {
                                    self.artifactVersion = self.artifactVersions[idx];
                                    break;
                                }
                            }
                        }
                        self.editServing.artifactVersion = self.artifactVersion.key;
                    }
                }
            };

            // System variables

            self.setIsKFServing = function (isKFServing) {
                self.isKFServing = isKFServing;
                if (!self.isKFServing) {
                    self.kfserving = false;
                }
            };

            self.setIsKubernetes = function (isKubernetes) {
                self.isKubernetes = isKubernetes;
                if (!self.isKubernetes) {
                    self.predictorSliderOptions.options.disabled = true;
                    self.transformerSliderOptions.options.disabled = true;
                }
            };

            // Get all servings
            self.getAllServings();

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
                        self.predictorSliderOptions.options.ceil = success.data.maxNumInstances;
                        self.transformerSliderOptions.options.ceil = success.data.maxNumInstances;
                        self.kafkaSchemaName = success.data.kafkaTopicSchema;
                        self.kafkaSchemaVersion = success.data.kafkaTopicSchemaVersion;
                    },
                    function (error) {
                        console.log("Error getting configuration")
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
                );

                VariablesService.isKubernetes().then(
                    function (success) {
                        self.setIsKubernetes(success.data.successMessage === "true")
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch if kubernetes is enabled',
                            ttl: 15000
                        });
                    }
                );

                VariablesService.isKFServing().then(
                    function (success) {
                        self.setIsKFServing(success.data.successMessage === "true")
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch if kfserving is enabled',
                            ttl: 15000
                        });
                    }
                );

                VariablesService.knativeDomainName().then(
                    function (success) {
                        self.knativeDomainName = success.data.successMessage
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch knative domain name',
                            ttl: 15000
                        });
                    }
                );
            };
            self.init();
        }
    ]);
