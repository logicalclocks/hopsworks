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
 * Controller for managing the "create new training dataset" page
 */
angular.module('hopsWorksApp')
    .controller('newTrainingDatasetCtrl', ['$routeParams', 'growl',
        '$location', 'StorageService', 'FeaturestoreService', 'ModalService',
        function ($routeParams, growl, $location, StorageService, FeaturestoreService, ModalService) {

            var self = this;

            //Controller Input
            self.projectId = $routeParams.projectID;
            self.featurestore = StorageService.get(self.projectId + "_featurestore")
            self.projectName = StorageService.get("projectName");
            self.features = StorageService.get(self.projectId + "_fgFeatures");
            self.trainingDatasetOperation = StorageService.get("trainingdataset_operation");
            self.trainingDataset = StorageService.get(self.projectId + "_trainingDataset");
            self.storageConnectors = StorageService.get(self.projectId + "_storageconnectors")
            self.settings = StorageService.get(self.projectId + "_fssettings")
            self.newJobName = self.projectId + "_newjob";

            //State
            self.showCart = true
            self.configureJob = true;
            self.selectFeatureStage = true;
            self.phase = 0
            self.working = false
            self.queryPlan = {
                "features": [],
                "featuregroups": [],
                "featuregroupToFeatures": {},
                "joinKey": [],
                "possibleJoinKeys": []
            }
            self.version = 1
            self.s3Connectors = []
            self.hopsfsConnectors = []
            self.selectedS3Connector = null
            self.selectedHopsfsConnector = null
            self.sinkType = 0
            self.path = ""

            //Constants
            self.trainingDatasetNameMaxLength = self.settings.featurestoreEntityNameMaxLength
            self.trainingDatasetDescriptionMaxLength = self.settings.featurestoreEntityDescriptionMaxLength
            self.trainingDatasetNameRegexp = self.settings.featurestoreRegex
            self.dataFormats = self.settings.trainingDatasetDataFormats
            self.hopsfsTrainingDatasetType = self.settings.hopsfsTrainingDatasetType
            self.hopsfsTrainingDatasetTypeDTO = self.settings.hopsfsTrainingDatasetDtoType
            self.externalTrainingDatasetType = self.settings.externalTrainingDatasetType
            self.externalTrainingDatasetTypeDTO = self.settings.externalTrainingDatasetDtoType
            self.s3ConnectorType = self.settings.s3ConnectorType
            self.hopsfsConnectorType = self.settings.hopsfsConnectorType
            self.featuregroupType = self.settings.featuregroupType
            self.trainingDatasetType = self.settings.trainingDatasetType
            self.featurestoreUtil4jMainClass = self.settings.featurestoreUtil4jMainClass
            self.featurestoreUtilPythonMainClass = self.settings.featurestoreUtilPythonMainClass
            self.featurestoreUtil4JExecutable = self.settings.featurestoreUtil4jExecutable
            self.featurestoreUtilPythonExecutable = self.settings.featurestoreUtilPythonExecutable
            self.sparkJobType = "SPARK"
            self.pySparkJobType = "PYSPARK"

            //Input Variables
            self.trainingDatasetName = ""
            self.trainingDatasetDoc = ""
            self.trainingDatasetFormat = null;
            self.featureBasket = []

            /**
             * Input validation
             */
            self.trainingDatasetWrong_values = 1;
            //Name and Description Flags
            self.trainingDatasetNameWrongValue = 1;
            self.trainingDatasetDocWrongValue = 1;
            //Query Plan Flags
            self.queryPlanWrongValue = 1;
            self.joinKeyWrongValue = 1;
            //Data format flags
            self.trainingDatasetDataFormatWrongValue = 1
            //Output sink flags
            self.sinkWrongValue = 1;
            self.trainingDatasetSinkNotSelected = 1

            //front-end variables
            self.td_accordion0 = {
                "isOpen": false,
                "visible": true,
                "value": "",
                "title": "Query Plan"
            };

            self.td_accordion1 = {
                "isOpen": true,
                "visible": true,
                "value": "",
                "title": "Training Dataset Name"
            };
            self.td_accordion2 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Training Dataset Description"
            };
            self.td_accordion3 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Training Dataset Format"
            };
            self.td_accordion4 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Create"
            };

            self.td_accordion5 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Output Location"
            };

            self.td_accordion6 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Schema"
            };

            /**
             * Perform initialization of variables that require it
             */
            self.initVariables = function () {
                self.trainingDatasetFormat = self.dataFormats[1]
                //self.projectName + "_Training_Datasets"
                self.s3Connectors = []
                self.selectedS3Connector = null
                self.hopsfsConnectors = []
                self.selectedHopsfsConnector = null;
                for (var i = 0; i < self.storageConnectors.length; i++) {
                    if (self.storageConnectors[i].storageConnectorType == self.s3ConnectorType) {
                        self.s3Connectors.push(self.storageConnectors[i])
                    }
                    if (self.storageConnectors[i].storageConnectorType == self.hopsfsConnectorType) {
                        self.hopsfsConnectors.push(self.storageConnectors[i])
                        if (self.storageConnectors[i].name === self.projectName + "_Training_Datasets") {
                            self.selectedHopsfsConnector = self.storageConnectors[i]
                        }
                    }
                }
                if (self.selectedHopsfsConnector === null && self.hopsfsConnectors.length > 0) {
                    self.selectedHopsfsConnector = self.hopsfsConnectors[0]
                }
                if (self.trainingDataset != null &&
                    (self.trainingDatasetOperation === 'UPDATE' || self.trainingDatasetOperation === 'NEW_VERSION')) {
                    self.trainingDatasetName = self.trainingDataset.name
                    self.trainingDatasetDoc = self.trainingDataset.description
                    for (var i = 0; i < self.dataFormats; i++) {
                        if (self.dataFormats[i] === self.trainingDataset.dataFormat) {
                            self.trainingDatasetFormat = self.dataFormats[i]
                        }
                    }
                    //self.featureBasket = self.trainingDataset.features //Needs provenance with information about
                    // which featuregroup a feature originates from for this to work
                    self.td_accordion2.isOpen = true
                    self.td_accordion2.visible = true
                    self.td_accordion3.isOpen = false
                    self.td_accordion3.visible = true
                    self.td_accordion3.isOpen = true
                    self.td_accordion4.visible = true
                    self.td_accordion4.isOpen = true

                    if (self.trainingDatasetOperation === 'UPDATE') {
                        self.version = self.trainingDataset.version
                        self.td_accordion4.title = "Update"
                    }
                    if (self.trainingDatasetOperation === 'NEW_VERSION') {
                        self.version = self.trainingDataset.version + 1
                    }
                }
                if(self.trainingDataset != null && self.trainingDatasetOperation === 'UPDATE'){
                    self.selectFeatureStage = false;
                    self.td_accordion6.visible = true
                    self.td_accordion6.isOpen = false
                }


            }

            /**
             * Validates user input for creating new training dataset
             */
            self.validateTrainingDatasetInput = function () {
                self.trainingDatasetNameWrongValue = 1
                self.trainingDatasetDataFormatWrongValue = 1
                self.trainingDatasetDocWrongValue = 1;
                self.queryPlanWrongValue = 1;
                self.joinKeyWrongValue = 1
                self.trainingDatasetWrong_values = 1;
                self.trainingDatasetSinkNotSelected = 1
                self.sinkWrongValue = 1
                self.working = true;
                if (!self.trainingDatasetName || self.trainingDatasetName.search(self.trainingDatasetNameRegexp) == -1) {
                    self.trainingDatasetNameWrongValue = -1;
                    self.trainingDatasetWrong_values = -1;
                } else {
                    self.trainingDatasetNameWrongValue = 1;
                }
                if (!self.trainingDatasetFormat || self.dataFormats.indexOf(self.trainingDatasetFormat) < 0) {
                    self.trainingDatasetDataFormatWrongValue = -1;
                    self.trainingDatasetWrong_values = -1;
                } else {
                    self.trainingDatasetDataFormatWrongValue = 1;
                }
                if (!self.trainingDatasetDoc || self.trainingDatasetDoc == undefined) {
                    self.trainingDatasetDoc = ""
                }
                if (self.trainingDatasetDoc.length > self.trainingDatasetDescriptionMaxLength) {
                    self.trainingDatasetDocWrongValue = -1;
                    self.trainingDatasetWrong_values = -1;
                } else {
                    self.trainingDatasetDocWrongValue = 1;
                }
                if(self.trainingDatasetOperation !== 'UPDATE') {
                    if (self.queryPlan.possibleJoinKeys.length === 0) {
                        self.trainingDatasetWrong_values = -1;
                        self.queryPlanWrongValue = -1;
                        self.joinKeyWrongValue = -1;
                    }
                    if (self.sinkType === 1 && (self.selectedS3Connector === null || !self.selectedS3Connector)) {
                        self.trainingDatasetSinkNotSelected = -1
                        self.sinkWrongValue = -1
                        self.trainingDatasetWrong_values = -1;
                    }
                    if (self.sinkType === 0 && (self.selectedHopsfsConnector === null || !self.selectedHopsfsConnector)) {
                        self.trainingDatasetSinkNotSelected = -1
                        self.sinkWrongValue = -1
                        self.trainingDatasetWrong_values = -1;
                    }
                }
            }

            /**
             * Check whether a feature already exists in the basket
             * @param feature
             * @returns {boolean} true if it exists, otherwise false
             */
            self.featureInBasket = function (feature) {
                for (var j = 0; j < self.featureBasket.length; j++) {
                    if (self.featureBasket[j] == feature) {
                        return true
                    }
                }
                return false
            }

            /**
             * Callback method for when the user filled in a training dataset description. Will then
             * display the type field
             * @returns {undefined}
             */
            self.descriptionFilledIn = function () {
                if (self.trainingDatasetOperation === 'CREATE') {
                    if (self.phase === 1) {
                        if (!self.trainingDatasetDoc) {
                            self.trainingDatasetDoc = "-";
                        }
                        self.phase = 2;
                        self.td_accordion3.visible = true;
                        self.td_accordion3.isOpen = false;
                        self.td_accordion4.visible = true;
                        self.td_accordion4.isOpen = true;
                        self.td_accordion5.visible = true;
                        self.td_accordion5.isOpen = false;
                    }
                    self.td_accordion2.value = " - " + self.trainingDatasetDoc; //Edit panel title
                }
            };

            /**
             * Callback method for when the user filled in a training dataset name. Will then
             * display the description field
             * @returns {undefined}
             */
            self.nameFilledIn = function () {
                if (self.trainingDatasetOperation === 'CREATE') {
                    if (self.phase === 0) {
                        if (!self.trainingDatasetName) {
                            self.trainingDatasetName = "TrainingDataset-" + Math.round(new Date().getTime() / 1000);
                        }
                        self.phase = 1;
                        self.td_accordion2.isOpen = true; //Open description selection
                        self.td_accordion2.visible = true; //Display description selection
                    }
                    self.td_accordion1.value = " - " + self.trainingDatasetName; //Edit panel title
                }
            };

            /**
             * Cart dropdown toggle
             */
            self.toggleCart = function () {
                self.showCart = !self.showCart;
            };

            /**
             * Removes a feature from the basket of selected features
             *
             * @param index the index of the feature to remove.
             */
            self.removeFeatureFromBasket = function (index) {
                self.featureBasket.splice(index, 1);
            }

            /*
             * Adds a feature to the basket
             *
             * @param feature the feature to add
             */
            self.addFeatureToBasket = function (feature) {
                if (self.featureInBasket(feature)) {
                    growl.info("Feature already selected", {title: 'Success', ttl: 1000});
                } else {
                    self.featureBasket.push(feature)
                    growl.success("Added Feature", {title: 'Success', ttl: 1000});
                }
            }

            /**
             * The list of feature groups of the features.
             *
             * @returns {Array}
             */
            self.basketFeaturegroups = function () {
                var featuregroups = []
                for (var j = 0; j < self.featureBasket.length; j++) {
                    if (!self.featuregroupsIncludes(featuregroups, self.featureBasket[j].featuregroup)) {
                        featuregroups.push(self.featureBasket[j].featuregroup)
                    }
                }
                return featuregroups
            }

            /**
             * Checks whether a featuregroup exists in a list of feature groups
             *
             * @param featuregroups the list of feature groups
             * @param fg the feature group to look for
             * @returns {boolean}
             */
            self.featuregroupsIncludes = function (featuregroups, fg) {
                for (var i = 0; i < featuregroups.length; i++) {
                    if (featuregroups[i] === fg || (featuregroups[i].name === fg.name && featuregroups[i].version === fg.version)) {
                        return true
                    }
                }
                return false
            }

            /**
             * Exit the "create new training dataset page" and go back to the featurestore page
             */
            self.exitToFeaturestore = function () {
                StorageService.store(self.projectId + "_featurestore_tab", 1);
                self.goToUrl("featurestore")
            };

            /**
             * Exit the "create new feature group query planning" and go back to the feature selection page
             */
            self.exitToFeatureSearch = function () {
                self.selectFeatureStage = true;
            };


            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            /**
             * Sets up the query plan for a new training dataset
             */
            self.setupQueryPlan = function () {
                if (self.featureBasket.length == 0) {
                    var title = 'You must select at least one feature to create a training dataset'
                    if (self.trainingDatasetOperation === 'UPDATE') {
                        title = 'You must select at least one feature to update the training dataset'
                    }
                    growl.error("", {
                        title: title,
                        ttl: 15000
                    });
                } else {
                    self.queryPlan.features = self.featureBasket
                    self.queryPlan.featuregroups = self.basketFeaturegroups()
                    self.inferJoinCol(self.queryPlan.featuregroups)
                    var featuregroupToFeatures = {}
                    for (var i = 0; i < self.queryPlan.features.length; i++) {
                        var key = self.queryPlan.features[i].featuregroup.name + "_" +
                            self.queryPlan.features[i].featuregroup.version
                        if (key in featuregroupToFeatures) {
                            featuregroupToFeatures[key].push(self.queryPlan.features[i])
                        } else {
                            featuregroupToFeatures[key] = [self.queryPlan.features[i]]
                        }
                    }
                    self.queryPlan.featuregroupToFeatures = featuregroupToFeatures
                    self.trainingDatasetNameWrongValue = 1
                    self.trainingDatasetDataFormatWrongValue = 1
                    self.trainingDatasetDocWrongValue = 1;
                    self.queryPlanWrongValue = 1;
                    self.joinKeyWrongValue = 1
                    self.trainingDatasetWrong_values = 1;
                    self.working = false;
                    if (self.queryPlan.possibleJoinKeys.length === 0) {
                        self.trainingDatasetWrong_values = -1;
                        self.queryPlanWrongValue = -1;
                        self.joinKeyWrongValue = -1;
                    }
                    self.selectFeatureStage = false;
                }
            };

            /**
             * Get a SQL string to display in the UI to select a bunch of features from a feature group
             *
             * @param featuregroup the feature group
             * @returns SELECT feature1,feature2,... FROM featuregroup_version
             */
            self.getSelectStr = function (featuregroup) {
                var features = self.queryPlan.featuregroupToFeatures[featuregroup.name + "_" + featuregroup.version]
                var featureNames = []
                for (var i = 0; i < features.length; i++) {
                    featureNames.push(features[i].name)
                }
                return "SELECT " + featureNames.join(",") + " FROM " + featuregroup.name + "_" + featuregroup.version
            }

            /**
             * Infer the join column for the query planner
             * Currently only supports single join column and not composite keys
             *
             * @param featuregroups
             */
            self.inferJoinCol = function (featuregroups) {
                var featureSets = new Set()
                var features = new Set()
                for (var j = 0; j < featuregroups.length; j++) {
                    var fSet = new Set()
                    for (var k = 0; k < featuregroups[j].features.length; k++) {
                        fSet.add(featuregroups[j].features[k].name)
                        features.add(featuregroups[j].features[k].name)
                    }
                    featureSets.add(fSet)
                }
                var intersection = []
                var featuresList = Array.from(features)
                var featureSetsList = Array.from(featureSets)
                for (var j = 0; j < featuresList.length; j++) {
                    var existInAll = true;
                    for (var k = 0; k < featureSetsList.length; k++) {
                        if (!featureSetsList[k].has(featuresList[j])) {
                            existInAll = false;
                        }
                    }
                    if (existInAll) {
                        intersection.push(featuresList[j])
                    }
                }
                self.queryPlan.joinKey = intersection[0]
                self.queryPlan.possibleJoinKeys = intersection
            }

            /**
             * Updates a training dataset
             */
            self.updateTrainingDatasetMetadata = function () {
                self.validateTrainingDatasetInput()
                if (self.trainingDatasetWrong_values === -1) {
                    self.working = false;
                    return;
                }
                var jobName = "update_training_dataset_" + self.trainingDatasetName + "_" + new Date().getTime()
                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "jobName": jobName,
                    "version": self.version,
                    "description": self.trainingDatasetDoc,
                    "dataFormat": self.trainingDatasetFormat,
                    "featureCorrelationMatrix": null,
                    "descriptiveStatistics": null,
                    "featuresHistogram": null,
                    "features": self.trainingDataset.features,
                    "updateMetadata": true,
                    "updateStats": false,
                    "trainingDatasetType": self.hopsfsTrainingDatasetType,
                    "type": self.hopsfsTrainingDatasetTypeDTO,
                    "hopsfsConnectorId": self.selectedHopsfsConnector.id,
                    "hopsfsConnectorName": self.selectedHopsfsConnector.name,
                    "jobs": []
                }
                FeaturestoreService.updateTrainingDatasetMetadata(self.projectId, self.trainingDataset.id,
                    trainingDatasetJson, self.featurestore).then(
                    function (success) {
                        self.working = false;
                        growl.success("Training dataset updated", {title: 'Success', ttl: 1000});
                        self.exitToFeaturestore()
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to update training dataset',
                            ttl: 15000
                        });
                        self.working = false;
                    });
                growl.info("Updating training dataset... wait", {title: 'Creating', ttl: 1000})
            }

            /**
             * Creates a training dataset
             */
            self.createTrainingDataset = function () {
                self.validateTrainingDatasetInput()
                if (self.trainingDatasetWrong_values === -1) {
                    self.working = false;
                    return;
                }
                var jobName = "create_training_dataset_" + self.trainingDatasetName + "_" + new Date().getTime()
                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "jobName": jobName,
                    "version": self.version,
                    "description": self.trainingDatasetDoc,
                    "dataFormat": self.trainingDatasetFormat,
                    "featureCorrelationMatrix": null,
                    "descriptiveStatistics": null,
                    "featuresHistogram": null,
                    "features": self.featureBasket,
                    "trainingDatasetType": self.hopsfsTrainingDatasetType,
                    "type": self.hopsfsTrainingDatasetTypeDTO,
                    "hopsfsConnectorId": self.selectedHopsfsConnector.id,
                    "hopsfsConnectorName": self.selectedHopsfsConnector.name,
                    "jobs": []
                }
                if(self.configureJob){
                    var utilArgs = self.setupJobArgs(jobName + "_args.json");
                    FeaturestoreService.writeUtilArgstoHdfs(self.projectId, utilArgs).then(
                        function (success) {
                            var hdfsPath = success.data.successMessage;
                            StorageService.store(self.projectId + "_" + jobName + "_fs_hdfs_path", hdfsPath);
                            var runConfig = self.setupHopsworksCreateTdJob(jobName, hdfsPath);
                            FeaturestoreService.createTrainingDataset(self.projectId, trainingDatasetJson, self.featurestore).then(
                                function (success) {
                                    self.working = false;
                                    growl.success("New training dataset created", {title: 'Success', ttl: 1000});
                                    var jobState = self.setupJobState(runConfig);
                                    StorageService.store(self.newJobName, jobState);
                                    self.goToUrl("newjob")
                                }, function (error) {
                                    growl.error(error.data.errorMsg, {
                                        title: 'Failed to create training dataset',
                                        ttl: 15000
                                    });
                                    self.working = false;
                                });
                            growl.info("Creating new training dataset... wait", {title: 'Creating', ttl: 1000})
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to setup featurestore util job arguments',
                                ttl: 15000
                            });
                            self.working = false;
                        });
                    growl.info("Settings up job arguments... wait", {title: 'Creating', ttl: 1000})
                } else {
                    FeaturestoreService.createTrainingDataset(self.projectId, trainingDatasetJson, self.featurestore).then(
                        function (success) {
                            self.working = false;
                            growl.success("New training dataset created", {title: 'Success', ttl: 1000});
                            self.exitToFeaturestore()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to create training dataset',
                                ttl: 15000
                            });
                            self.working = false;
                        });
                    growl.info("Creating new training dataset... wait", {title: 'Creating', ttl: 1000})
                }
            };

            /**
             * Setup jobState for redirecting to 'newjob' page
             *
             * @param runConfig the job runConfig
             * @returns the jobState
             */
            self.setupJobState = function (runConfig) {
                var jobState = {}
                jobState.accordion1 = {//Contains the job name
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job name - " + runConfig.appName
                };
                jobState.accordion2 = {//Contains the job type
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job type - " + runConfig.jobType
                };
                if (runConfig.jobType === self.sparkJobType) {
                    jobState.accordion3 = {// Contains the main execution file (jar, workflow,...)
                        "isOpen": false,
                        "visible": false,
                        "value": "",
                        "title": "App file (.jar, .py or .ipynb) - " + runConfig.appPath
                    };
                    jobState.jobtype = 1
                }
                if (runConfig.jobType === self.pySparkJobType) {
                    jobState.accordion3 = {// Contains the main execution file (jar, workflow,...)
                        "isOpen": false,
                        "visible": false,
                        "value": "",
                        "title": "App file (.py or .ipynb) - " + runConfig.appPath
                    };
                    jobState.jobtype = 2
                }
                jobState.accordion4 = {// Contains the job setup (main class, input variables,...)
                    "isOpen": false,
                    "visible": true,
                    "value": "",
                    "title": "Job details"
                };
                jobState.accordion5 = {//Contains the configuration and creation
                    "isOpen": true,
                    "visible": true,
                    "value": "",
                    "title": "Configure and create"
                };
                jobState.phase = 5
                jobState.jobname = runConfig.appName
                jobState.runConfig = runConfig
                jobState.sparkState = {
                    "selectedJar": runConfig.appPath
                }
                return jobState
            }

            /**
             * Utility function to get the name of the selected connector
             */
            self.getConnectorName = function() {
                if (self.sinkType === 0) {
                    return self.selectedHopsfsConnector.name;
                } else { 
                    return self.selectedS3Connector.name;
                }
            };

            /**
             * Sets up the JSON input arguments for a job to create a new training dataset using the Feature Store API
             * and Spark
             *
             * @param fileName name of the file to save the JSON
             * @returns the configured JSON
             */
            self.setupJobArgs = function (fileName) {
                return {
                    "operation": "create_td",
                    "featurestore": self.featurestore.featurestoreName,
                    "features": self.featureBasket,
                    "featuregroups": self.queryPlan.featuregroups,
                    "trainingDataset": self.trainingDatasetName,
                    "dataFormat": self.trainingDatasetFormat,
                    "version": 1,
                    "joinKey": self.queryPlan.joinKey,
                    "description": self.trainingDatasetDoc,
                    "fileName": fileName,
                    "descriptiveStats": false,
                    "featureCorrelation": false,
                    "clusterAnalysis": false,
                    "featureHistograms": false,
                    "statColumns": [],
                    "sink": self.getConnectorName(),
                    "path": self.path
                };
            };

            /**
             * Sets up the job configuration for creating a training dataset using Spark and the Featurestore API
             *
             * @param jobName name of the job
             * @param argsPath HDSF path to the input arguments
             * @returns the configured json
             */
            self.setupHopsworksCreateTdJob = function (jobName, argsPath) {
                var path = ""
                var mainClass = ""
                var jobType = ""
                if (self.trainingDatasetFormat === "petastorm" || self.trainingDatasetFormat === "npy") {
                    path = self.featurestoreUtilPythonExecutable
                    mainClass = self.settings.featurestoreUtilPythonMainClass
                    jobType = self.pySparkJobType
                } else {
                    path = self.featurestoreUtil4JExecutable
                    mainClass = self.settings.featurestoreUtil4jMainClass
                    jobType = self.sparkJobType
                }
                return {
                    type: "sparkJobConfiguration",
                    appName: jobName,
                    amQueue: "default",
                    amMemory: 4000,
                    amVCores: 1,
                    jobType: jobType,
                    appPath: path,
                    mainClass: mainClass,
                    "spark.blacklist.enabled": false,
                    "spark.dynamicAllocation.enabled": true,
                    "spark.dynamicAllocation.initialExecutors": 1,
                    "spark.dynamicAllocation.maxExecutors": 10,
                    "spark.dynamicAllocation.minExecutors": 1,
                    "spark.executor.cores": 1,
                    "spark.executor.gpus": 0,
                    "spark.executor.instances": 1,
                    "spark.executor.memory": 4000,
                    "spark.tensorflow.num.ps": 0
                };
            };

            /**
             * Update the sink type for the training dataset
             *
             * @param sinkType the new type
             */
            self.setSinkType = function (sinkType) {
                self.sinkType = sinkType
            };

            /**
             * Initialize controller
             */
            self.init = function () {
                self.initVariables()
            };

            /**
             * Boolean parameter indicating whether a spark job should be configured for creating the new training
             * dataset
             */
            self.setConfigureJob = function() {
                self.configureJob = !self.configureJob;
            };

            /**
             * Function called when the user press "add Feature" button in the update-training-dataset form
             */
            self.addNewFeature = function () {
                self.trainingDataset.features.push({
                    'name': '',
                    'type': '',
                    'description': "",
                    primary: false,
                    partition: false
                });
            };

            /**
             * Function called when the user press "delete Feature" button in the update-training-dataset form
             */
            self.removeFeature = function (index) {
                self.trainingDataset.features.splice(index, 1);
            };

            /**
             * Function called when the user clicks the "Feature type" button, opens up a modal where the user
             * can select a pre-defined Hive type or define a custom type.
             *
             * @param feature the feature to define the type for
             */
            self.selectFeatureType = function (feature) {
                ModalService.selectFeatureType('lg', false, self.settings).then(
                    function (success) {
                        feature.type = success
                    },
                    function (error) {
                        // Users changed their minds.
                    });
            };

            self.init();
        }
    ]);
