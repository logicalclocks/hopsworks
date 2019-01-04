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
 * Controller for the Training Dataset-Info view
 */
angular.module('hopsWorksApp')
    .controller('trainingDatasetViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'ProjectService',
        'growl', 'projectId', 'trainingDataset', 'featurestore',
        function ($uibModalInstance, $scope, FeaturestoreService, ProjectService, growl, projectId, trainingDataset, featurestore) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.trainingDataset = trainingDataset;
            self.featurestore = featurestore;
            self.sizeWorking = false;
            self.size = "Not fetched"
            self.code = ""
            self.table = []

            /**
             * Get the API code to retrieve the featuregroup
             */
            self.getCode = function (trainingDataset, featurestore) {
                var codeStr = "from hops import featurestore\n"
                codeStr = codeStr + "featurestore.get_training_dataset_path(\n"
                codeStr = codeStr + "'" + trainingDataset.name + "',\n"
                codeStr = codeStr + "featurestore='" + trainingDataset.featurestoreName + "',\n"
                codeStr = codeStr + "training_dataset_version=" + trainingDataset.version + "\n"
                codeStr = codeStr + ")"
                return codeStr
            };

            /**
             * Initialization function
             */
            self.init= function () {
                self.code = self.getCode(self.trainingDataset, self.featurestore)
                self.table.push({"property": "Id", "value": self.trainingDataset.id})
                self.table.push({"property": "Name", "value": self.trainingDataset.name})
                self.table.push({"property": "Version", "value": self.trainingDataset.version})
                self.table.push({"property": "Size", "value": self.trainingDataset.size})
                self.table.push({"property": "Description", "value": self.trainingDataset.description})
                self.table.push({"property": "Featurestore", "value": self.trainingDataset.featurestoreName})
                self.table.push({"property": "HDFS path", "value": self.trainingDataset.hdfsStorePath})
                self.table.push({"property": "Creator", "value": self.trainingDataset.creator})
                self.table.push({"property": "Created", "value": self.formatDate(self.trainingDataset.created)})
                self.table.push({"property": "Data format", "value": self.trainingDataset.dataFormat})
                self.table.push({"property": "API Retrieval Code", "value": self.code})
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
             * Convert bytes into bytes + suitable unit (e.g KB, MB, GB etc)
             *
             * @param fileSizeInBytes the raw byte number
             */
            self.sizeOnDisk = function (fileSizeInBytes) {
                return convertSize(fileSizeInBytes);
            };

            /**
             * Send async request to hopsworks to calculate the inode size of the training dataset
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchSize = function () {
                if(self.sizeWorking){
                    return
                }
                self.sizeWorking = true
                var request = {id: self.projectId, type: "inode", inodeId: self.trainingDataset.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.sizeWorking = false;
                    self.size = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch training dataset size', ttl: 5000});
                    self.sizeWorking = false;
                });
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()
        }]);

