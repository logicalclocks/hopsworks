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
 * Controller for the update-feature-group modal
 */
angular.module('hopsWorksApp')
    .controller('updateFeaturegroupCtrl', ['$uibModalInstance', 'FeaturestoreService',
        'growl', 'projectId', 'featuregroup', 'featurestore', 'jobs', 'featuregroups', '$scope', 'ModalService',
        function ($uibModalInstance, FeaturestoreService, growl, projectId, featuregroup, featurestore, jobs, featuregroups, $scope, ModalService) {
            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featurestore = featurestore;
            self.oldFeaturegroupFeatures = JSON.stringify(featuregroup.features) //Deep copy to keep as reference of old state
            self.oldFeaturegroupDescription = JSON.stringify(featuregroup.description) //Deep copy to keep as reference of old state
            self.oldFeaturegroupName = JSON.stringify(featuregroup.name) //Deep copy to keep as reference of old state
            self.oldFeaturegroupId = featuregroup.id
            self.featuregroup = featuregroup;
            self.version = self.featuregroup.version
            self.jobs = []
            self.jobNoneOption = {name: "None", id: null}
            self.jobs.push(self.jobNoneOption)
            self.jobs = self.jobs.concat(jobs);
            self.featuregroups = featuregroups;
            var selectedJob = self.jobs[0];
            var i;
            if (featuregroup.jobId !== null) {
                for (i = 0; i < self.jobs.length; i++) {
                    if (self.jobs[i].id === featuregroup.jobId) {
                        selectedJob = self.jobs[i];
                    }
                }
            }
            $scope.selected = {value: selectedJob}
            self.features = featuregroup.features;
            self.hiveRegexp = /^[a-zA-Z0-9-_]+$/;

            self.featuregroupNameWrongValue = 1;
            self.featuregroupDocWrongValue = 1;
            self.featuresNameWrongValue = [];
            self.featuresTypeWrongValue = [];
            self.featuresDocWrongValue = [];

            for (i = 0; i < self.features.length; i++) {
                self.featuresNameWrongValue[i] = 1;
                self.featuresTypeWrongValue[i] = 1;
                self.featuresDocWrongValue[i] = 1;
            }
            self.featureNamesNotUnique = 1
            self.featuregroupNameNotUnique = 1
            self.primaryKeyWrongValue = 1;
            self.emptyFeatures = 1
            self.wrong_values = 1;
            self.working = false;
            self.dependenciesNotUnique = 1

            self.featuregroupName = featuregroup.name
            self.featuregroupDoc = featuregroup.description;
            self.dependencies = []
            for (i = 0; i < self.featuregroup.dependencies.length; i++) {
                self.dependencies.push(self.featuregroup.dependencies[i].path)
            }

            /**
             * Function called when the "update feature group" button is pressed.
             * Validates parameters and then sends a POST request to the backend to update the
             * feature group
             */
            self.updateFeaturegroup = function () {
                self.featuregroupNameWrongValue = 1;
                self.wrong_values = 1;
                self.featureNamesNotUnique = 1
                self.featuregroupNameNotUnique = 1
                self.emptyFeatures = 1
                self.dependenciesNotUnique = 1
                self.featuregroupDocWrongValue = 1;
                self.primaryKeyWrongValue = 1;
                self.working = true;
                for (i = 0; i < self.featuresNameWrongValue.length; i++) {
                    self.featuresNameWrongValue[i] = 1
                }

                for (i = 0; i < self.featuresTypeWrongValue.length; i++) {
                    self.featuresTypeWrongValue[i] = 1
                }

                for (i = 0; i < self.featuresDocWrongValue.length; i++) {
                    self.featuresDocWrongValue[i] = 1
                }

                if (!self.featuregroupName || self.featuregroupName.search(self.hiveRegexp) == -1 || self.featuregroupName.length > 256) {
                    self.featuregroupNameWrongValue = -1;
                    self.wrong_values = -1;
                }
                else {
                    self.featuregroupNameWrongValue = 1;
                }
                if(!self.featuregroupDoc || self.featuregroupDoc == undefined){
                    self.featuregroupDoc=""
                }
                if (self.featuregroupDoc && self.featuregroupDoc.length > 2000) {
                    self.featuregroupDocWrongValue = -1;
                    self.wrong_values = -1;
                } else {
                    self.featuregroupDocWrongValue = 1;
                }
                for (i = 0; i < self.dependencies.length; i++) {
                    if(self.dependencies[i].substring(0,7) === "hdfs://"){
                        self.dependencies[i] = self.dependencies[i].substring(7)
                    }
                }
                if (self.features.length == 0) {
                    self.emptyFeatures = -1;
                    self.wrong_values = -1;
                } else {
                    self.emptyFeatures = 1;
                }
                var i;
                var featureNames = []
                var numberOfPrimary = 0
                for (i = 0; i < self.features.length; i++) {
                    featureNames.push(self.features[i].name)
                    if (self.features[i].name === "" || self.features[i].name.search(self.hiveRegexp) == -1 || self.features[i].name.length > 767) {
                        self.featuresNameWrongValue[i] = -1
                        self.wrong_values = -1;
                    }
                    if (self.features[i].type === "") {
                        self.featuresTypeWrongValue[i] = -1
                        self.wrong_values = -1;
                    }
                    if (self.features[i].description && self.features[i].description.length > 256) {
                        self.featuresDocWrongValue[i] = -1
                        self.wrong_values = -1;
                    }
                    if (self.features[i].primary) {
                        numberOfPrimary++;
                    }
                }
                if (numberOfPrimary != 1) {
                    self.primaryKeyWrongValue = -1
                    self.wrong_values = -1;
                } else {
                    self.primaryKeyWrongValue = 1
                }
                var hasDuplicates = (new Set(featureNames)).size !== featureNames.length;
                if (hasDuplicates) {
                    self.featureNamesNotUnique = -1
                    self.wrong_values = -1;
                }
                var hasDuplicates2 = (new Set(self.dependencies)).size !== self.dependencies.length;
                if(hasDuplicates2){
                    self.dependenciesNotUnique = -1
                    self.wrong_values = -1;
                }
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                for (i = 0; i < self.features.length; i++) {
                    if (!self.features[i].description || self.features[i].description.length == 0) {
                        self.features[i].description = "-"
                    }
                }
                var onlyMetadataChanged = false
                if (angular.toJson(self.features) === self.oldFeaturegroupFeatures &&
                    JSON.stringify(self.featuregroupDoc) === self.oldFeaturegroupDescription &&
                    JSON.stringify(self.featuregroupName) === self.oldFeaturegroupName
                ) {
                    onlyMetadataChanged = true
                }
                var featuregroupJson = {
                    "name": self.featuregroupName,
                    "dependencies": self.dependencies,
                    "jobId": $scope.selected.value.id,
                    "description": self.featuregroupDoc,
                    "features": self.features,
                    "version": self.version,
                    "featureCorrelationMatrixBase64": null,
                    "descriptiveStatistics": null,
                    "updateMetadata": true,
                    "updateStats": false,
                    "featuresHistogram": null
                }
                if (!onlyMetadataChanged) {
                    ModalService.confirm('sm', 'Updating the feature group Hive data (description, feature group name, and features schema) will delete the existing data',
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
                                            self.working = false;
                                            $uibModalInstance.close(success)
                                            growl.success("Feature group updated", {title: 'Success', ttl: 1000});
                                        }, function (error) {
                                            growl.error(error.data.errorMsg, {
                                                title: 'Failed to update feature group',
                                                ttl: 15000
                                            });
                                            self.working = false;
                                        });
                                },
                                function (error) {
                                    growl.error(error.data.errorMsg, {
                                        title: 'Failed to update feature group',
                                        ttl: 15000
                                    });
                                    self.working = false;
                                });
                            growl.info("Updating featuregroup...", {title: 'Updating', ttl: 1000})
                        }, function (error) {
                            self.working = false;
                        });
                } else {
                    FeaturestoreService.updateFeaturegroupMetadata(self.projectId, self.featurestore, self.oldFeaturegroupId, featuregroupJson).then(
                        function (success) {
                            self.working = false;
                            $uibModalInstance.close(success)
                            growl.success("Feature group updated", {title: 'Success', ttl: 1000});
                        }, function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Failed to update feature group',
                                ttl: 15000
                            });
                            self.working = false;
                        });
                    growl.info("Updating featuregroup...", {title: 'Updating', ttl: 1000})
                }
            };

            /**
             * Function called when the user clicks the "Data Dependency" button, opens up a modal where the user
             * can select a dataset from a file-viewer.
             */
            self.selectDataDependency = function (index) {
                ModalService.selectFile('lg', '*', '', true).then(
                    function (success) {
                        self.dependencies[index] = success
                    },
                    function (error) {
                        // Users changed their minds.
                    });
            };

            /**
             * Function called when the user press "add dependency" button in the create-feature-group form
             * Adds a new dependency to the form
             */
            self.addNewDependency = function() {
                self.dependencies.push("");
            };

            /**
             * Function called when the user press "delete dependency" button in the create-feature-group form
             * Deletes a new dependency from the form
             */
            self.removeNewDependency = function(index) {
                self.dependencies.splice(index, 1);
            };

            /**
             * Function called when the user clicks the "Feature type" button, opens up a modal where the user
             * can select a pre-defined Hive type or define a custom type.
             *
             * @param feature the feature to define the type for
             */
            self.selectFeatureType = function (feature) {
                ModalService.selectFeatureType('lg').then(
                    function (success) {
                        feature.type = success
                    },
                    function (error) {
                        // Users changed their minds.
                    });
            };

            /**
             * Function called when the user press "add Feature" button in the create-feature-group form
             * Adds a new feature to the form
             */
            self.addNewFeature = function () {
                self.features.push({'name': '', 'type': '', 'description': "", primary: false});
                self.featuresNameWrongValue.push(1);
                self.featuresTypeWrongValue.push(1);
            };

            /**
             * Function called when the user press "delete Feature" button in the create-feature-group form
             * Deletes a new feature from the form
             */
            self.removeNewFeature = function (index) {
                self.features.splice(index, 1);
                self.featuresNameWrongValue.splice(index, 1);
                self.featuresTypeWrongValue.splice(index, 1);
            };


            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);