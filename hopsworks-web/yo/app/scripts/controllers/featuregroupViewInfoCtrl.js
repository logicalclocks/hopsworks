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
 * Controller for the Featuregroup-Info view
 */
angular.module('hopsWorksApp')
    .controller('featuregroupViewInfoCtrl', ['$scope', 'FeaturestoreService', 'ProjectService',
        'JobService', 'StorageService', 'ModalService', '$location', 'growl',
        function ($scope, FeaturestoreService, ProjectService, JobService, StorageService, ModalService, $location, growl) {

            /**
             * Initialize controller state
             */
            var self = this;

            //Controller State
            self.tgState = false;
            self.projectName = null;
            self.projectId = null;
            self.selectedFeaturegroup = null;
            self.featuregroups = null;
            self.activeVersion = null;
            self.featurestore = null;
            self.settings = null;
            self.sampleWorking = false;
            self.sizeWorking = false;
            self.size = "Not fetched"
            self.offlineSchema ="Not fetched";
            self.onlineSchema = "Not fetched";
            self.pythonCode = ""
            self.scalaCode = ""
            self.schemaWorking = false;
            self.sampleWorking = false;
            self.offlineSampleColumns = []
            self.offlineSample = []
            self.onlineSampleColumns = []
            self.onlineSample = []

            self.customMetadata = null;

            /**
             * Get the API code to retrieve the featuregroup with the Python API
             */
            self.getPythonCode = function () {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_featuregroup('" + self.selectedFeaturegroup.name + "')"
                return codeStr
            };

            /**
             * Fetches a preview of the feature group from Hopsworks
             */
            self.fetchSample = function () {
                if(self.sampleWorking){
                    return
                }
                self.sampleWorking = true
                FeaturestoreService.getFeaturegroupSample(self.projectId, self.featurestore, self.selectedFeaturegroup).then(
                    function (success) {
                        self.sampleWorking = false;
                        if(success.data.offlineFeaturegroupPreview != null) {
                            var preProcessedOfflineSample = self.preprocessSample(success.data.offlineFeaturegroupPreview);
                            self.offlineSample = preProcessedOfflineSample[0]
                            self.offlineSampleColumns = preProcessedOfflineSample[1]
                        }
                        if(success.data.onlineFeaturegroupPreview != null) {
                            var preProcessedOnlineSample = self.preprocessSample(success.data.onlineFeaturegroupPreview);
                            self.onlineSample = preProcessedOnlineSample[0]
                            self.onlineSampleColumns = preProcessedOnlineSample[1]
                        }
                    }, function (error) {
                        self.sampleWorking = false;
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch data sample', ttl: 5000});
                    });
            };

            /**
             * Function for preprocessing the sample returned from the backend before visualizing it to the user
             *
             * @param rawSample the sample returned by the backend
             */
            self.preprocessSample = function (rawSample) {
                var columns = []
                var samples = []
                var sampleRow;
                var i;
                var j;
                if(rawSample.length > 0){
                    for (i = 0; i < rawSample[0].columns.length; i++) {
                        columns.push(self.removeTableNameFromColName(rawSample[0].columns[i].name))
                    }
                }
                for (i = 0; i < rawSample.length; i++) {
                    sampleRow = {}
                    for (j = 0; j < rawSample[i].columns.length; j++) {
                        sampleRow[self.removeTableNameFromColName(rawSample[i].columns[j].name)] = rawSample[i].columns[j].value
                    }
                    samples.push(sampleRow)
                }
                return [samples, columns]
            }

            /**
             * Previews are prepended with table name and a dot. Remove this prefix to make the UI more readable
             *
             * @param colName the colName to preprocess
             * @returns truncated colName
             */
            self.removeTableNameFromColName = function(colName) {
                return colName.replace(self.selectedFeaturegroup.name + "_" + self.selectedFeaturegroup.version + ".", "")
            }


            /**
             * Get the API code to retrieve the featuregroup with the Scala API
             */
            self.getScalaCode = function () {
                var codeStr = "import io.hops.util.Hops\n"
                codeStr = codeStr + "Hops.getFeaturegroup(\"" + self.selectedFeaturegroup.name + "\").read()"
                return codeStr
            };

            /**
             * Fetch schema from Hive by making a REST call to Hopsworks
             */
            self.fetchSchema = function () {
                if(self.schemaWorking){
                    return
                }
                self.schemaWorking = true

                FeaturestoreService.getFeaturegroupSchema(self.projectId, self.featurestore, self.selectedFeaturegroup).then(
                    function (success) {
                        self.schemaWorking = false;
                        self.offlineSchema = success.data.columns[0].value;
                        if(self.selectedFeaturegroup.onlineFeaturegroupEnabled){
                            self.onlineSchema = success.data.columns[1].value;
                        }
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch featuregroup schema', ttl: 5000});
                        self.schemaWorking = false;
                    });
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
             * Called when the launch-job button is pressed
             */
            self.launchJob = function (jobName) {
                JobService.setJobFilter(jobName);
                self.goToUrl("jobs")
            };

            /**
             * Send async request to hopsworks to calculate the inode size of the featuregroup
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchSize = function () {
                if(self.sizeWorking){
                    return
                }
                self.sizeWorking = true
                var request = {type: "inode", inodeId: self.selectedFeaturegroup.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.sizeWorking = false;
                    self.size = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch feature group size', ttl: 5000});
                    self.sizeWorking = false;
                });
            };

            /**
             * Get custom metadata for feature groups
             */
            self.fetchCustomMetadata = function() {
                FeaturestoreService.getFeaturegroupCustomMetadata(self.projectId, self.featurestore, self.selectedFeaturegroup).then(
                    function (success) {
                        self.customMetadata = success.data.items;
                    });
            };

            self.toggle = function(selectedFeatureGroup) {
                if(self.selectedFeaturegroup === null) {
                    self.tgState = true;
                } else if ((self.selectedFeaturegroup.id === selectedFeatureGroup.id) && self.tgState === true) {
                    self.tgState = false;
                    return;
                } else {
                    self.tgState = true;
                }
            }

            /**
             * Initialization function
             */
            self.view = function (projectId, projectName, featurestore, featuregroups, settings, toggle) {

                if(toggle) {
                    self.toggle(featuregroups.versionToGroups[featuregroups.activeVersion]);
                }

                self.selectedFeaturegroup = featuregroups.versionToGroups[featuregroups.activeVersion]

                self.projectId = projectId;
                self.projectName = projectName;
                self.featurestore = featurestore;
                self.featuregroups = featuregroups;
                self.activeVersion = featuregroups.activeVersion;
                self.settings = settings;

                self.cachedFeaturegroupType = self.settings.cachedFeaturegroupType;
                self.onDemandFeaturegroupType = self.settings.onDemandFeaturegroupType;
                self.pythonCode = self.getPythonCode();
                self.scalaCode = self.getScalaCode();

                self.featuregroupType = "";
                if(self.selectedFeaturegroup.featuregroupType === self.onDemandFeaturegroupType){
                    self.featuregroupType = "ON DEMAND";
                } else {
                    self.featuregroupType = "CACHED";
                    self.fetchSchema();
                    self.fetchSize();
                    self.fetchSample();
                    self.fetchCustomMetadata();
                }

            };

            /**
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.goToDataValidation = function () {
                StorageService.store("dv_featuregroup", self.selectedFeaturegroup);
                $location.path('project/' + self.projectId + "/featurestore/datavalidation");
            };

            /**
             * Called when the increment-version-featuregroup-button is pressed
             *
             */
            self.newFeaturegroupVersion = function () {
                var i;
                var maxVersion = -1;
                for (i = 0; i < self.featuregroups.versions.length; i++) {
                    if (self.featuregroups.versions[i] > maxVersion)
                        maxVersion = self.featuregroups.versions[i]
                }
                StorageService.store("featuregroup_operation", "NEW_VERSION");
                StorageService.store(self.projectId + "_featuregroup", self.featuregroups.versionToGroups[maxVersion]);
                self.goToUrl("newfeaturegroup")
            };

            /**
             * Called when the delete-featuregroup-button is pressed
             *
             */
            self.deleteFeaturegroup = function (featurestoreCtrl) {
                ModalService.confirm('md', 'Are you sure?',
                    'Are you sure that you want to delete version ' + self.selectedFeaturegroup.version + ' of the ' + self.selectedFeaturegroup.name + ' featuregroup? ' +
                                        'This action will delete the data and metadata and can not be undone.')
                    .then(function (success) {
                        FeaturestoreService.deleteFeaturegroup(self.projectId, self.featurestore, self.selectedFeaturegroup.id).then(
                            function (success) {
                                self.tgState = false;
                                featurestoreCtrl.getFeaturegroups(self.featurestore);
                                growl.success("Feature group deleted", {title: 'Success', ttl: 2000});
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to delete the feature group',
                                    ttl: 15000
                                });
                            });
                        growl.info("Deleting featuregroup...", {title: 'Deleting', ttl: 2000})
                    }, function (error) {});
            };

            /**
             * Goes to the edit page for updating a feature group
             *
             * @param featuregroup
             */
            self.updateFeaturegroup = function () {
                StorageService.store("featuregroup_operation", "UPDATE");
                StorageService.store(self.projectId + "_featuregroup", self.selectedFeaturegroup);
                self.goToUrl("newfeaturegroup")
            };

            /**
             * Called when the view-featuregroup-statistics button is pressed
             *
             * @param featuregroup
             */
            self.viewFeaturegroupStatistics = function () {
                ModalService.viewFeaturegroupStatistics('lg', self.projectId, self.selectedFeaturegroup, self.projectName,
                    self.featurestore, self.settings).then(
                    function (success) {
                    }, function (error) {
                    });
            };
        }]);

