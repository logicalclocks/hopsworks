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
    .controller('featuregroupViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'ProjectService',
        'JobService', '$location', 'growl', 'projectId', 'featuregroup', 'featurestore', 'settings',
        function ($uibModalInstance, $scope, FeaturestoreService, ProjectService, JobService, $location, growl,
                  projectId, featuregroup, featurestore, settings) {

            /**
             * Initialize controller state
             */
            var self = this;

            //Controller Input
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.featurestore = featurestore;
            self.settings = settings;

            //Controller State
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
            self.notFetchedSample = true;

            self.customMetadata = null;

            //Constants
            self.cachedFeaturegroupType = self.settings.cachedFeaturegroupType
            self.onDemandFeaturegroupType = self.settings.onDemandFeaturegroupType

            /**
             * Get the API code to retrieve the featuregroup with the Python API
             */
            self.getPythonCode = function (featuregroup) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_featuregroup('" + featuregroup.name + "')"
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
                FeaturestoreService.getFeaturegroupSample(self.projectId, self.featurestore, self.featuregroup).then(
                    function (success) {
                        self.sampleWorking = false;
                        self.notFetchedSample = false;
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
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch data sample', ttl: 5000});
                        self.sampleWorking = false;
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
                return colName.replace(self.featuregroup.name + "_" + self.featuregroup.version + ".", "")
            }


            /**
             * Get the API code to retrieve the featuregroup with the Scala API
             */
            self.getScalaCode = function (featuregroup) {
                var codeStr = "import io.hops.util.Hops\n"
                codeStr = codeStr + "Hops.getFeaturegroup(\"" + featuregroup.name + "\").read()"
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
                FeaturestoreService.getFeaturegroupSchema(self.projectId, self.featurestore, self.featuregroup).then(
                    function (success) {
                        self.schemaWorking = false;
                        self.offlineSchema = success.data.columns[0].value;
                        if(self.featuregroup.onlineFeaturegroupEnabled){
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
                self.close();
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
                var request = {type: "inode", inodeId: self.featuregroup.inodeId};
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
                FeaturestoreService.getFeaturegroupCustomMetadata(self.projectId, self.featurestore, self.featuregroup).then(
                    function (success) {
                        self.customMetadata = success.data.items;
                    });
            };

            /**
             * Initialization function
             */
            self.init = function () {
                self.formatCreated = self.formatDate(self.featuregroup.created)
                self.pythonCode = self.getPythonCode(self.featuregroup)
                self.scalaCode = self.getScalaCode(self.featuregroup)
                self.featuregroupType = ""
                if(self.featuregroup.featuregroupType === self.onDemandFeaturegroupType){
                    self.featuregroupType = "ON DEMAND"
                } else {
                    self.featuregroupType = "CACHED";
                    self.fetchSchema();
                    self.fetchSize();
                    self.fetchSample();
                    self.fetchCustomMetadata();
                }
            };

            /**
             * Format javascript date as string (YYYY-mm-dd HH:MM:SS)
             *
             * @param d date to format
             * @returns {string} formatted string
             */
            self.formatDate = function(javaDate) {
                var d = new Date(javaDate)
                var date_format_str = d.getFullYear().toString()+"-"+((d.getMonth()+1).toString().length==2?(d.getMonth()+1).toString():"0"+(d.getMonth()+1).toString())+"-"+(d.getDate().toString().length==2?d.getDate().toString():"0"+d.getDate().toString())+" "+(d.getHours().toString().length==2?d.getHours().toString():"0"+d.getHours().toString())+":"+((parseInt(d.getMinutes()/5)*5).toString().length==2?(parseInt(d.getMinutes()/5)*5).toString():"0"+(parseInt(d.getMinutes()/5)*5).toString())+":00";
                return date_format_str
            }

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
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
             * Helper function for redirecting to another project page
             *
             * @param serviceName project page
             */
            self.goToUrl = function (serviceName) {
                $location.path('project/' + self.projectId + '/' + serviceName);
            };

            self.init()
        }]);

