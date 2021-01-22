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
    .controller('newStorageConnectorCtrl', ['$routeParams', 'growl', '$location', 'StorageService',
        'FeaturestoreService', 'DataSetService', 'UserService', 'ProjectService',
        function ($routeParams, growl, $location, StorageService, FeaturestoreService, DataSetService, UserService, ProjectService) {

            var self = this;

            //Controller Input
            self.projectId = $routeParams.projectID;
            self.featurestore = StorageService.get(self.projectId + "_featurestore");
            self.storageConnectors = StorageService.get(self.projectId + "_featurestore_storageconnectors");
            self.settings = StorageService.get(self.projectId + "_fssettings");
            self.storageConnectorOperation = StorageService.get("connector_operation");
            self.storageConnector = StorageService.get(self.projectId + "_connector");

            //Input Variables
            self.storageConnectorName = ""
            self.storageConnectorDescription = ""
            self.storageConnectorType = 0
            self.jdbcConnectionString = ""
            self.jdbcArguments = [];
            self.redshiftArguments = [];
            self.s3Bucket = undefined;
            self.s3SecretKey = undefined;
            self.s3AccessKey = undefined;
            self.s3ServerEncryptionAlgorithm = undefined;
            self.s3ServerEncryptionKey = undefined;
            self.s3BucketEncryption = false;
            self.s3BucketEncryptionRequiresKey = false;
            self.s3BucketSecretAndAccessKeys = false;
            self.s3BucketAuth = 0;
            self.s3AWSRole = undefined;
            self.jdbcConnectorType = undefined;

            self.redshiftClusterIdentifier = undefined;
            self.redshiftDatabaseDriver = undefined;
            self.redshiftDatabaseEndpoint = undefined;
            self.redshiftDatabaseName = undefined;
            self.redshiftDatabasePort = 5439;
            self.redshiftTableName = undefined;
            self.redshiftDatabaseUserName = undefined;
            self.redshiftDatabasePassword = undefined;
            self.redshiftIAMRole = undefined;

            self.generation = "2";
            self.directoryId = undefined;
            self.applicationId = undefined;
            self.serviceCredential = undefined;
            self.accountName = undefined;
            self.containerName = undefined;

            //State
            self.working = false;
            self.hopsFsDataset = undefined;
            self.datasets = [];
            self.phase = 0;

            //Constants
            self.dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.
            self.storageConnectorNameMaxLength = self.settings.storageConnectorNameMaxLength;
            self.storageConnectorDescriptionMaxLength = self.settings.storageConnectorDescriptionMaxLength;
            self.jdbcStorageConnectorConnectionStringMaxLength = self.settings.jdbcStorageConnectorConnectionstringMaxLength;
            self.jdbcStorageConnectorArgumentsMaxLength = self.settings.jdbcStorageConnectorArgumentsMaxLength;
            self.s3StorageConnectorBucketMaxLength = self.settings.s3StorageConnectorBucketMaxLength;
            self.s3StorageConnectorAccesskeyMaxLength = self.settings.s3StorageConnectorAccesskeyMaxLength;
            self.s3StorageConnectorSecretkeyMaxLength = self.settings.s3StorageConnectorSecretkeyMaxLength;
            self.s3ServerEncryptionAlgorithmMaxLength = self.settings.s3ServerEncryptionAlgorithmMaxLength;
            self.s3ServerEncryptionKeyMaxLength = self.settings.s3StorageServerEncryptionKeyMaxLength;
            self.s3ServerEncryptionAlgorithmAlgorithms = [];
            self.s3IAMRole = self.settings.s3IAMRole;
            self.hopsfsConnectorType = self.settings.hopsfsConnectorType;
            self.s3ConnectorType = self.settings.s3ConnectorType;
            self.jdbcConnectorType = self.settings.jdbcConnectorType;
            self.redshiftConnectorType = self.settings.redshiftConnectorType;
            self.adlsConnectorType = "ADLS";
            self.s3ConnectorDTOType = self.settings.s3ConnectorDtoType;
            self.jdbcConnectorDTOType = self.settings.jdbcConnectorDtoType;
            self.redshiftConnectorDTOType = self.settings.redshiftConnectorDtoType;
            self.hopsfsConnectorDTOType = self.settings.hopsfsConnectorDtoType;

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
            self.s3ServerEncryptionKeyWrongValue = 1;
            //JDBC validation
            self.storageConnectorJdbcStringWrongValue = 1
            self.jdbcArgumentsWrongValue = [];
            self.jdbcArgumentsNotUnique = 1;
            //HopsFS validation
            self.storageConnectorHopsfsDatasetWrongValue = 1;
            self.s3BucketAuthMethods = [{id: 0, name: 'Access/Secret keys'}, {id: 1, name: 'Temporary credentials'}];

            //front-end variables
            self.accordion1 = {
                "isOpen": true,
                "value": "",
                "title": "Name"
            };
            self.accordion2 = {
                "isOpen": false,
                "value": "",
                "title": "Description"
            };
            self.accordion3 = {
                "isOpen": false,
                "value": "",
                "title": "Configure"
            };
            self.accordion4 = {
                "isOpen": true,
                "value": "",
                "title": "Create"
            };
            self.cloudRoleMappings = undefined;
            var getCloudRoleMappings = function () {
                UserService.getRole(self.projectId).then(function (success) {
                    self.role = success.data.role;
                    ProjectService.getCloudRoleMappings({id: self.projectId}).$promise.then(
                        function (success) {
                            var items = typeof success.items !== 'undefined'? success.items : [];
                            self.cloudRoleMappings = items.filter(function(mapping) {
                                return mapping.projectRole === self.role || mapping.projectRole === 'ALL';
                            });
                            if (typeof self.cloudRoleMappings !== 'undefined' && self.cloudRoleMappings.length > 0 &&
                                self.storageConnectorOperation === "CREATE") {
                                self.s3BucketAuth = 1;
                            }
                    });
                });
            };

            /**
             * Perform initialization of variables that require it
             */
            self.initVariables = function () {
                self.s3ServerEncryptionAlgorithmAlgorithms = self.makeArrayOfEncryptionAlgorithms();
                var j = 0;
                if (self.storageConnectorOperation === "UPDATE") {
                    self.accordion4.title = "Update"
                    self.storageConnectorName = self.storageConnector.name
                    self.storageConnectorDescription = self.storageConnector.description
                    if (self.storageConnector.storageConnectorType === self.jdbcConnectorType) {
                        self.storageConnectorType = 0;
                        self.jdbcConnectionString = self.storageConnector.connectionString;
                        var args = self.storageConnector.arguments
                        args = args + ''
                        var argsList = args.split(",")
                        self.jdbcArguments = argsList
                    }
                    if (self.storageConnector.storageConnectorType === self.s3ConnectorType) {
                        self.storageConnectorType = 1
                        self.s3Bucket = self.storageConnector.bucket
                        self.s3BucketAuth = 0;

                        if(self.storageConnector.serverEncryptionAlgorithm){
                            self.s3ServerEncryptionAlgorithm = self.storageConnector.serverEncryptionAlgorithm
                        }
                        if(self.storageConnector.serverEncryptionKey){
                            self.s3ServerEncryptionKey = self.storageConnector.serverEncryptionKey
                        }
                        if(self.storageConnector.secretKey){
                            self.s3SecretKey = self.storageConnector.secretKey
                        }
                        if(self.storageConnector.accessKey){
                            self.s3AccessKey = self.storageConnector.accessKey
                        }
                        if (self.storageConnector.iamRole) {
                            self.s3AWSRole = self.storageConnector.iamRole;
                            self.s3BucketAuth = 1;
                        }

                        if(self.storageConnector.serverEncryptionAlgorithm){
                            self.s3BucketEncryption = true;
                            var providedEncryptionAlgorithm =
                                self.findEcryptionAlgorithmFromArray(self.storageConnector.serverEncryptionAlgorithm);
                            if(providedEncryptionAlgorithm && providedEncryptionAlgorithm.requiresKey){
                                self.s3BucketEncryptionRequiresKey = true;
                            }
                        }
                    }
                    if (self.storageConnector.storageConnectorType === self.hopsfsConnectorType) {
                        self.storageConnectorType = 2
                        for (var i = 0; i < self.datasets.length; i++) {
                            if (self.datasets[i].name === self.storageConnector.datasetName) {
                                j = i;
                            }
                        }
                    }

                    if (self.storageConnector.storageConnectorType === self.redshiftConnectorType) {
                        self.storageConnectorType = 3
                        var args = self.storageConnector.arguments
                        args = args + ''
                        self.redshiftArguments = args.split(";");
                        self.redshiftClusterIdentifier = self.storageConnector.clusterIdentifier;
                        self.redshiftDatabaseDriver = self.storageConnector.databaseDriver;
                        self.redshiftDatabaseEndpoint = self.storageConnector.databaseEndpoint;
                        self.redshiftDatabaseName = self.storageConnector.databaseName;
                        self.redshiftDatabaseGroup = self.storageConnector.databaseGroup;
                        self.redshiftAutoCreate = self.storageConnector.autoCreate;
                        self.redshiftDatabasePort = self.storageConnector.databasePort;
                        self.redshiftDatabaseUserName = self.storageConnector.databaseUserName;
                        self.redshiftDatabasePassword = self.storageConnector.databasePassword;
                        self.redshiftIAMRole = self.storageConnector.iamRole;
                    }

                    if (self.storageConnector.storageConnectorType === self.adlsConnectorType) {
                        self.storageConnectorType = 4
                        self.generation = self.storageConnector.generation.toString();
                        self.directoryId = self.storageConnector.directoryId;
                        self.applicationId = self.storageConnector.applicationId;
                        self.serviceCredential = self.storageConnector.serviceCredential;
                        self.accountName = self.storageConnector.accountName;
                        self.containerName = self.storageConnector.containerName;
                    }
                }

                if (self.datasets.length > 0) {
                    self.hopsFsDataset = self.datasets[j];
                }
            }

            /**
             * Validates user input for creating new storage connector
             */
            self.validateStorageConnectorInput = function () {
                self.storageConnectorNameWrongValue = 1
                self.storageConnectorS3BucketWrongValue = 1
                self.storageConnectorS3AccessKeyWrongValue = 1
                self.storageConnectorS3SecretKeyWrongValue = 1
                self.storageConnectorS3ServerEncryptionAlgorithmWrongValue = 1
                self.storageConnectorS3ServerEncryptionKeyWrongValue = 1
                self.storageConnectorNameNotUnique = 1
                self.storageConnectorDescriptionWrongValue = 1;
                self.storageConnectorJdbcStringWrongValue = 1;
                self.jdbcArgumentsNotUnique = 1;
                self.storageConnectorConfigWrongValue = 1;
                self.storageConnectorHopsfsDatasetWrongValue = 1;
                self.redshiftArgumentsNotUnique = 1;
                self.s3AWSRoleWrongValue = 1;
                self.wrong_values = 1;
                self.working = true;
                for (i = 0; i < self.jdbcArgumentsWrongValue.length; i++) {
                    self.jdbcArgumentsWrongValue[i] = 1
                }

                if (!self.storageConnectorName || self.storageConnectorName === ""
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
                    self.storageConnectorDescriptionWrongValue = -1;
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
                if (self.storageConnectorType === 0) {
                    if (!self.jdbcConnectionString || self.jdbcConnectionString === "" ||
                        self.jdbcConnectionString === null ||
                        self.jdbcConnectionString > self.jdbcStorageConnectorConnectionStringMaxLength) {
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
                    if (self.jdbcArguments.length > 0) {
                        var hasDuplicates = (new Set(self.jdbcArguments)).size !== self.jdbcArguments.length;
                        if (hasDuplicates) {
                            self.jdbcArgumentsNotUnique = -1
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                    }
                }

                /**
                 * Redshift
                 */
                if (self.storageConnectorType === 3) {
                    for (var i = 0; i < self.redshiftArguments.length; i++) {
                        if (self.redshiftArguments[i] === '') {
                            self.redshiftArguments.splice(i, 1);
                        }
                    }
                    if (self.redshiftArguments.length > 0) {
                        var hasDuplicates = (new Set(self.redshiftArguments)).size !== self.redshiftArguments.length;
                        if (hasDuplicates) {
                            self.redshiftArgumentsNotUnique = -1;
                            self.wrong_values = -1;
                        }
                    }
                }

                /**
                 * Validate S3 connector input
                 */
                if (self.storageConnectorType === 1) {
                    if (!self.s3Bucket || self.s3Bucket === "" || self.s3Bucket === undefined
                        || self.s3Bucket.length > self.s3StorageConnectorBucketMaxLength) {
                        self.storageConnectorS3BucketWrongValue = -1;
                        self.wrong_values = -1;
                        self.storageConnectorConfigWrongValue = -1
                    }
                    if (self.s3BucketAuth == 0) {
                        if (!self.s3IAMRole && (self.s3AccessKey === "" || self.s3AccessKey === undefined
                            || self.s3AccessKey.length > self.s3StorageConnectorAccesskeyMaxLength)) {
                            self.storageConnectorS3AccessKeyWrongValue = -1;
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                        if (!self.s3IAMRole && (self.s3SecretKey === "" || self.s3SecretKey === undefined
                            || self.s3SecretKey.length > self.s3StorageConnectorSecretkeyMaxLength)) {
                            self.storageConnectorS3SecretKeyWrongValue = -1;
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                        if (self.s3BucketEncryption && self.s3ServerEncryptionAlgorithm === undefined) {
                            self.storageConnectorS3ServerEncryptionAlgorithmWrongValue = -1;
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                        if (self.s3BucketEncryption && self.s3BucketEncryptionRequiresKey
                            && (self.s3ServerEncryptionKey === "" || self.s3ServerEncryptionKey === undefined
                                || self.s3ServerEncryptionKey.length > self.s3ServerEncryptionKeyMaxLength)) {
                            self.storageConnectorS3ServerEncryptionKeyWrongValue = -1;
                            self.wrong_values = -1;
                            self.storageConnectorConfigWrongValue = -1
                        }
                    } else {
                        if (typeof self.s3AWSRole === "undefined") {
                            self.wrong_values = -1;
                            self.s3AWSRoleWrongValue = -1
                        }
                    }
                }

                /**
                 * Validate HopsFS connector input
                 */
                if (self.storageConnectorType === 2) {
                    if (!self.hopsFsDataset.name || self.hopsFsDataset.name === null || self.hopsFsDataset.name === undefined) {
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
                if (self.storageConnectorOperation === "CREATE") {
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
                }
            };

            /**
             * Callback method for when the user filled in the storage connector name. Will then
             * display the description field
             * @returns {undefined}
             */
            self.nameFilledIn = function () {
                if (self.storageConnectorOperation === "CREATE") {
                    if (self.phase === 0) {
                        if (!self.storageConnectorName) {
                            self.storageConnectorName = "Connector-" + Math.round(new Date().getTime() / 1000);
                        }
                        self.phase = 1;
                        self.accordion2.isOpen = true; //Open description selection
                        self.accordion2.visible = true; //Display description selection
                    }
                    self.accordion1.value = " - " + self.storageConnectorName; //Edit panel title
                }
            };

            /**
             * Exit the "create new training dataset page" and go back to the featurestore page
             */
            self.exitToFeaturestore = function () {
                StorageService.store(self.projectId + "_featurestore_tab", 4);
                $location.path('project/' + self.projectId + '/featurestore');
            };

            var showError = function (error, msg, ttl) {
                var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : msg;
                growl.error(errorMsg, {title: error.data.errorMsg, ttl: ttl});
            };

            var createStorageConnector = function (type, storageConnectorJson) {
                FeaturestoreService.createStorageConnector(self.projectId, storageConnectorJson, self.featurestore)
                .then(
                    function (success) {
                        self.working = false;
                        growl.success(type + " Storage Connector created", { title: 'Success', ttl: 1000 });
                        self.exitToFeaturestore()
                    }, function (error) {
                        showError(error, 'Failed to create storage connector', 15000);
                        self.working = false;
                    });
            };

            var updateStorageConnector = function (type, storageConnectorJson) {
                FeaturestoreService.updateStorageConnector(self.projectId, storageConnectorJson, self.featurestore,
                    self.storageConnector.name).then(
                    function (success) {
                        self.working = false;
                        growl.success(type + " Storage Connector updated", { title: 'Success', ttl: 1000 });
                        self.exitToFeaturestore()
                    }, function (error) {
                        showError(error, 'Failed to update storage connector', 15000);
                        self.working = false;
                    });
            };

            var createStorageConnectorJson = function () {
                return {"name": self.storageConnectorName,
                        "description": self.storageConnectorDescription}
            }

            var getJdbcStorageConnector = function () {
                var storageConnectorJson = createStorageConnectorJson();
                storageConnectorJson["storageConnectorType"] = self.jdbcConnectorType;
                storageConnectorJson["type"] = self.jdbcConnectorDTOType;
                storageConnectorJson["arguments"] = self.jdbcArguments.join(",");
                storageConnectorJson["connectionString"] = self.jdbcConnectionString;
                return storageConnectorJson;
            };

            var getS3StorageConnector = function () {
                var storageConnectorJson = createStorageConnectorJson();
                self.resetAccessAndEncryptionProperties()
                storageConnectorJson["type"] = self.s3ConnectorDTOType;
                storageConnectorJson["storageConnectorType"] = self.s3ConnectorType;
                storageConnectorJson["bucket"] = self.s3Bucket;
                if (self.s3BucketEncryption) {
                    storageConnectorJson["serverEncryptionAlgorithm"] = self.s3ServerEncryptionAlgorithm;
                    storageConnectorJson["serverEncryptionKey"] = self.s3ServerEncryptionKey;
                }
                if (self.s3BucketAuth == 0) {
                    storageConnectorJson["secretKey"] = self.s3SecretKey;
                    storageConnectorJson["accessKey"] = self.s3AccessKey;
                } else {
                    storageConnectorJson["iamRole"] = self.s3AWSRole;
                }
                return storageConnectorJson;
            };

            var getRedshiftStorageConnector = function () {
                var storageConnectorJson = createStorageConnectorJson();
                storageConnectorJson["storageConnectorType"] = self.redshiftConnectorType;
                storageConnectorJson["type"] = self.redshiftConnectorDTOType;
                storageConnectorJson["arguments"] = self.redshiftArguments.join(";");
                storageConnectorJson["clusterIdentifier"] = self.redshiftClusterIdentifier;
                storageConnectorJson["databaseDriver"] = self.redshiftDatabaseDriver;
                storageConnectorJson["databaseEndpoint"] = self.redshiftDatabaseEndpoint;
                storageConnectorJson["databaseName"] = self.redshiftDatabaseName;
                storageConnectorJson["databaseGroup"] = self.redshiftDatabaseGroup;
                storageConnectorJson["autoCreate"] = self.redshiftAutoCreate;
                storageConnectorJson["databasePort"] = self.redshiftDatabasePort;
                storageConnectorJson["tableName"] = self.redshiftTableName;
                storageConnectorJson["databaseUserName"] = self.redshiftDatabaseUserName;
                storageConnectorJson["databasePassword"] = self.redshiftDatabasePassword;
                storageConnectorJson["iamRole"] = self.redshiftIAMRole;
                return storageConnectorJson;
            };

            var getHdfsStorageConnector = function () {
                var storageConnectorJson = createStorageConnectorJson();
                storageConnectorJson["type"] = self.hopsfsConnectorDTOType;
                storageConnectorJson["storageConnectorType"] = self.hopsfsConnectorType;
                storageConnectorJson["datasetName"] = self.hopsFsDataset.name;
                return storageConnectorJson;
            };

            var getADLSConnector = function() {
                var storageConnectorJson = createStorageConnectorJson();
                storageConnectorJson["type"] = "featurestoreADLSConnectorDTO";
                storageConnectorJson["storageConnectorType"] = "ADLS";
                storageConnectorJson["generation"] = parseInt(self.generation);
                storageConnectorJson["directoryId"] = self.directoryId;
                storageConnectorJson["applicationId"] = self.applicationId;
                storageConnectorJson["serviceCredential"] = self.serviceCredential;
                storageConnectorJson["accountName"] = self.accountName;
                if (self.generation === "2") {
                    storageConnectorJson["containerName"] = self.containerName;
                }
                return storageConnectorJson;
            }

            /**
             * Updates an existing storage connector
             */
            self.updateStorageConnector = function () {
                self.validateStorageConnectorInput()
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                if (self.storageConnectorType === 0) {
                    updateStorageConnector(self.jdbcConnectorType, getJdbcStorageConnector());
                }
                if (self.storageConnectorType === 1) {
                    updateStorageConnector(self.s3ConnectorType, getS3StorageConnector());
                }
                if (self.storageConnectorType === 2) {
                    updateStorageConnector(self.hopsfsConnectorType, getHdfsStorageConnector());
                }
                if (self.storageConnectorType === 4) {
                    updateStorageConnector("ADLS", getADLSConnector());
                }
                growl.info("Updating Storage Connector... wait", { title: 'Updating..', ttl: 1000 })
            }

            /**
             * Creates a storage connector
             */
            self.createStorageConnector = function () {
                self.validateStorageConnectorInput()
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                if (self.storageConnectorType === 0) {
                    createStorageConnector(self.jdbcConnectorType, getJdbcStorageConnector());
                }
                if (self.storageConnectorType === 1) {
                    createStorageConnector(self.s3ConnectorType, getS3StorageConnector());
                }
                if (self.storageConnectorType === 2) {
                    createStorageConnector(self.hopsfsConnectorType, getHdfsStorageConnector());
                }
                if (self.storageConnectorType === 4) {
                    createStorageConnector("ADLS", getADLSConnector());
                }
                growl.info("Creating Storage Connector... wait", { title: 'Creating', ttl: 1000 })
            };

            self.submitRedshiftConnector = function () {
                self.working = true;
                self.validateStorageConnectorInput();
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                var storageConnectorJson = getRedshiftStorageConnector();
                if (self.storageConnectorOperation === 'CREATE') {
                    createStorageConnector(self.redshiftConnectorType, storageConnectorJson);
                } else if (self.storageConnectorOperation === 'UPDATE') {
                    updateStorageConnector(self.redshiftConnectorType, storageConnectorJson);
                }
                growl.info("Creating Storage Connector... wait", { title: 'Creating', ttl: 1000 })
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
                self.dataSetService.getAllDatasets("").then(
                    function (success) {
                        self.datasets = success.data.items;
                        self.initVariables()
                    }, function (error) {
                        showError(error, 'Failed to fetch datasets in project', 15000);
                    });
            };

            /**
             * Initialize controller
             */
            self.init = function () {
                self.getStorageConnectorNames();
                getCloudRoleMappings();
                self.getAllDatasets();
            }

            /**
             * Called when the user clicks on the "S3 Bucket Encryption checkbox"
             */
            self.toggleS3BucketEncryptionInputFields = function () {
                self.s3BucketEncryption = !self.s3BucketEncryption;
            }

            /**
             * Called when the user clicks on the "S3 Bucket Encryption Checkbox"
             */
            self.toggleS3BucketEncryptionKeyInputField = function (requiresKey) {
                if(requiresKey){
                    self.s3BucketEncryptionRequiresKey = true;
                }
                else{
                    self.s3BucketEncryptionRequiresKey = false;
                }
            }

            /**
             * Resets the values of access key, secrete key, encryption algorithm and encryption key
             */
            self.resetAccessAndEncryptionProperties = function () {
                if(self.s3IAMRole){
                    self.s3SecretKey = ""
                    self.s3AccessKey = ""
                }

                if(!self.s3BucketEncryption){
                    self.s3ServerEncryptionAlgorithm = ""
                    self.s3ServerEncryptionKey = ""
                }

                if(!self.s3BucketEncryptionRequiresKey){
                    self.s3ServerEncryptionKey = ""
                }
            }

            /**
             * Filter an encryption algorithm from array
             * @param algorithm
             * @returns {*}
             */
            self.findEcryptionAlgorithmFromArray = function (algorithm) {
                return self.s3ServerEncryptionAlgorithmAlgorithms.find(function (a) {
                    return a.algorithm == algorithm
                })
            }

            /**
             * Convert json strings of encryption algorithms into json objects
             * @returns {*}
             */
            self.makeArrayOfEncryptionAlgorithms = function () {
                return self.settings.s3ServerEncryptionAlgorithms.map( function (a) {
                    var algorithm = JSON.parse(a);
                    algorithm.value = algorithm.algorithm;
                    return algorithm;
                })
            }

            self.init()
        }
    ]);