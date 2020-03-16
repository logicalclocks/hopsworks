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
 * Controller for the featurestore page.
 */
'use strict';

angular.module('hopsWorksApp')
    .controller('featurestoreCtrl', ['$scope', '$routeParams', 'growl', 'FeaturestoreService', '$location', 'ModalService', 'TourService', 'ProjectService', 'StorageService', 'JobService',
        function ($scope, $routeParams, growl, FeaturestoreService, $location, ModalService,
                  TourService, ProjectService, StorageService, JobService) {

            /**
             * Initialize controller state
             */
            var self = this;
            //Controller inputs
            self.projectId = $routeParams.projectID;

            //State
            self.projectFeaturestoreName = null;
            self.projectTrainingdatasetsName = null;
            self.featurestores = [];
            self.trainingDatasets = [];
            self.storageConnectors = [];
            self.settings = null;
            self.featuregroups = [];
            self.featuregroupsPageSize = 20;
            self.trainingDatasetsPageSize = 20;
            self.storageConnectorsPageSize = 20;
            self.currentPage = 1;
            self.featurestore = null;
            self.featureSearchTerm = "";
            self.featuregroupsSortKey = 'name';
            self.trainingDatasetsSortKey = 'name';
            self.featuresSortKey = 'name';
            self.storageConnectorsSortKey = 'name';
            self.featuregroupsReverse = false;
            self.trainingDatasetsReverse = false;
            self.featuresReverse = false;
            self.storageConnectorsReverse = false;
            self.tdFilter = "";
            self.fgFilter = "";
            self.scFilter = "";
            self.featuregroupsDictList = [];
            self.trainingDatasetsDictList = [];
            self.loading = false;
            self.loadingText = "";
            self.featuregroupsLoaded = false;
            self.trainingDatasetsLoaded = false;
            self.storageConnectorsLoaded = false;
            self.settingsLoaded = false;
            self.quotaLoaded = false;
            self.tourService = TourService;
            self.tourService.currentStep_TourNine = 0; //Feature store tour
            self.featurestoreSizeWorking = false
            self.featurestoreSize = "Not fetched"
            self.featuregroupSizeWorking = false
            self.featuregroupSize = "Not fetched"
            self.date = new Date()
            self.quotaChartOptions = null;
            self.quotaChart = null;
            self.quotas = null;
            self.featureSearchFilterForm = false;
            self.featuregroupsToDate = null; 
            self.featuregroupsFromDate = null; 
            self.trainingDatasetsToDate = null;
            self.trainingDatasetsFromDate = null;
            self.searchInFeaturegroups = true
            self.searchInTrainingDatasets = false
            self.featureSearchFgFilter = ""
            self.featureSearchFgVersionFilter = ""
            self.fgFeatures = [];

            //Constants
            self.hopsfsConnectorType = ""
            self.s3ConnectorType = ""
            self.jdbcConnectorType = ""
            self.hopsfsTrainingDatasetType = ""
            self.externalTrainingDatasetType = ""
            self.featuregroupType = ""
            self.trainingDatasetType = ""


            /**
             * Boolean parameter in the feature search that indicates whether features inside feature groups should
             * be included
             */
            self.setSearchInFeaturegroups = function() {
                self.searchInFeaturegroups = !self.searchInFeaturegroups;
            }


            /**
             * Boolean parameter in the feature search that indicates whether features inside training datasets should
             * be included
             */
            self.setSearchInTrainingDatasets = function() {
                self.searchInTrainingDatasets = !self.searchInTrainingDatasets 
            }


            /**
             * Gets the name of the project using the id
             */
            self.getProjectName = function () {
                ProjectService.get({}, {'id': self.projectId}).$promise.then(
                    function (success) {
                        self.projectName = success.projectName;
                        self.projectFeaturestoreName = self.projectName.toLowerCase() + "_featurestore";
                        self.projectTrainingdatasetsName = self.projectName + "_Training_Datasets";
                        StorageService.store("projectName", self.projectName)
                        self.selectProjectFeaturestore()
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the name of the project',
                            ttl: 15000
                        });
                    });
            };

            /**
             * Called when clicking the sort-arrow in the UI of storage connectors table
             *
             * @param keyname
             */
            self.storageConnectorsSort = function (keyname) {
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.storageConnectorsSortKey = keyname
                self.storageConnectorsReverse = !self.storageConnectorsReverse; //if true make it false and vice versa
            };

            /**
             * Called when clicking the sort-arrow in the UI of featuregroup table
             *
             * @param keyname
             */
            self.featuregroupsSort = function (keyname) {
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.featuregroupsSortKey = keyname
                self.featuregroupsReverse = !self.featuregroupsReverse; //if true make it false and vice versa
            };

            /**
             * Called when clicking the sort-arrow in the UI of training datasets table
             *
             * @param keyname
             */
            self.trainingDatasetsSort = function (keyname) {
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.trainingDatasetsSortKey = keyname
                self.trainingDatasetsReverse = !self.trainingDatasetsReverse; //if true make it false and vice versa
            };

            /**
             * Called when clicking the sort-arrow in the UI of features table
             *
             * @param keyname
             */
            self.sortFeature = function (keyname) {
                self.sortKey = keyname;   //set the sortKey to the param passed
                self.featuresSortKey = keyname;
                self.featuresReverse = !self.featuresReverse; //if true make it false and vice versa
            };

            /**
             * Function to start the loading screen
             *
             * @param label the text to show to the user while loading
             */
            self.startLoading = function (label) {
                self.loading = true;
                self.loadingText = label;
            };

            /**
             * Callback when the user switched to the 'overview' tab
             */
            self.overviewTab = function () {
                self.renderQuotaChart()
            };


            /**
             * Function to get the current index in the paginated features table
             *
             * @param pageIndex the index in the current page
             */
            self.getTotalIndex = function (pageIndex) {
                return ((self.currentPage - 1) * self.featuresPageSize) + pageIndex + 1
            };

            /**
             * Function to stop the loading screen
             */
            self.stopLoading = function () {
                if(self.featuregroupsLoaded) {
                    self.collectAllFeatures();
                }
                if(self.storageConnectorsLoaded && self.settingsLoaded) {
                    self.setupStorageConnectors()
                }
                if (self.featuregroupsLoaded && self.trainingDatasetsLoaded && self.quotaLoaded
                    && self.storageConnectorsLoaded && self.settingsLoaded) {
                    var tabIndex = StorageService.get(self.projectId + "_featurestore_tab")
                    if(tabIndex != null && tabIndex != undefined && tabIndex != false){
                        self.goToTab(tabIndex)
                    }
                    StorageService.store(self.projectId + "_featurestore_tab", 0);
                    self.loading = false;
                    self.loadingText = "";
                }
            };

            /**
             * Shows the Modal for creating new feature groups through the UI
             */
            self.showCreateFeaturegroupForm = function () {
                StorageService.store("featuregroup_operation", "CREATE");
                self.goToUrl("newfeaturegroup")
            };

            /**
             * Opens the modal with a form for creating a new storage connector
             */
            self.showAddStorageConnectorForm = function () {
                StorageService.store("connector_operation", "CREATE");
                self.goToUrl("newstorageconnector")
            }

            /**
             * Convert storage connectors into a more human-readable list to display in the UI
             */
            self.setupStorageConnectors = function() {
                for (var i = 0; i < self.storageConnectors.length; i++) {
                    self.storageConnectors[i].canDelete = self.canNotDeleteStorageConnector(
                        self.storageConnectors[i].name)
                    if(self.storageConnectors[i].storageConnectorType === self.jdbcConnectorType){
                        self.storageConnectors[i].info = "Connection String: <code>" +
                            self.storageConnectors[i].connectionString +
                            "</code> | Additional arguments: <code>" +
                            self.storageConnectors[i].arguments + "</code>"
                    }

                    if(self.storageConnectors[i].storageConnectorType === self.s3ConnectorType){
                        self.storageConnectors[i].info = "S3 Bucket: <code>" +
                            self.storageConnectors[i].bucket
                            + "</code> | accessKey: <code>" + self.storageConnectors[i].accessKey
                            + "</code> | secretKey: <code>" + self.storageConnectors[i].secretKey +
                            "</code>"
                    }

                    if(self.storageConnectors[i].storageConnectorType === self.hopsfsConnectorType){
                        self.storageConnectors[i].info = "HopsFs Path: <code>" +
                            self.storageConnectors[i].hopsfsPath
                            + "</code>"
                    }
                }
            }

            /**
             * Delete a storage connector
             *
             * @param connector the connector to delete
             */
            self.deleteStorageConnector = function (connector) {
                FeaturestoreService.deleteStorageConnector(self.projectId, self.featurestore, connector.id,
                    connector.storageConnectorType).then(
                    function (success) {
                        self.getStorageConnectors(self.featurestore);
                        growl.success("Storage connector deleted", {title: 'Success', ttl: 1000});
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to delete the storage connector',
                            ttl: 15000
                        });
                    });
                growl.info("Deleting storage connector... wait", {title: 'Deleting', ttl: 1000})
            }

            /**
             * Go to the page for creating a new training dataset
             */
            self.showCreateTrainingDatasetForm = function () {
                StorageService.store("trainingdataset_operation", "CREATE");
                StorageService.store(self.projectId + "_fgFeatures", self.fgFeatures);
                self.goToUrl("newtrainingdataset")
            };

            /**
             * Retrieves a list of all featurestores for the project from the backend
             */
            self.getFeaturestores = function () {
                FeaturestoreService.getFeaturestores(self.projectId).then(
                    function (success) {
                        self.featurestores = success.data;
                        if(self.featurestore === null || self.featurestore === 'undefined'){
                            self.selectProjectFeaturestore();
                        } else {
                            self.selectFeaturestore(self.featurestore.featurestoreName);
                        }
                    },
                    function (error) {
                        growl.error(error, {
                            title: 'Failed to fetch list of featurestores',
                            ttl: 15000
                        });
                    }
                );
            };


            /**
             * Retrieves a list of all storage connectors for a given featurestore
             *
             * @param featurestore the featurestore to query
             */
            self.getFeaturestoreSettings = function() {
                FeaturestoreService.getFeaturestoreSettings(self.projectId).then(
                    function (success) {
                        self.settings = success.data
                        self.hopsfsConnectorType = self.settings.hopsfsConnectorType
                        self.s3ConnectorType = self.settings.s3ConnectorType
                        self.jdbcConnectorType = self.settings.jdbcConnectorType
                        self.featuregroupType = self.settings.featuregroupType
                        self.trainingDatasetType = self.settings.trainingDatasetType
                        self.onDemandFeaturegroupType = self.settings.onDemandFeaturegroupType
                        self.cachedFeaturegroupType = self.settings.cachedFeaturegroupType
                        self.hopsfsTrainingDatasetType = self.settings.hopsfsTrainingDatasetType
                        self.externalTrainingDatasetType = self.settings.externalTrainingDatasetType
                        StorageService.store(self.projectId + "_fssettings", success.data);
                        self.settingsLoaded = true
                        self.stopLoading()
                    },
                    function (error) {
                        self.settingsLoaded = true
                        self.stopLoading()
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the feature store settings',
                            ttl: 15000
                        });
                    });
            }

            /**
             * Get Feature Store Settings
             *
             * @param featurestore the featurestore to query
             */
            self.getStorageConnectors = function(featurestore) {
                FeaturestoreService.getStorageConnectors(self.projectId, featurestore).then(
                    function (success) {
                        self.storageConnectors = success.data
                        StorageService.store(self.projectId + "_storageconnectors", success.data);
                        self.storageConnectorsLoaded = true
                        self.stopLoading()
                    },
                    function (error) {
                        self.storageConnectorsLoaded = true
                        self.stopLoading()
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the storage connectors for the featurestore',
                            ttl: 15000
                        });
                    });
            }

            /**
             * Check whether a storage connector can be deleted or not (pre-defined storage connectors cannot be
             * deleted).
             *
             * @param name the name of the storage connector to check
             * @returns {boolean} true if it cannot be deleted, otherwise false.
             */
            self.canNotDeleteStorageConnector = function(name) {
                if(name === self.projectName){
                    return true
                }
                if(name === (self.projectFeaturestoreName)){
                    return true
                }
                if(name === (self.projectTrainingdatasetsName)){
                    return true
                }
                if(name.includes("_onlinefeaturestore")) {
                    return true
                }
                return false
            }

            /**
             * Selects a particular feature store (select means that the feature groups/training datasets/features for
             * that featurestore will be shown in the UI.
             *
             * @param fs the feature store to select.
             */
            self.selectFeaturestore = function(featurestoreName) {
                for (var i = 0; i < self.featurestores.length; i++) {
                    if(self.featurestores[i].featurestoreName === featurestoreName) {
                        self.featurestore = self.featurestores[i];
                        StorageService.store(self.projectId + "_featurestore", self.featurestore);
                        self.setupStorageConnectors()
                        self.fetchFeaturestoreSize();
                        self.getStorageConnectors(self.featurestore);
                        self.getTrainingDatasets(self.featurestore);
                        self.getFeaturegroups(self.featurestore);
                        return
                    }
                }
            }

            /**
             * Selects the project feature store (in case there is a list of multiple feature stores shared with
             * the project).
             */
            self.selectProjectFeaturestore = function() {
                if(self.projectFeaturestoreName == null || self.featurestores.length < 1){
                    return
                }
                self.selectFeaturestore(self.projectFeaturestoreName);
            };

            /**
             * Shows the page for updating an existing storage connector.
             *
             * @param storageConnector
             */
            self.updateStorageConnector = function (storageConnector) {
                StorageService.store("connector_operation", "UPDATE");
                StorageService.store(self.projectId + "_connector", storageConnector);
                self.goToUrl("newstorageconnector")
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
             * Retrieves a list of all featuregroups for a given featurestore
             *
             * @param featurestore the featurestore to query
             */
            self.getFeaturegroups = function (featurestore) {
                FeaturestoreService.getFeaturegroups(self.projectId, featurestore).then(
                    function (success) {
                        self.featuregroups = success.data;
                        self.groupFeaturegroupsByVersion();

                        // Compute from-to dates for feature groups
                        var dates = self.featuregroups.map(function(f) { return f.created});
                        dates.sort(function(a, b) {
                            return new Date(b) - new Date(a)
                        });
                        self.featuregroupsFromDate = new Date(dates[dates.length -1]);
                        self.featuregroupsFromDate.setDate(self.featuregroupsFromDate.getDate() - 1);
                        self.featuregroupsToDate = new Date(dates[0]);
                        self.featuregroupsToDate.setDate(self.featuregroupsToDate.getDate() + 1);

                        self.featuregroupsLoaded = true;
                        self.stopLoading()
                    },
                    function (error) {
                        self.featuregroupsLoaded = true;
                        self.stopLoading();
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the featuregroups for the featurestore',
                            ttl: 15000
                        });
                    });
            };


            /**
             * Retrieves a list of all training datasets for a given featurestore
             *
             * @param featurestore the featurestore to query
             */
            self.getTrainingDatasets = function (featurestore) {
                FeaturestoreService.getTrainingDatasets(self.projectId, featurestore).then(
                    function (success) {
                        self.trainingDatasets = success.data;
                        self.groupTrainingDatasetsByVersion();

                        // Compute from-to dates for training datasets 
                        var dates = self.trainingDatasets.map(function(f) { return f.created });
                        dates.sort(function(a, b) {
                            return new Date(b) - new Date(a)
                        });

                        self.trainingDatasetsFromDate = new Date(dates[dates.length - 1]);
                        self.trainingDatasetsFromDate.setDate(self.trainingDatasetsFromDate.getDate() - 1);
                        self.trainingDatasetsToDate = new Date(dates[0]);
                        self.trainingDatasetsToDate.setDate(self.trainingDatasetsToDate.getDate() + 1);

                        self.trainingDatasetsLoaded = true;
                        self.stopLoading()
                    },
                    function (error) {
                        self.trainingDatasetsLoaded = true;
                        self.stopLoading();
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to fetch the training datasets for the featurestore',
                            ttl: 15000
                        });
                    });
            };

            /**
             * Helper to collect the features of all featuregroups in the featurestore into a
             * single list
             */
            self.collectAllFeatures = function () {
                self.fgFeatures = [];
                for (var i = 0; i < self.featuregroups.length; i++) {
                    for (var j = 0; j < self.featuregroups[i].features.length; j++) {
                        self.fgFeatures.push({
                            name: self.featuregroups[i].features[j].name,
                            type: self.featuregroups[i].features[j].type,
                            description: self.featuregroups[i].features[j].description,
                            primary: self.featuregroups[i].features[j].primary,
                            featuregroup: self.featuregroups[i],
                            date: self.featuregroups[i].created,
                            version: self.featuregroups[i].version,
                        })
                    }
                }
            };

            /**
             * Returns the sort field for a training dataset
             *
             * @param td the training dataset to sort
             * @returns {*}
             */
            self.trainingDatasetSortFn = function (td) {
                if(self.trainingDatasetsSortKey == "created"){
                    return td.versionToGroups[td.activeVersion].created
                } else if (self.trainingDatasetsSortKey === "description") {
                    return td.versionToGroups[td.activeVersion].description
                } else if (self.trainingDatasetsSortKey == "dataFormat"){
                    return td.versionToGroups[td.activeVersion].dataFormat
                } else {
                    return td.name
                }
            };

            /**
             * Returns the sort field for a feature group
             *
             * @param featuregroup the feature group to sort
             * @returns {*}
             */
            self.featuregroupsSortFn = function (featuregroup) {
                if(self.featuregroupsSortKey == "created"){
                    return featuregroup.versionToGroups[featuregroup.activeVersion].created
                } else if(self.featuregroupsSortKey == "featuregroupType"){
                    return featuregroup.versionToGroups[featuregroup.activeVersion].featuregroupType
                } else if (self.featuregroupsSortKey === "description") {
                    return featuregroup.versionToGroups[featuregroup.activeVersion].description
                } else if(self.featuregroupsSortKey == "onlineEnabled"){
                    if(featuregroup.versionToGroups[featuregroup.activeVersion].featuregroupType == self.cachedFeaturegroupType
                        && featuregroup.versionToGroups[featuregroup.activeVersion].onlineFeaturegroupEnabled){
                       return "Yes"
                    } else {
                        return "No"
                    }
                } else { 
                    return featuregroup.name
                }
            };

            /**
             * Returns the sort field for a storage connector
             *
             * @param storageConnector the storageConnector to sort
             * @returns {*}
             */
            self.storageConnectorsSortFn = function (storageConnector) {
                if(self.storageConnectorsSortKey == "storageConnectorType"){
                    return storageConnector.storageConnectorType
                }
                if(self.storageConnectorsSortKey == "description"){
                    return storageConnector.description
                }
                return storageConnector.name
            }

            /**
             * Goes through a list of featuregroups and groups them by name so that you get name --> versions mapping
             */
            self.groupFeaturegroupsByVersion = function () {
                var dict = {};
                var i;
                var versionVar;
                for (i = 0; i < self.featuregroups.length; i++) {
                    if (self.featuregroups[i].name in dict) {
                        versionVar = self.featuregroups[i].version.toString();
                        dict[self.featuregroups[i].name][versionVar] = self.featuregroups[i]
                    } else {
                        versionVar = self.featuregroups[i].version.toString();
                        dict[self.featuregroups[i].name] = {};
                        dict[self.featuregroups[i].name][versionVar] = self.featuregroups[i]
                    }
                }
                var dictList = [];
                var item;
                for (var key in dict) {
                    item = {};
                    item.name = key;
                    item.versionToGroups = dict[key];
                    var versions = Object.keys(item.versionToGroups);
                    item.versions = versions;
                    item.activeVersion = versions[versions.length - 1];
                    dictList.push(item);
                }
                self.featuregroupsDictList = dictList
            };

            /**
             * Goes through a list of training datasets and groups them by name so that you get name --> versions mapping
             */
            self.groupTrainingDatasetsByVersion = function () {
                var dict = {};
                var i;
                var versionVar;
                for (i = 0; i < self.trainingDatasets.length; i++) {
                    if (self.trainingDatasets[i].name in dict) {
                        versionVar = self.trainingDatasets[i].version.toString();
                        dict[self.trainingDatasets[i].name][versionVar] = self.trainingDatasets[i]
                    } else {
                        versionVar = self.trainingDatasets[i].version.toString();
                        dict[self.trainingDatasets[i].name] = {};
                        dict[self.trainingDatasets[i].name][versionVar] = self.trainingDatasets[i]
                    }
                }
                var dictList = [];
                var item;
                for (var key in dict) {
                    item = {};
                    item.name = key;
                    item.versionToGroups = dict[key];
                    var versions = Object.keys(item.versionToGroups);
                    item.versions = versions;
                    item.activeVersion = versions[versions.length - 1];
                    dictList.push(item);
                }
                self.trainingDatasetsDictList = dictList
            };

            /**
             * Gets the feature store Quota from Hopsworks
             */
            self.getFeaturestoreQuota = function () {
                self.quotaLoaded = false
                ProjectService.get({}, {'id': self.projectId}).$promise.then(
                    function (success) {
                        self.quotas = success.quotas;
                        self.quotaLoaded = true
                        self.stopLoading()
                    }, function (error) {
                        self.quotaLoaded = true
                        self.stopLoading()
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch featurestore quota', ttl: 15000});
                    }
                );
            }

            /**
             * Gets the featurestore HDFS usage (how many bytes of storage is being used)
             *
             * @returns {null if quota have not been fetched, otherwise the usage in a readable string}
             */
            self.featurestoreHdfsUsage = function () {
                if (self.quotas !== null) {
                    return convertSize(self.quotas.featurestoreHdfsUsageInBytes);
                }
                return null;
            };

            /**
             * Gets the featurestore HDFS quota (how many bytes of storage is allowed)
             *
             * @returns {null if quota have not been fetched, otherwise the quota in a readable string}
             */
            self.featurestoreHdfsQuota = function () {
                if (self.quotas !== null) {
                    return convertSize(self.quotas.featurestoreHdfsQuotaInBytes);
                }
                return null;
            };

            /**
             * Gets the featurestore files count (how many inodes are stored in the feature store)
             *
             * @returns {null if quota have not been fetched, otherwise the number of files}
             */
            self.featurestoreHdfsNsCount = function () {
                if (self.quotas !== null) {
                    return self.quotas.featurestoreHdfsNsCount;
                }
                return null;
            };

            /**
             * Gets the featurestore HDFS number of files quota (how many inodes is allowed in the featurestore)
             *
             * @returns {null if quota have not been fetched, otherwise the quota}
             */
            self.featurestoreHdfsNsQuota = function () {
                if (self.quotas !== null) {
                    return self.quotas.featurestoreHdfsNsQuota;
                }
                return null;
            };

            /**
             * Called when a new featurestore is selected in the dropdown list in the UI
             *
             * @param featurestore the selected featurestore
             */
            self.onSelectFeaturestoreCallback = function (featurestore) {
                self.featurestore = featurestore;
                self.startLoading("Loading Feature store data...");
                self.getStorageConnectors(featurestore);
                self.getTrainingDatasets(featurestore);
                self.getFeaturegroups(featurestore);
                self.fetchFeaturestoreSize();
            };

            /**
             * Initializes the UI by retrieving featurestores from the backend
             */
            self.init = function () {
                self.startLoading("Loading Feature store data...");
                JobService.setJobFilter("");
                self.getProjectName();
                self.getFeaturestores();
                self.getFeaturestoreQuota();
                self.getFeaturestoreSettings();
            };

            self.refresh = function () {
                self.startLoading("Loading Feature store data...");
                self.getFeaturestores();
                self.getFeaturestoreQuota();
                self.getFeaturestoreSettings();
            }

            /**
             * Called when clicking the link to featuregroup from the list of features. Switches the view to the
             * specific featuregroup
             *
             * @param featuregroupName the featuregroup to go to
             */
            self.goToFeaturegroup = function (featuregroupName) {
                self.fgFilter = featuregroupName;
            };

            /**
             * Convert bytes into bytes + suitable unit (e.g KB, MB, GB etc)
             *
             * @param fileSizeInBytes the raw byte number
             */
            self.sizeOnDisk = function (fileSizeInBytes) {
                return convertSize(fileSizeInBytes);
            };

            /**
             * Add version to featuregroup name
             *
             * @param featuregroupName the original featuregroup name
             * @param version the version
             * @returns the featuregroupVersionName
             */
            self.getFeaturegroupSelectName = function (featuregroupName, version) {
                return featuregroupName + "_" + version
            };

            /**
             * Get the API code to retrieve the feature
             */
            self.getCode = function (feature) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_feature(\n"
                codeStr = codeStr + "'" + feature.name + "'"
                codeStr = codeStr + ",\nfeaturestore="
                codeStr = codeStr + "'" + self.featurestore.featurestoreName + "'"
                codeStr = codeStr + ",\nfeaturegroup="
                codeStr = codeStr + "'" + feature.featuregroup.name + "'"
                codeStr = codeStr + ",\nfeaturegroup_version="
                codeStr = codeStr + feature.version
                codeStr = codeStr + ")"
                return codeStr
            };

            /**
             * Format javascript date as string (YYYY-mm-dd HH:MM:SS)
             *
             * @param javaDate date to format
             * @returns {string} formatted string
             */
            $scope.formatDate = function (javaDate) {
                var d = new Date(javaDate);
                return d.getFullYear().toString() + "-" + ((d.getMonth() + 1).toString().length == 2 ? (d.getMonth() + 1).toString() : "0" + (d.getMonth() + 1).toString()) + "-" + (d.getDate().toString().length == 2 ? d.getDate().toString() : "0" + d.getDate().toString()) + " " + (d.getHours().toString().length == 2 ? d.getHours().toString() : "0" + d.getHours().toString()) + ":" + ((parseInt(d.getMinutes() / 5) * 5).toString().length == 2 ? (parseInt(d.getMinutes() / 5) * 5).toString() : "0" + (parseInt(d.getMinutes() / 5) * 5).toString()) + ":00";
            };


            /**
             * Find featuregroup with a given name and version
             *
             * @param featuregroupName the name of the featuregroup
             * @param version the version of the featuregroup
             * @returns featuregroup
             */
            self.getFeaturegroupByNameAndVersion = function (featuregroupName, version) {
                for (var i = 0; i < self.featuregroups.length; i++) {
                    if (self.featuregroups[i].name == featuregroupName && self.featuregroups[i].version == version) {
                        return self.featuregroups[i]
                    }
                }
            };

            /**
             * Send async request to hopsworks to calculate the inode size of the featurestore
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchFeaturestoreSize = function () {
                if (self.featurestoreSizeWorking) {
                    return
                }
                self.featurestoreSizeWorking = true
                var request = {type: "inode", inodeId: self.featurestore.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.featurestoreSizeWorking = false;
                    self.featurestoreSize = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch size of featurestore', ttl: 5000});
                    self.featurestoreSizeWorking = false;
                });
            };

            /**
             * Send async request to hopsworks to calculate the inode size of a feature group
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchFeaturegroupSize = function (featuregroup) {
                if (self.featuregroupSizeWorking) {
                    return
                }
                self.featuregroupSizeWorking = true
                var request = {id: self.projectId, type: "inode", inodeId: featuregroup.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.featuregroupSizeWorking = false;
                    self.featuregroupSize = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch size of feature group', ttl: 5000});
                    self.featuregroupSizeWorking = false;
                });
            };

            /**
             * Setups the configuration of the quota chart in the header in the featurestore UI
             */
            self.setupQuotaChart = function () {
                var quote = Math.round((self.quotas.featurestoreHdfsUsageInBytes / self.quotas.featurestoreHdfsQuotaInBytes)*100)
                var quotaChartOptions = {
                    chart: {
                        height: 225,
                        width:225,
                        type: 'radialBar',
                    },
                    plotOptions: {
                        radialBar: {
                            hollow: {
                                size: '70%',
                                offsetY: -60,
                                offsetX: -200,
                            }
                        },
                    },
                    fill: {
                        colors: ['#111']
                    },
                    dataLabels: {
                        style: {
                            fontSize: '14px',
                            colors: ['#555']
                        }
                    },
                    stroke: {
                        lineCap: "round",
                    },
                    colors: ["#111"],
                    series: [quote],
                    labels: ['Quota'],
                }
                self.quotaChartOptions = quotaChartOptions
            }

            /**
             * Renders the featurestore quota chart on the div in the featurestore header with the id "quotaChart"
             */
            self.renderQuotaChart = function () {
                if(self.quotaChart != null) {
                    self.quotaChart.destroy()
                    self.quotaChart = null;
                    self.quotaChart = new ApexCharts(
                        document.querySelector("#quotaChart"),
                        self.quotaChartOptions
                    );
                    self.quotaChart.render();
                }
                if(self.quotaChart == null) {
                    self.setupQuotaChart();
                    self.quotaChart = new ApexCharts(
                        document.querySelector("#quotaChart"),
                        self.quotaChartOptions
                    );
                    self.quotaChart.render();
                }
            }

            /**
             * Opens the modal to view storage connector information
             *
             * @param storageConnector the connector to inspect
             */
            self.viewStorageConnectorInfo = function (storageConnector) {
                ModalService.storageConnectorViewInfo('lg', self.projectId, storageConnector, self.featurestore, self.settings).then(
                    function (success) {
                    }, function (error) {
                    });
            };

            /**
             * Utility function for switching tabs
             *
             * @param tabIndex
             */
            self.goToTab = function(tabIndex) {
                $scope.featurestoreSelectedTab = tabIndex
            }

            self.init();
       }
    ])
;
