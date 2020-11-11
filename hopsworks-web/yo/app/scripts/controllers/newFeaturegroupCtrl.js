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
 * Controller for managing the "create new feature group page"
 */
angular.module('hopsWorksApp')
    .controller('newFeaturegroupCtrl', ['$routeParams', 'growl',
        '$location', 'StorageService', 'FeaturestoreService', 'ModalService',
        function ($routeParams, growl, $location, StorageService, FeaturestoreService, ModalService) {

            var self = this;

            //Controller Input
            self.projectId = $routeParams.projectID;
            self.featurestore = StorageService.get(self.projectId + "_featurestore")
            self.projectName = StorageService.get("projectName");
            self.featuregroupOperation = StorageService.get("featuregroup_operation");
            self.featuregroup = StorageService.get(self.projectId + "_featuregroup");
            self.version = StorageService.get(self.projectId + "_featuregroup_version");
            self.storageConnectors = StorageService.get(self.projectId + "_storageconnectors")
            self.settings = StorageService.get(self.projectId + "_fssettings")

            //State
            self.configureJob = true;
            self.cachedPhase = 0;
            self.onDemandPhase = 0;
            self.cachedFgWorking = false;
            self.onDemandFgWorking = false;
            self.enableServingWorking = false;
            self.disableServingWorking = false;
            self.onlineFg = false;

            //User Input values for Cached Feature Groups
            self.cachedFeaturegroupName = ""
            self.cachedFeaturegroupDoc = "";
            self.cachedFeaturegroupFeatures = []

            //User Input values for OnDemand Feature Groups
            self.onDemandFeaturegroupName = ""
            self.onDemandFeaturegroupDoc = "";
            self.onDemandFeaturegroupFeatures = []
            self.onDemandFeaturegroupStorageConnector = null;
            self.onDemandSqlQuery = ""

            /**
             * Input validation for Cached feature Groups
             */
            self.cachedFeaturegroupWrong_values = 1;
            //Name and Description Flags
            self.cachedFeaturegroupNameWrongValue = 1;
            self.cachedFeaturegroupDocWrongValue = 1;
            //Schema Flags
            self.cachedFeaturegroupFeatureNamesNotUnique = 1
            self.cachedFeaturegroupPrimaryKeyWrongValue = 1;
            self.cachedFeaturegroupPartitionKeyWrongValue = 1;
            self.cachedFeaturegroupFeaturesWrongValue = 1;
            self.cachedFeaturegroupFeaturesNameWrongValue = [];
            self.cachedFeaturegroupFeaturesTypeWrongValue = [];
            self.cachedFeaturegroupFeaturesOnlineTypeWrongValue = [];
            self.cachedFeaturegroupFeaturesDocWrongValue = [];

            /**
             * Input validation for On Demand feature Groups
             */
            self.onDemandFeaturegroupWrong_values = 1;
            //Name and Description Flags
            self.onDemandFeaturegroupNameWrongValue = 1;
            self.onDemandFeaturegroupDocWrongValue = 1;
            //Schema Flags
            self.onDemandFeaturegroupFeatureNamesNotUnique = 1
            self.onDemandFeaturegroupPrimaryKeyWrongValue = 1;
            self.onDemandFeaturegroupPartitionKeyWrongValue = 1;
            self.onDemandFeaturegroupFeaturesWrongValue = 1;
            self.onDemandFeaturegroupFeaturesNameWrongValue = [];
            self.onDemandFeaturegroupFeaturesTypeWrongValue = [];
            self.onDemandFeaturegroupFeaturesDocWrongValue = [];
            //SQL Flags
            self.onDemandFeaturegroupSqlWrongValue = 1
            self.onDemandFeaturegroupHiveDbWrongValue = 1;
            self.onDemandFeaturegroupJdbcConnectorWrongValue = 1;
            self.onDemandFeaturegroupSqlQueryWrongValue = 1

            //Constants
            self.hiveDatabases = [self.featurestore.featurestoreName, self.projectName.toLowerCase()]
            self.hiveRegexp = self.settings.featurestoreRegex;
            self.featurestoreEntityNameMaxLength = self.settings.featurestoreEntityNameMaxLength
            self.featurestoreEntityDescriptionMaxLength = self.settings.featurestoreEntityDescriptionMaxLength
            self.onDemandFeaturegroupSqlQueryMaxLength = self.settings.onDemandFeaturegroupSqlQueryMaxLength
            self.cachedFeaturegroupDTOType = self.settings.cachedFeaturegroupDtoType
            self.onDemandFeaturegroupDTOType = self.settings.onDemandFeaturegroupDtoType

            //front-end variables
            self.cached_fg_accordion1 = {
                "isOpen": true,
                "value": "",
                "title": "Feature Group Name"
            };
            self.cached_fg_accordion2 = {
                "isOpen": true,
                "value": "",
                "title": "Feature Group Description"
            };
            self.cached_fg_accordion3 = {
                "isOpen": false,
                "value": "",
                "title": "Feature Group Schema (Optional)"
            };
            self.cached_fg_accordion4 = {
                "isOpen": false,
                "value": "",
                "title": "SQL Query (Optional)"
            };
            self.cached_fg_accordion5 = {
                "isOpen": true,
                "value": "",
                "title": "Create"
            };

            self.on_demand_fg_accordion1 = {
                "isOpen": true,
                "value": "",
                "title": "Feature Group Name"
            };
            self.on_demand_fg_accordion2 = {
                "isOpen": true,
                "value": "",
                "title": "Feature Group Description"
            };
            self.on_demand_fg_accordion3 = {
                "isOpen": false,
                "value": "",
                "title": "Feature Group Schema (Optional)"
            };
            self.on_demand_fg_accordion4 = {
                "isOpen": false,
                "value": "",
                "title": "SQL Query"
            };
            self.on_demand_fg_accordion5 = {
                "isOpen": true,
                "value": "",
                "title": "Create"
            };

            /**
             * Perform initialization of variables that require it
             */
            self.initVariables = function () {
                if (self.featuregroupOperation === 'CREATE') {
                    self.cachedFeaturegroupHeading = 'Create Cached Feature Group'
                    self.onDemandFeaturegroupHeading = 'Create On-Demand Feature Group'
                    self.activeTab = 0
                    return;
                }
                if (self.featuregroup != null && self.featuregroupOperation === 'UPDATE') {
                    self.cachedFeaturegroupHeading = 'Update Cached Feature Group'
                    self.onDemandFeaturegroupHeading = 'Update On-Demand Feature Group'
                    self.onDemandFeaturegroupName = self.featuregroup.name
                    self.cachedFeaturegroupName = self.featuregroup.name
                    self.onDemandFeaturegroupDoc = self.featuregroup.description
                    self.cachedFeaturegroupDoc = self.featuregroup.description
                    self.onDemandSqlQuery = self.featuregroup.query
                    self.onDemandFeaturegroupStorageConnector = self.featuregroup.storageConnector
                    self.onDemandFeaturegroupFeatures = self.featuregroup.features
                    self.cachedFeaturegroupFeatures = self.featuregroup.features
                    self.oldFeaturegroupId = self.featuregroup.id
                    if (self.featuregroup.type === 'onDemandFeaturegroupDTO') {
                        self.activeTab = 1
                    }
                    if (self.featuregroup.type === 'cachedFeaturegroupDTO') {
                        self.activeTab = 0
                        if(self.featuregroup.onlineEnabled != null){
                            self.onlineFg = self.featuregroup.onlineEnabled
                        }
                    }
                    self.cached_fg_accordion1.isOpen = true
                    self.cached_fg_accordion2.isOpen = false
                    self.cached_fg_accordion3.isOpen = false
                    self.cached_fg_accordion4.isOpen = false
                    self.cached_fg_accordion5.isOpen = true
                    self.cached_fg_accordion5.title = "Update"

                    self.on_demand_fg_accordion1.isOpen = true
                    self.on_demand_fg_accordion2.isOpen = false
                    self.on_demand_fg_accordion3.isOpen = false
                    self.on_demand_fg_accordion4.isOpen = false
                    self.on_demand_fg_accordion5.isOpen = true
                    self.on_demand_fg_accordion5.title = "Update"
                }
                if (self.featuregroup != null && self.featuregroupOperation === 'NEW_VERSION') {
                    self.onDemandFeaturegroupName = self.featuregroup.name
                    self.cachedFeaturegroupName = self.featuregroup.name
                    self.onDemandFeaturegroupDoc = self.featuregroup.description
                    self.cachedFeaturegroupDoc = self.featuregroup.description
                    self.onDemandSqlQuery = self.featuregroup.query
                    self.onDemandFeaturegroupFeatures = self.featuregroup.features
                    self.cachedFeaturegroupFeatures = self.featuregroup.features
                    self.oldFeaturegroupId = self.featuregroup.id
                    if (self.featuregroup.type === 'onDemandFeaturegroupDTO') {
                        self.activeTab = 1
                    }
                    if (self.featuregroup.type === 'cachedFeaturegroupDTO') {
                        self.activeTab = 0
                    }
                    self.onDemandFeaturegroupHeading = 'Create New Version of On-Demand Feature Group'
                    self.cachedFeaturegroupHeading = 'Create New Version of Cached Feature Group'

                    self.cached_fg_accordion1.isOpen = true
                    self.cached_fg_accordion2.isOpen = false
                    self.cached_fg_accordion3.isOpen = false
                    self.cached_fg_accordion4.isOpen = false
                    self.cached_fg_accordion5.isOpen = true
                    self.cached_fg_accordion5.title = "Create New Version"

                    self.on_demand_fg_accordion1.isOpen = true
                    self.on_demand_fg_accordion2.isOpen = false
                    self.on_demand_fg_accordion3.isOpen = false
                    self.on_demand_fg_accordion4.isOpen = false
                    self.on_demand_fg_accordion5.isOpen = true
                    self.on_demand_fg_accordion5.title = "Create New Version"
                }
            }

            /**
             * Callback method for when the user filled in a featuregroup name. Will then
             * display the description field
             * @returns {undefined}
             */
            self.cachedNameFilledIn = function () {
                if (self.featuregroupOperation === 'CREATE') {
                    if (self.cachedPhase === 0) {
                        if (!self.cachedFeaturegroupName) {
                            self.cachedFeaturegroupName = "Featuregroup-" + Math.round(new Date().getTime() / 1000);
                        }
                        self.cachedPhase = 1;
                        self.cached_fg_accordion2.isOpen = true; //Open description selection
                    }
                    self.cached_fg_accordion1.value = " - " + self.cachedFeaturegroupName; //Edit panel title
                }
            };

            /**
             * Callback method for when the user filled in a featuregroup name. Will then
             * display the description field
             * @returns {undefined}
             */
            self.onDemandNameFilledIn = function () {
                if (self.featuregroupOperation === 'CREATE') {
                    if (self.onDemandPhase === 0) {
                        if (!self.onDemandFeaturegroupName) {
                            self.onDemandFeaturegroupName = "Featuregroup-" + Math.round(new Date().getTime() / 1000);
                        }
                        self.onDemandPhase = 1;
                        self.on_demand_fg_accordion2.isOpen = true; //Open description selection
                    }
                    self.on_demand_fg_accordion1.value = " - " + self.onDemandFeaturegroupName; //Edit panel title
                }
            };

            /**
             * Callback method for when the user filled in a featuregroup description. Will then
             * display the type field
             * @returns {undefined}
             */
            self.cachedDescriptionFilledIn = function () {
                if (self.featuregroupOperation === 'CREATE') {
                    if (self.cachedPhase === 1) {
                        if (!self.cachedFeaturegroupDoc) {
                            self.cachedFeaturegroupDoc = "-";
                        }
                        self.cachedPhase = 2;
                        self.cached_fg_accordion5.isOpen = true;
                    }
                    self.cached_fg_accordion2.value = " - " + self.cachedFeaturegroupDoc; //Edit panel title
                }
            };

            /**
             * Callback method for when the user filled in a featuregroup description. Will then
             * display the type field
             * @returns {undefined}
             */
            self.onDemandDescriptionFilledIn = function () {
                if (self.featuregroupOperation === 'CREATE') {
                    if (self.onDemandPhase === 1) {
                        if (!self.onDemandFeaturegroupDoc) {
                            self.onDemandFeaturegroupDoc = "-";
                        }
                        self.onDemandPhase = 2;
                        self.on_demand_fg_accordion5.isOpen = true;
                    }
                    self.on_demand_fg_accordion2.value = " - " + self.onDemandFeaturegroupDoc; //Edit panel title
                }
            };

            /**
             * Function called when the user press "add Feature" button in the create-feature-group form
             * for a cached feature group, adds a new feature
             */
            self.addNewCachedFeature = function () {
                self.cachedFeaturegroupFeatures.push({
                    'name': '',
                    'type': '',
                    'onlineType': '',
                    'description': "",
                    primary: false,
                    partition: false
                });
                self.cachedFeaturegroupFeaturesNameWrongValue.push(1);
                self.cachedFeaturegroupFeaturesTypeWrongValue.push(1);
                self.cachedFeaturegroupFeaturesOnlineTypeWrongValue.push(1);
            };

            /**
             * Function called when the user press "delete Feature" button in the create-feature-group form
             * for a cached feature group, Deletes a new feature
             */
            self.removeNewCachedFeature = function (index) {
                self.cachedFeaturegroupFeatures.splice(index, 1);
                self.cachedFeaturegroupFeaturesNameWrongValue.splice(index, 1);
                self.cachedFeaturegroupFeaturesTypeWrongValue.splice(index, 1);
                self.cachedFeaturegroupFeaturesOnlineTypeWrongValue.splice(index, 1);
            };

            /**
             * Function called when the user press "add Feature" button in the create-feature-group form
             * for an on-demand feature group, adds a new feature
             */
            self.addNewOnDemandFeature = function () {
                self.onDemandFeaturegroupFeatures.push({
                    'name': '',
                    'type': '',
                    'onlineType': null,
                    'description': "",
                    primary: false,
                    partition: false
                });
                self.onDemandFeaturegroupFeaturesNameWrongValue.push(1);
                self.onDemandFeaturegroupFeaturesTypeWrongValue.push(1);
            };

            /**
             * Function called when the user press "delete Feature" button in the create-feature-group form
             * for an on-demand feature group, Deletes a new feature
             */
            self.removeNewOnDemandFeature = function (index) {
                self.onDemandFeaturegroupFeatures.splice(index, 1);
                self.onDemandFeaturegroupFeaturesNameWrongValue.splice(index, 1);
                self.onDemandFeaturegroupFeaturesTypeWrongValue.splice(index, 1);
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

            /**
             * Function called when the user clicks the "Feature type" button, opens up a modal where the user
             * can select a pre-defined Hive type or define a custom type.
             *
             * @param feature the feature to define the type for
             */
            self.selectOnlineFeatureType = function (feature) {
                ModalService.selectFeatureType('lg', true, self.settings).then(
                    function (success) {
                        feature.onlineType = success
                    },
                    function (error) {
                        // Users changed their minds.
                    });
            };

            /**
             * Exit the "create new feature group page" and go back to the featurestore page
             */
            self.exitToFeaturestore = function () {
                //StorageService.store(self.projectId + "_featurestore_tab", 0);
                $location.path('project/' + self.projectId + '/featurestore');
            };


            /**
             * Validates user input for creating new 'On-Demand' Feature Groups
             */
            self.validateOnDemandFeaturegroupInputs = function () {
                //Reset Validation Flags
                self.onDemandFeaturegroupNameWrongValue = 1;
                self.onDemandFeaturegroupWrong_values = 1;
                self.onDemandFeaturegroupFeatureNamesNotUnique = 1
                self.onDemandFeaturegroupFeaturesDocWrongValue = 1;
                self.onDemandFeaturegroupPrimaryKeyWrongValue = 1;
                self.onDemandFeaturegroupPartitionKeyWrongValue = 1;
                self.onDemandSqlQueryWrongValue = 1
                self.onDemandFeaturegroupFeaturesWrongValue = 1;
                self.onDemandFeaturegroupSqlWrongValue = 1
                self.onDemandFeaturegroupSqlQueryWrongValue = 1
                self.onDemandFeaturegroupJdbcConnectorWrongValue = 1;
                self.onDemandFgWorking = true;
                for (i = 0; i < self.onDemandFeaturegroupFeaturesNameWrongValue.length; i++) {
                    self.onDemandFeaturegroupFeaturesNameWrongValue[i] = 1
                }

                for (i = 0; i < self.onDemandFeaturegroupFeaturesTypeWrongValue.length; i++) {
                    self.onDemandFeaturegroupFeaturesTypeWrongValue[i] = 1
                }

                for (i = 0; i < self.onDemandFeaturegroupFeaturesDocWrongValue.length; i++) {
                    self.onDemandFeaturegroupFeaturesDocWrongValue[i] = 1
                }

                //Validate Name and Description
                if (!self.onDemandFeaturegroupName || self.onDemandFeaturegroupName.search(self.hiveRegexp) == -1) {
                    self.onDemandFeaturegroupNameWrongValue = -1;
                    self.onDemandFeaturegroupWrong_values = -1;
                } else {
                    self.onDemandFeaturegroupNameWrongValue = 1;
                }
                if (!self.onDemandFeaturegroupDoc || self.onDemandFeaturegroupDoc == undefined) {
                    self.onDemandFeaturegroupDoc = ""
                }
                if (self.onDemandFeaturegroupDoc && self.onDemandFeaturegroupDoc.length > self.featurestoreEntityDescriptionMaxLength) {
                    self.onDemandFeaturegroupDocWrongValue = -1;
                    self.onDemandFeaturegroupWrong_values = -1;
                } else {
                    self.onDemandFeaturegroupDocWrongValue = 1;
                }

                //Validate Schema
                var i;
                var featureNames = []
                var numberOfPrimary = 0
                for (i = 0; i < self.onDemandFeaturegroupFeatures.length; i++) {
                    featureNames.push(self.onDemandFeaturegroupFeatures[i].name)
                    if (self.onDemandFeaturegroupFeatures[i].name.search(self.hiveRegexp) == -1) {
                        self.onDemandFeaturegroupFeaturesNameWrongValue[i] = -1
                        self.onDemandFeaturegroupWrong_values = -1;
                        self.onDemandFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.onDemandFeaturegroupFeatures[i].type === "") {
                        self.onDemandFeaturegroupFeaturesTypeWrongValue[i] = -1
                        self.onDemandFeaturegroupWrong_values = -1;
                        self.onDemandFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.onDemandFeaturegroupFeatures[i].description && self.onDemandFeaturegroupFeatures[i].description.length >
                        self.featurestoreEntityDescriptionMaxLength) {
                        self.onDemandFeaturegroupFeaturesDocWrongValue[i] = -1
                        self.onDemandFeaturegroupWrong_values = -1;
                        self.onDemandFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.onDemandFeaturegroupFeatures[i].primary) {
                        numberOfPrimary++;
                        if (self.onDemandFeaturegroupFeatures[i].partition) {
                            self.onDemandFeaturegroupPartitionKeyWrongValue = -1
                            self.onDemandFeaturegroupWrong_values = -1;
                            self.onDemandFeaturegroupFeaturesWrongValue = -1;
                        }
                    }
                }
                if (self.onDemandFeaturegroupFeatures.length > 0) {
                    if (numberOfPrimary == 0) {
                        self.onDemandFeaturegroupPrimaryKeyWrongValue = -1
                        self.onDemandFeaturegroupWrong_values = -1;
                        self.onDemandFeaturegroupFeaturesWrongValue = -1;
                    } else {
                        self.onDemandFeaturegroupPrimaryKeyWrongValue = 1
                    }
                    var hasDuplicates = (new Set(featureNames)).size !== featureNames.length;
                    if (hasDuplicates) {
                        self.onDemandFeaturegroupFeatureNamesNotUnique = -1
                        self.onDemandFeaturegroupWrong_values = -1;
                        self.onDemandFeaturegroupFeaturesWrongValue = -1;
                    }
                    for (i = 0; i < self.onDemandFeaturegroupFeatures.length; i++) {
                        if (!self.onDemandFeaturegroupFeatures[i].description || self.onDemandFeaturegroupFeatures[i].description.length == 0) {
                            self.onDemandFeaturegroupFeatures[i].description = "-"
                        }
                    }
                } else {
                    self.onDemandFeaturegroupFeaturesWrongValue = -1;
                }

                //Validate SQL Query
                if (!self.onDemandSqlQuery || self.onDemandSqlQuery == undefined || self.onDemandSqlQuery == null
                    || self.onDemandSqlQuery.length > self.onDemandFeaturegroupSqlQueryMaxLength) {
                    self.onDemandFeaturegroupSqlQueryWrongValue = -1
                    self.onDemandFeaturegroupSqlWrongValue = -1
                    self.onDemandFeaturegroupWrong_values = -1;
                }

                if (self.onDemandFeaturegroupStorageConnector == null 
                    || !self.onDemandFeaturegroupStorageConnector
                    || self.onDemandFeaturegroupStorageConnector == undefined) {
                    self.onDemandFeaturegroupSqlWrongValue = -1
                    self.onDemandFeaturegroupJdbcConnectorWrongValue = -1;
                    self.onDemandFeaturegroupWrong_values = -1;
                }
            }

            /**
             * Validates user input for creating new 'Cached' Feature Groups
             */
            self.validateCachedFeaturegroupInputs = function () {
                //Reset Validation Flags
                self.cachedFeaturegroupNameWrongValue = 1;
                self.cachedFeaturegroupWrong_values = 1;
                self.cachedFeaturegroupFeatureNamesNotUnique = 1
                self.cachedFeaturegroupFeaturesDocWrongValue = 1;
                self.cachedFeaturegroupPrimaryKeyWrongValue = 1;
                self.cachedFeaturegroupPartitionKeyWrongValue = 1;
                self.cachedSqlQueryWrongValue = 1
                self.cachedFeaturegroupFeaturesWrongValue = 1;
                if(!self.enableServingWorking){
                    self.cachedFgWorking = true;
                }
                for (i = 0; i < self.cachedFeaturegroupFeaturesNameWrongValue.length; i++) {
                    self.cachedFeaturegroupFeaturesNameWrongValue[i] = 1
                }

                for (i = 0; i < self.cachedFeaturegroupFeaturesTypeWrongValue.length; i++) {
                    self.cachedFeaturegroupFeaturesTypeWrongValue[i] = 1
                }

                for (i = 0; i < self.cachedFeaturegroupFeaturesOnlineTypeWrongValue.length; i++) {
                    self.cachedFeaturegroupFeaturesOnlineTypeWrongValue[i] = 1
                }

                for (i = 0; i < self.cachedFeaturegroupFeaturesDocWrongValue.length; i++) {
                    self.cachedFeaturegroupFeaturesDocWrongValue[i] = 1
                }

                //Validate Name and Description
                if (!self.cachedFeaturegroupName || self.cachedFeaturegroupName.search(self.hiveRegexp) == -1) {
                    self.cachedFeaturegroupNameWrongValue = -1;
                    self.cachedFeaturegroupWrong_values = -1;
                } else {
                    self.cachedFeaturegroupNameWrongValue = 1;
                }
                if (!self.cachedFeaturegroupDoc || self.cachedFeaturegroupDoc == undefined) {
                    self.cachedFeaturegroupDoc = ""
                }
                if (self.cachedFeaturegroupDoc && self.cachedFeaturegroupDoc.length > self.featurestoreEntityDescriptionMaxLength) {
                    self.cachedFeaturegroupDocWrongValue = -1;
                    self.cachedFeaturegroupWrong_values = -1;
                } else {
                    self.cachedFeaturegroupDocWrongValue = 1;
                }

                //Validate Schema
                var i;
                var featureNames = []
                var numberOfPrimary = 0
                for (i = 0; i < self.cachedFeaturegroupFeatures.length; i++) {
                    featureNames.push(self.cachedFeaturegroupFeatures[i].name)
                    if (self.cachedFeaturegroupFeatures[i].name.search(self.hiveRegexp) == -1 ) {
                        self.cachedFeaturegroupFeaturesNameWrongValue[i] = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.cachedFeaturegroupFeatures[i].type === "") {
                        self.cachedFeaturegroupFeaturesTypeWrongValue[i] = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    }
                    if ((self.cachedFeaturegroupFeatures[i].onlineType == undefined ||
                        self.cachedFeaturegroupFeatures[i].onlineType == null ||
                        self.cachedFeaturegroupFeatures[i].onlineType === "") && (self.onlineFg || self.enableServingWorking)) {
                        self.cachedFeaturegroupFeaturesOnlineTypeWrongValue[i] = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.cachedFeaturegroupFeatures[i].description && self.cachedFeaturegroupFeatures[i].description.length >
                        self.featurestoreEntityDescriptionMaxLength) {
                        self.cachedFeaturegroupFeaturesDocWrongValue[i] = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    }
                    if (self.cachedFeaturegroupFeatures[i].primary) {
                        numberOfPrimary++;
                        if (self.cachedFeaturegroupFeatures[i].partition) {
                            self.cachedFeaturegroupPartitionKeyWrongValue = -1
                            self.cachedFeaturegroupWrong_values = -1;
                            self.cachedFeaturegroupFeaturesWrongValue = -1;
                        }
                    }
                }
                if (self.cachedFeaturegroupFeatures.length > 0) {
                    if (numberOfPrimary == 0) {
                        self.cachedFeaturegroupPrimaryKeyWrongValue = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    } else {
                        self.cachedFeaturegroupPrimaryKeyWrongValue = 1
                    }
                    var hasDuplicates = (new Set(featureNames)).size !== featureNames.length;
                    if (hasDuplicates) {
                        self.cachedFeaturegroupFeatureNamesNotUnique = -1
                        self.cachedFeaturegroupWrong_values = -1;
                        self.cachedFeaturegroupFeaturesWrongValue = -1;
                    }
                    for (i = 0; i < self.cachedFeaturegroupFeatures.length; i++) {
                        if (!self.cachedFeaturegroupFeatures[i].description || self.cachedFeaturegroupFeatures[i].description.length == 0) {
                            self.cachedFeaturegroupFeatures[i].description = "-"
                        }
                    }
                }
            }

            /**
             * Function called when the "create feature group" button is pressed for an on-demand feature group.
             * Validates parameters and then sends a POST request to the backend to create the new
             * feature group
             */
            self.createOnDemandFeaturegroup = function () {
                self.validateOnDemandFeaturegroupInputs()
                if (self.onDemandFeaturegroupWrong_values === -1) {
                    self.onDemandFgWorking = false;
                    return;
                }
                var featuregroupJson = {
                    "name": self.onDemandFeaturegroupName,
                    "description": self.onDemandFeaturegroupDoc,
                    "features": self.onDemandFeaturegroupFeatures,
                    "version": self.version,
                    "storageConnector": self.onDemandFeaturegroupStorageConnector,
                    "query": self.onDemandSqlQuery,
                    "type": self.onDemandFeaturegroupDTOType,
                    "jobs": []
                }
                ModalService.confirm('sm', 'If a Feature Group with the same name and version already' +
                    ' exists in the Feature Store, it will be overridden.')
                    .then(function (success) {
                        FeaturestoreService.createFeaturegroup(self.projectId, featuregroupJson, self.featurestore).then(
                            function (success) {
                                self.onDemandFgWorking = false;
                                self.exitToFeaturestore()
                                growl.success("Feature group created", {title: 'Success', ttl: 1000});
                            }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Failed to create feature group', ttl: 15000});
                                self.onDemandFgWorking = false;
                            });
                        growl.info("Creating feature group... wait", {title: 'Creating', ttl: 1000})
                    }, function (error) {
                        self.onDemandFgWorking = false;
                    });
            }

            /**
             * Function called when the "update feature group" button is pressed for an on-demand feature group.
             * Validates parameters and then sends a PUT request to the backend to update the featuregroup
             */
            self.updateOnDemandFeaturegroup = function () {
                self.validateOnDemandFeaturegroupInputs()
                if (self.onDemandFeaturegroupWrong_values === -1) {
                    self.onDemandFgWorking = false;
                    return;
                }

                var featuregroupJson = {
                    "name": self.onDemandFeaturegroupName,
                    "description": self.onDemandFeaturegroupDoc,
                    "features": self.onDemandFeaturegroupFeatures,
                    "version": self.version,
                    "storageConnector": self.onDemandFeaturegroupStorageConnector,
                    "query": self.onDemandSqlQuery,
                    "type": self.onDemandFeaturegroupDTOType,
                    "jobs": []
                }

                FeaturestoreService.updateFeaturegroupMetadata(self.projectId, self.featurestore, self.oldFeaturegroupId, featuregroupJson).then(
                    function (success) {
                        self.onDemandFgWorking = false;
                        self.exitToFeaturestore()
                        growl.success("Feature group updated", {title: 'Success', ttl: 1000});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to update feature group',
                            ttl: 15000
                        });
                        self.onDemandFgWorking = false;
                    });
                growl.info("Updating featuregroup...", {title: 'Updating', ttl: 1000})
            }

            /**
             * Function called when the "enable online serving" button for a cached feature group is pressed
             */
            self.enableOnlineServing = function () {
                self.enableServingWorking = true;
                self.validateCachedFeaturegroupInputs()
                if (self.cachedFeaturegroupWrong_values === -1) {
                    self.enableServingWorking = false;
                    return;
                }
                var featuregroupJson = {
                    "name": self.cachedFeaturegroupName,
                    "description": self.cachedFeaturegroupDoc,
                    "features": self.cachedFeaturegroupFeatures,
                    "version": self.version,
                    "type": self.cachedFeaturegroupDTOType,
                    "jobs": [],
                    "onlineEnabled": self.onlineFg
                }
                FeaturestoreService.enableOnlineServing(self.projectId, self.featurestore,
                    self.oldFeaturegroupId, featuregroupJson).then(
                    function (success) {
                        self.enableServingWorking = false;
                        self.exitToFeaturestore()
                        growl.success("Online feature serving enabled for feature group",
                            {title: 'Success', ttl: 1000});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to enable online serving for feature group',
                            ttl: 15000
                        });
                        self.enableServingWorking = false;
                    });
                growl.info("Enabled online serving for featuregroup...",
                    {title: 'Enabling online serving for featuregroup', ttl: 1000})
            }

            /**
             * Function called when the "disable online serving" button for a cached feature group is pressed
             */
            self.disableOnlineServing = function () {
                self.disableServingWorking = true;
                var featuregroupJson = {
                    "name": self.cachedFeaturegroupName,
                    "description": self.cachedFeaturegroupDoc,
                    "features": self.cachedFeaturegroupFeatures,
                    "version": self.version,
                    "type": self.cachedFeaturegroupDTOType,
                    "jobs": [],
                    "onlineEnabled": self.onlineFg
                }
                FeaturestoreService.disableOnlineServing(self.projectId, self.featurestore,
                    self.oldFeaturegroupId, featuregroupJson).then(
                    function (success) {
                        self.disableServingWorking = false;
                        self.exitToFeaturestore()
                        growl.success("Online feature serving disabled for feature group",
                            {title: 'Success', ttl: 1000});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Failed to disable online serving for feature group',
                            ttl: 15000
                        });
                        self.disableServingWorking = false;
                    });
                growl.info("Disable online serving for featuregroup...",
                    {title: 'Disable online serving for featuregroup', ttl: 1000})
            }

            /**
             * Function called when the "update feature group" button is pressed for a cached feature group.
             * Validates parameters and then sends a POST or PUT request to the backend to update the featuregroup
             */
            self.updateCachedFeaturegroup = function () {
                self.validateCachedFeaturegroupInputs()
                if (self.cachedFeaturegroupWrong_values === -1) {
                    self.cachedFgWorking = false;
                    return;
                }
                var featuregroupJson = {
                    "name": self.cachedFeaturegroupName,
                    "description": self.cachedFeaturegroupDoc,
                    "features": self.cachedFeaturegroupFeatures,
                    "version": self.version,
                    "type": self.cachedFeaturegroupDTOType,
                    "jobs": [],
                    "onlineEnabled": self.onlineFg
                }
                ModalService.confirm('lg', 'This is a cached feature group, updating the feature group Hive/MySQL' +
                    ' metadata' +
                    ' (description, feature group name, and features schema) ' +
                    'will delete the existing data.',
                    'Are you sure that you want to update the feature group schema? ' +
                    'It will drop all the existing data stored in the feature group table.' +
                    ' If you want to keep the feature group contents but modify the schema, create a ' +
                    'new version of the feature group and keep the old one. ' +
                    'If you change the name of the featuregroup you have to do it for each version of the featuregroup manually, it is not recommended if you have more than one version.')
                    .then(function (success) {
                        FeaturestoreService.deleteFeaturegroup(self.projectId, self.featurestore, self.oldFeaturegroupId).then(
                            function (success) {
                                FeaturestoreService.createFeaturegroup(self.projectId, featuregroupJson, self.featurestore).then(
                                    function (success) {
                                        self.cachedFgWorking = false;
                                        self.exitToFeaturestore()
                                        growl.success("Feature group updated", {title: 'Success', ttl: 1000});
                                    }, function (error) {
                                        growl.error(error.data.errorMsg, {
                                            title: 'Failed to update feature group',
                                            ttl: 15000
                                        });
                                        self.cachedFgWorking = false;
                                    });
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to update feature group',
                                    ttl: 15000
                                });
                                self.cachedFgWorking = false;
                            });
                        growl.info("Updating featuregroup...", {title: 'Updating', ttl: 1000})
                    }, function (error) {
                        self.cachedFgWorking = false;
                    });
            }

            /**
             * Function called when the "create feature group" button is pressed for a cached feature group.
             * Validates parameters and then sends a POST request to the backend to create the new
             * feature group
             */
            self.createCachedFeaturegroup = function () {
                self.validateCachedFeaturegroupInputs()
                if (self.cachedFeaturegroupWrong_values === -1) {
                    self.cachedFgWorking = false;
                    return;
                }
                var featuregroupJson = {
                    "name": self.cachedFeaturegroupName,
                    "description": self.cachedFeaturegroupDoc,
                    "features": self.cachedFeaturegroupFeatures,
                    "version": self.version,
                    "type": self.cachedFeaturegroupDTOType,
                    "jobs": [],
                    "onlineEnabled": self.onlineFg
                }

                FeaturestoreService.createFeaturegroup(self.projectId, featuregroupJson, self.featurestore).then(
                function (success) {
                    self.cachedFgWorking = false;
                    self.exitToFeaturestore()
                    growl.success("Feature group created", {title: 'Success', ttl: 1000});
                }, function (error) {
                    growl.error(error.data.errorMsg, {
                        title: 'Failed to create feature group',
                        ttl: 15000
                    });
                    self.cachedFgWorking = false;
                });
                growl.info("Creating feature group... wait", {title: 'Creating', ttl: 1000})
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
             * Initialize controller
             */
            self.init = function () {
                self.initVariables()
            }

            /**
             * Boolean parameter indicating whether a spark job should be configured for creating the new training
             * dataset
             */
            self.setConfigureJob = function() {
                if(self.configureJob){
                    self.configureJob = false
                } else {
                    self.configureJob = true
                }
            }

            /**
             * Boolean parameter indicating whether online feature serving should be enabled for the feature group
             */
            self.setOnlineFg = function() {
                if(self.onlineFg) {
                    self.onlineFg = false
                } else {
                    self.onlineFg = true
                }
            }

            self.init()
        }]);
