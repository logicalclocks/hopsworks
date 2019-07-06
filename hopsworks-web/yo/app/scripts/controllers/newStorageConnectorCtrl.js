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
 * Controller for managing the "create new storage connector" page
 */
angular.module('hopsWorksApp')
    .controller('newStorageConnectorCtrl', ['$routeParams', 'growl',
        '$location', 'StorageService', 'FeaturestoreService', 'DataSetService',
        function ($routeParams, growl, $location, StorageService, FeaturestoreService, DataSetService) {

            var self = this;

            //Controller Input
            self.projectId = $routeParams.projectID;
            self.featurestore = StorageService.get(self.projectId + "_featurestore")
            self.storageConnectors = StorageService.get(self.projectId + "_featurestore_storageconnectors")

            //Input Variables
            self.storageConnectorName = ""
            self.storageConnectorDescription = ""
            self.storageConnectorType = 0;
            self.jdbcConnectionString = "";
            self.jdbcArguments = []
            self.s3Bucket = ""
            self.s3SecretKey = ""
            self.s3AccessKey = ""

            //State
            self.working = false
            self.hopsFsDataset = null
            self.datasets = []
            self.phase = 0

            //Constants
            self.storageConnectorNameRegexp = FeaturestoreService.featurestoreRegExp()
            self.dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
            self.storageConnectorNameMaxLength = FeaturestoreService.storageConnectorNameMaxLength();
            self.storageConnectorDescriptionMaxLength = FeaturestoreService.storageConnectorDescriptionMaxLength();
            self.jdbcStorageConnectorConnectionStringMaxLength = FeaturestoreService.jdbcStorageConnectorConnectionStringMaxLength();
            self.jdbcStorageConnectorArgumentsMaxLength = FeaturestoreService.jdbcStorageConnectorArgumentsMaxLength();
            self.s3StorageConnectorBucketMaxLength = FeaturestoreService.s3StorageConnectorBucketMaxLength();
            self.s3StorageConnectorAccesskeyMaxLength = FeaturestoreService.s3StorageConnectorAccesskeyMaxLength();
            self.s3StorageConnectorSecretkeyMaxLength = FeaturestoreService.s3StorageConnectorSecretkeyMaxLength();
            self.hopsfsConnectorType = FeaturestoreService.hopsfsConnectorType()
            self.s3ConnectorType = FeaturestoreService.s3ConnectorType()
            self.jdbcConnectorType = FeaturestoreService.jdbcConnectorType()
            self.s3ConnectorDTOType = FeaturestoreService.s3ConnectorDTO()
            self.jdbcConnectorDTOType = FeaturestoreService.jdbcConnectorDTO()
            self.hopsfsConnectorDTOType = FeaturestoreService.hopsfsConnectorDTO()


            /**
             * Input validation
             */
            self.wrong_values = 1;
            //General validation
            self.storageConnectorNameWrongValue = 1
            self.storageConnectorNameNotUnique = 1
            self.storageConnectorDescriptionWrongValue = 1;
            self.storageConnectorNames = []
            self.storageConnectorConfigWrongValue = 1
            //JDBC validation
            self.storageConnectorJdbcStringWrongValue = 1
            self.jdbcArgumentsWrongValue = [];
            self.jdbcArgumentsNotUnique = 1;
            //HopsFS validation
            self.storageConnectorHopsfsDatasetWrongValue = 1

            //front-end variables
            self.accordion1 = {
                "isOpen": true,
                "visible": true,
                "value": "",
                "title": "Name"
            };
            self.accordion2 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Description"
            };
            self.accordion3 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Configure"
            };
            self.accordion4 = {
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Create"
            };

            /**
             * Validates user input for creating new storage connector
             */
            self.validateStorageConnectorInput = function () {
                self.storageConnectorNameWrongValue = 1
                self.storageConnectorS3BucketWrongValue = 1
                self.storageConnectorS3AccessKeyWrongValue = 1
                self.storageConnectorS3SecretKeyWrongValue = 1
                self.storageConnectorNameNotUnique = 1
                self.storageConnectorDescriptionWrongValue = 1;
                self.storageConnectorJdbcStringWrongValue = 1
                self.jdbcArgumentsNotUnique = 1
                self.storageConnectorConfigWrongValue = 1
                self.storageConnectorHopsfsDatasetWrongValue = 1
                self.wrong_values = 1;
                self.working = true;
                for (i = 0; i < self.jdbcArgumentsWrongValue.length; i++) {
                    self.jdbcArgumentsWrongValue[i] = 1
                }

                if (!self.storageConnectorName || self.storageConnectorName === "" ||
                    self.storageConnectorName.search(self.storageConnectorNameRegexp) == -1
                    || self.storageConnectorName.length > self.storageConnectorNameMaxLength) {
                    self.storageConnectorNameWrongValue = -1;
                    self.wrong_values = -1;
                } else {
                    self.storageConnectorNameWrongValue = 1;
                }

                if (!self.storageConnectorDescription || self.storageConnectorDescription == undefined) {
                    self.storageConnectorDescription = ""
                }
                if (self.storageConnectorDescription.length > self.storageConnectorDescriptionMaxLength) {
                    self.storageConnectorDescriptionWrongValue= -1;
                    self.wrong_values = -1;
                } else {
                    self.storageConnectorDescriptionWrongValue = 1;
                }
                var i;
                for (i = 0; i < self.storageConnectorNames.length; i++) {
                    if (self.storageConnectorName === self.storageConnectorNames[i]) {
                        self.storageConnectorNameNotUnique = -1;
                        self.wrong_values = -1;
                    }
                }

                /**
                 * Validate JDBC connector input
                 */
                if(self.storageConnectorType === 0) {
                    if(!self.jdbcConnectionString || self.jdbcConnectionString === "" ||
                        self.jdbcConnectionString === null ||
                        self.jdbcConnectionString > self.jdbcStorageConnectorConnectionStringMaxLength){
                        self.storageConnectorJdbcStringWrongValue = -1;
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue = -1
                    }

                    for (i = 0; i < self.jdbcArguments.length; i++) {
                        if (self.jdbcArguments[i] === "") {
                            self.jdbcArgumentsWrongValue[i] = -1
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                    }
                    if(self.jdbcArguments.length > 0) {
                        var hasDuplicates = (new Set(self.jdbcArguments)).size !== self.jdbcArguments.length;
                        if (hasDuplicates) {
                            self.jdbcArgumentsNotUnique = -1
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                    }
                }

                /**
                 * Validate S3 connector input
                 */
                if(self.storageConnectorType === 1) {
                    if(!self.s3Bucket || self.s3Bucket === "" || self.s3Bucket === null
                        || self.s3Bucket.length > self.s3StorageConnectorBucketMaxLength){
                        self.storageConnectorS3BucketWrongValue = -1;
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue = -1
                    }

                    if(self.s3AccessKey && self.s3AccessKey != null
                        && self.s3AccessKey.length > self.s3StorageConnectorAccesskeyMaxLength){
                        self.storageConnectorS3AccessKeyWrongValue = -1;
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue = -1
                    }

                    if(self.s3SecretKey && self.s3SecretKey != null
                        && self.s3SecretKey.length > self.s3StorageConnectorSecretkeyMaxLength) {
                        self.storageConnectorS3SecretKeyWrongValue = -1;
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue = -1
                    }
                }

                /**
                 * Validate HopsFS connector input
                 */
                if(self.storageConnectorType === 2) {
                    if(!self.hopsFsDataset.name || self.hopsFsDataset.name === null || self.hopsFsDataset.name === undefined){
                        self.storageConnectorHopsfsDatasetWrongValue = -1
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue
                    }
                }
            }


            /**
             * Callback method for when the user filled in the storage connector description. Will then
             * display the next field
             * @returns {undefined}
             */
            self.descriptionFilledIn = function () {
                if (self.phase === 1) {
                    if (!self.storageConnectorDescription) {
                        self.storageConnectorDescription = "-";
                    }
                    self.phase = 2;
                    self.accordion3.visible = true;
                    self.accordion3.isOpen = true;
                    self.accordion4.visible = true;
                    self.accordion4.isOpen = true;
                }
            };

            /**
             * Callback method for when the user filled in the storage connector name. Will then
             * display the description field
             * @returns {undefined}
             */
            self.nameFilledIn = function () {
                if (self.phase === 0) {
                    if (!self.storageConnectorName) {
                        self.storageConnectorName = "Connector-" + Math.round(new Date().getTime() / 1000);
                    }
                    self.phase = 1;
                    self.accordion2.isOpen = true; //Open description selection
                    self.accordion2.visible = true; //Display description selection
                }
                self.accordion1.value = " - " + self.storageConnectorName; //Edit panel title
            };

            /**
             * Exit the "create new training dataset page" and go back to the featurestore page
             */
            self.exitToFeaturestore = function () {
                StorageService.store(self.projectId + "_featurestore_tab", 4);
                $location.path('project/' + self.projectId + '/featurestore');
            };

            /**
             * Creates a storage connector
             */
            self.createStorageConnector = function () {
                self.validateStorageConnectorInput()
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                var storageConnectorJson = {
                    "name": self.storageConnectorName,
                    "description": self.storageConnectorDescription
                }
                if(self.storageConnectorType === 0){
                    storageConnectorJson["storageConnectorType"] = self.jdbcConnectorType
                    storageConnectorJson["type"] = self.jdbcConnectorDTOType
                    storageConnectorJson["arguments"] = self.jdbcArguments
                    storageConnectorJson["connectionString"] = self.jdbcConnectionString
                    FeaturestoreService.createStorageConnector(self.projectId, storageConnectorJson, self.featurestore,
                        self.jdbcConnectorType).then(
                        function (success) {
                            self.working = false;
                            growl.success("JDBC Storage Connector created", {title: 'Success', ttl: 1000});
                            self.exitToFeaturestore()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Failed to create storage connector', ttl: 15000});
                            self.working = false;
                        });
                }
                if(self.storageConnectorType === 1){
                    storageConnectorJson["type"] = self.s3ConnectorDTOType
                    storageConnectorJson["storageConnectorType"] = self.s3ConnectorType
                    storageConnectorJson["bucket"] = self.s3Bucket
                    storageConnectorJson["secretKey"] = self.s3SecretKey
                    storageConnectorJson["accessKey"] = self.s3AccessKey
                    FeaturestoreService.createStorageConnector(self.projectId, storageConnectorJson, self.featurestore,
                    self.s3ConnectorType).then(
                        function (success) {
                            self.working = false;
                            growl.success("S3 Storage Connector created", {title: 'Success', ttl: 1000});
                            self.exitToFeaturestore()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Failed to create storage connector', ttl: 15000});
                            self.working = false;
                        });
                }
                if(self.storageConnectorType === 2){
                    storageConnectorJson["type"] = self.hopsfsConnectorDTOType
                    storageConnectorJson["storageConnectorType"] = self.hopsfsConnectorType
                    storageConnectorJson["datasetName"] = self.hopsFsDataset.name
                    FeaturestoreService.createStorageConnector(self.projectId, storageConnectorJson, self.featurestore,
                        self.hopsfsConnectorType).then(
                        function (success) {
                            self.working = false;
                            growl.success("HopsFS Storage Connector created", {title: 'Success', ttl: 1000});
                            self.exitToFeaturestore()
                        }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Failed to create storage connector', ttl: 15000});
                            self.working = false;
                        });
                }
                growl.info("Creating Storage Connector... wait", {title: 'Creating', ttl: 1000})
            };

            /**
             * Group all storage connector names
             */
            self.getStorageConnectorNames = function () {
                self.storageConnectorNames = []
                for (var i = 0; i < self.storageConnectors.length; i++) {
                    self.storageConnectorNames.push(self.storageConnectors[i].name)
                }
            }

            /**
             * Update the storage connector type
             *
             * @param storageConnectorType the new type
             */
            self.setStorageConnectorType = function (storageConnectorType) {
                self.storageConnectorType = storageConnectorType
            }

            /**
             * Called when the user clicks the "remove argument" button in the UI
             *
             * @param index index of the argument to remove
             */
            self.removeJdbcArgument = function (index) {
                self.jdbcArguments.splice(index, 1);
                self.jdbcArgumentsWrongValue.splice(index, 1)
            };

            /**
             * Called when the user clicks the "add argument" button in the UI
             */
            self.addJdbcArgument = function () {
                self.jdbcArguments.push("");
                self.jdbcArgumentsWrongValue.push(1);
            };

            /*
             * Get all datasets under the current project.
             * @returns {undefined}
             */
            self.getAllDatasets = function () {
                //Get the path for an empty patharray: will get the datasets
                var path = ""
                self.dataSetService.getContents(path).then(
                    function (success) {
                        self.datasets = success.data;
                        if(self.datasets.length > 0){
                            self.hopsFsDataset = self.datasets[0];
                        }
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch datasets in project', ttl: 15000});
                    });
            };

            /**
             * Initialize controller
             */
            self.init = function () {
                self.getStorageConnectorNames()
                self.getAllDatasets()
            }

            self.init()
        }
    ]);
