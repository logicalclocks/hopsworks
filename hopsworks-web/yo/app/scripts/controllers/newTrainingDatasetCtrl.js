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
    .controller('newTrainingDatasetCtrl', ['$routeParams', '$scope', 'growl',
        '$location', 'StorageService', 'FeaturestoreService',
        function ($routeParams, $scope, growl, $location, StorageService, FeaturestoreService) {
            var self = this;

            //Controller Input
            self.projectId = $routeParams.projectID;
            self.featurestore = StorageService.get(self.projectId + "_featurestore")
            self.projectName = StorageService.get("projectName");
            self.features = StorageService.get(self.projectId + "_fgFeatures");
            self.trainingDatasetOperation = StorageService.get("trainingdataset_operation");
            self.trainingDataset = StorageService.get(self.projectId + "_trainingDataset");
            self.version = StorageService.get(self.projectId + "_trainingDataset_version");
            self.storageConnectors = StorageService.get(self.projectId + "_storageconnectors")
            self.settings = StorageService.get(self.projectId + "_fssettings")

            //State
            self.showCart = true
            self.phase = 0
            self.selectFeatureStage = true;
            self.working = false

            self.s3Connectors = []
            self.hopsfsConnectors = []
            self.adlsConnectors = []
            self.selectedS3Connector = null
            self.selectedHopsfsConnector = null
            self.selectedADLSConnector= null
            self.sinkType = 0
            self.path = ""

            //Constants
            self.trainingDatasetNameMaxLength = self.settings.featurestoreEntityNameMaxLength
            self.trainingDatasetDescriptionMaxLength = self.settings.featurestoreEntityDescriptionMaxLength
            self.trainingDatasetNameRegexp = self.settings.featurestoreRegex
            self.dataFormats = self.settings.trainingDatasetDataFormats
            self.hopsfsTrainingDatasetType = self.settings.hopsfsTrainingDatasetType
            self.externalTrainingDatasetType = self.settings.externalTrainingDatasetType
            self.trainingDatasetType = self.settings.trainingDatasetType
            self.s3ConnectorType = self.settings.s3ConnectorType
            self.hopsfsConnectorType = self.settings.hopsfsConnectorType
            self.adlsConnectorType = "ADLS"

            //Input Variables
            self.trainingDatasetName = ""
            self.trainingDatasetDoc = ""
            self.trainingDatasetFormat = null;
            self.featureBasket = []

            self.query = undefined;

            /**
             * Input validation
             */
            self.trainingDatasetWrong_values = 1;
            //Name and Description Flags
            self.trainingDatasetNameWrongValue = 1;
            self.trainingDatasetDocWrongValue = 1;
            //Data format flags
            self.trainingDatasetDataFormatWrongValue = 1
            //Output sink flags
            self.sinkWrongValue = 1;
            self.trainingDatasetSinkNotSelected = 1

            //front-end variables
            self.td_accordion0 = {"isOpen": true, "value": "", "title": "Query Plan"};
            self.td_accordion1 = {"isOpen": true, "value": "", "title": "Training Dataset Name"};
            self.td_accordion2 = {"isOpen": false, "value": "", "title": "Training Dataset Description"};
            self.td_accordion3 = {"isOpen": true, "value": "", "title": "Training Dataset Format"};
            self.td_accordion4 = {"isOpen": true, "value": "", "title": "Create"};
            self.td_accordion5 = {"isOpen": true, "value": "", "title": "Output Location"};
            self.td_accordion6 = {"isOpen": false, "value": "", "title": "Schema"};

            /**
             * Perform initialization of variables that require it
             */
            self.initVariables = function () {
                self.trainingDatasetFormat = self.dataFormats[1]
                self.s3Connectors = []
                self.selectedS3Connector = null
                self.hopsfsConnectors = []
                self.selectedHopsfsConnector = null;

                self.adlsConnectors = []
                self.selectedADLSConnector = null;

                for (var i = 0; i < self.storageConnectors.length; i++) {
                    if (self.storageConnectors[i].storageConnectorType == self.s3ConnectorType) {
                        self.s3Connectors.push(self.storageConnectors[i])
                    } else if (self.storageConnectors[i].storageConnectorType == self.adlsConnectorType) {
                        self.adlsConnectors.push(self.storageConnectors[i])
                    } else if (self.storageConnectors[i].storageConnectorType == self.hopsfsConnectorType) {
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
                        self.td_accordion4.title = "Update"
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
                self.td_accordion2.value = " - " + self.trainingDatasetDoc; //Edit panel title
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
             * Updates a training dataset
             */
            self.updateTrainingDatasetMetadata = function () {
                self.validateTrainingDatasetInput()
                if (self.trainingDatasetWrong_values === -1) {
                    self.working = false;
                    return;
                }
                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "version": self.version,
                    "description": self.trainingDatasetDoc,
                    "dataFormat": self.trainingDatasetFormat,
                    "trainingDatasetType": self.getTrainingDatasetType(),
                    "statisticsConfig": {
                        "enabled": true,
                        "histograms": true,
                        "correlations": true,
                    },
                    "storageConnector": self.getStorageConnector(),
                    "type": "trainingDatasetDTO"
                }
                FeaturestoreService.updateTrainingDatasetMetadata(self.projectId, self.trainingDataset.id,
                    trainingDatasetJson, self.featurestore).then(
                    function (success) {
                        self.working = false;
                        growl.success("Training dataset updated", {title: 'Success', ttl: 1000});
                    }, function (error) {
                        self.exitToFeaturestore()
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
                var query = self.buildQueryObject();

                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "version": self.version,
                    "description": self.trainingDatasetDoc,
                    "dataFormat": self.trainingDatasetFormat,
                    "queryDTO": query,
                    "trainingDatasetType": self.getTrainingDatasetType(),
                    "statisticsConfig": {
                        "enabled": true,
                        "histograms": true,
                        "correlations": true,
                    },
                    "storageConnector": self.getStorageConnector(),
                    "type": "trainingDatasetDTO"
                }

                FeaturestoreService.createTrainingDataset(self.projectId, trainingDatasetJson, self.featurestore).then(
                    function(success) {
                        self.working = false;
                        growl.success("New training dataset created", {title: 'Success', ttl: 1000});
                        var trainingDatasetId = success.data.id;
                        var trainingDatasetJobConf = {
                            "query": query,
                            "overwrite": false,
                        };
                        FeaturestoreService.computeTrainingDataset(self.projectId, self.featurestore, trainingDatasetId, trainingDatasetJobConf).then(
                            function(success) {
                                growl.success("Job created successfully", {title: 'Success', ttl: 1000});
                                self.goToUrl("jobs");
                            },function(error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to create job to compute training dataset',
                                    ttl: 15000
                                });       
                            }
                        );
                    }, function(error) {
                        self.working = false;
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to create training dataset',
                            ttl: 15000
                        });
                    }
                )
            };

            self.buildQueryObject = function() {
                var featuresGrouped = {}

                var topQueryFg = undefined;
                var joinFgs = [];

                self.featureBasket.forEach(function(feature) {
                    if (feature.featuregroup.id in featuresGrouped) {
                        featuresGrouped[feature.featuregroup.id].push(feature);
                    } else {
                        featuresGrouped[feature.featuregroup.id] = [feature]
                        if (topQueryFg === undefined) {
                            topQueryFg = feature.featuregroup.id;
                        } else {
                            joinFgs.push(feature.featuregroup.id);
                        }
                    }
                });
                return {
                    'featureStoreName': self.featurestore.featurestoreName,
                    'featureStoreId': self.featurestore.featurestoreId,
                    'leftFeatureGroup': {'id': topQueryFg},
                    'leftFeatures': self.getLeftFeatureNames(featuresGrouped[topQueryFg]),
                    'joins': self.getJoins(joinFgs, featuresGrouped)
                }
            };

            self.getLeftFeatureNames = function(features) {
                var featureNames = [];
                features.forEach(function(feature){
                    featureNames.push({'name': feature.name});
                });

                return featureNames;
            };

            self.getJoins = function(joinFgs, featuresGrouped) {
                var joins = [];
                joinFgs.forEach(function(joinFg){
                    joins.push({'query': {
                        'leftFeatureGroup': {'id': joinFg},
                        'leftFeatures': self.getLeftFeatureNames(featuresGrouped[joinFg])
                    }});
                });
                return joins;
            };

            self.getTrainingDatasetType = function() {
                if (self.sinkType == 0) {
                    return "HOPSFS_TRAINING_DATASET";
                } else { 
                    return "EXTERNAL_TRAINING_DATASET";
                }
            };
            
            self.getStorageConnector = function() {
                if (self.sinkType == 0) {
                    return self.selectedHopsfsConnector;
                } else if (self.sinkType == 1) {
                    return self.selectedS3Connector;
                } else if (self.sinkType == 2) {
                    return self.selectedADLSConnector;
                }
            };

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

            self.toggleSelectFeatureStage = function() { 
                self.selectFeatureStage = !self.selectFeatureStage;
                var queryJson = self.buildQueryObject();
                FeaturestoreService.constructQuery(self.projectId, queryJson).then(
                    function(success) {
                        self.query = success.data.query;
                    }, function(error) {
                        self.query = undefined;
                    }
                );
            };

            /**
             * Remove entries from local storage
             */
            $scope.$on("$destroy", function() {
                StorageService.remove(self.projectId + "_fgFeatures");
                StorageService.remove("trainingdataset_operation");
                StorageService.remove(self.projectId + "_trainingDataset");
            });

            self.init();
        }
    ]);
