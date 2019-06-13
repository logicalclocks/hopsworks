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
        'JobService', '$location', 'growl', 'projectId', 'featuregroup', 'featurestore', 'jobs',
        function ($uibModalInstance, $scope, FeaturestoreService, ProjectService, JobService, $location, growl,
                  projectId, featuregroup, featurestore, jobs) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.featurestore = featurestore;
            self.jobs = jobs;
            self.sampleWorking = false;
            self.sizeWorking = false;
            self.size = "Not fetched"
            self.schema ="Not fetched";
            self.code = ""
            self.table = []

            /**
             * Get the API code to retrieve the featuregroup
             */
            self.getCode = function (featuregroup, featurestore) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_featuregroup(\n"
                codeStr = codeStr + "'" + featuregroup.name + "'"
                codeStr = codeStr + ",\nfeaturestore="
                codeStr = codeStr + "'" + featurestore.featurestoreName + "'"
                codeStr = codeStr + ",\nfeaturegroup_version="
                codeStr = codeStr + featuregroup.version
                codeStr = codeStr + ")"
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
                        self.schema = success.data.columns[0].value;
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
             * Check if a job of a featuregroup in the featurestore belongs to this project's jobs or another project
             *
             * @param jobId the jobId to lookup
             */
            self.isJobLocal = function (jobId) {
                var i;
                var jobFoundBool = false;
                for (i = 0; i < self.jobs.length; i++) {
                    if (self.jobs[i].id === jobId) {
                        jobFoundBool = true
                    }
                }
                return jobFoundBool
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
             * Initialization function
             */
            self.init= function () {
                self.code = self.getCode(self.featuregroup, self.featurestore)
                self.table.push({"property": "Id", "value": self.featuregroup.id})
                self.table.push({"property": "Name", "value": self.featuregroup.name})
                self.table.push({"property": "Version", "value": self.featuregroup.version})
                self.table.push({"property": "Description", "value": self.featuregroup.description})
                self.table.push({"property": "Featurestore", "value": self.featuregroup.featurestoreName})
                self.table.push({"property": "HDFS path", "value": self.featuregroup.hdfsStorePaths[0]})
                self.table.push({"property": "Creator", "value": self.featuregroup.creator})
                self.table.push({"property": "Created", "value": self.formatDate(self.featuregroup.created)})
                self.table.push({"property": "Job", "value": "-"})
                self.table.push({"property": "Last Computed", "value": "-"})
                self.table.push({"property": "API Retrieval Code", "value": self.code})
                self.fetchSchema()
                self.fetchSize()
            };

            /**
             * Check if a row is a regular one or need special rendering
             */
            self.isRegularRow = function(property) {
                if (property == "API Retrieval Code" || property == "Job" || property == "Last Computed"){
                    return false
                }
                return true
            }

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

