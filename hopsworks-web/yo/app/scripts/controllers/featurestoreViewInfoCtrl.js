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
 * Controller for the featurestore info view
 */
angular.module('hopsWorksApp')
    .controller('featurestoreViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'ProjectService',
        'growl', 'projectId', 'featurestore',
        function ($uibModalInstance, $scope, FeaturestoreService, ProjectService, growl, projectId, featurestore) {


            /**
             * Initialize controller state
             */
            var self = this;
            self.featurestore = featurestore
            self.sizeWorking = false;
            self.size = "Not fetched"

            /**
             * Convert bytes into bytes + suitable unit (e.g KB, MB, GB etc)
             *
             * @param fileSizeInBytes the raw byte number
             */
            self.sizeOnDisk = function (fileSizeInBytes) {
                return convertSize(fileSizeInBytes);
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
             * Send async request to hopsworks to calculate the inode size of the feature store
             * this can potentially be a long running operation if the directory is deeply nested
             */
            self.fetchSize = function () {
                if(self.sizeWorking){
                    return
                }
                self.sizeWorking = true
                var request = {id: self.projectId, type: "inode", inodeId: self.featurestore.inodeId};
                ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    self.sizeWorking = false;
                    self.size = self.sizeOnDisk(success.size)
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to fetch feature store size', ttl: 5000});
                    self.sizeWorking = false;
                });
            };

            /**
             * Initialize controller state
             */
            self.init = function () {
                self.projectId = projectId;
                self.featurestore = featurestore;
                self.table = []
                self.table.push({"property": "Id", "value": self.featurestore.featurestoreId})
                self.table.push({"property": "Name", "value": self.featurestore.featurestoreName})
                self.table.push({"property": "Description", "value": self.featurestore.featurestoreDescription})
                self.table.push({"property": "Created", "value": self.formatDate(self.featurestore.created)})
                self.table.push({"property": "HDFS path", "value": self.featurestore.hdfsStorePath})
            }

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()


        }]);

