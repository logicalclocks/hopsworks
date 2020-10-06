/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

 angular.module('hopsWorksApp')
    .controller('FeaturegroupPreviewCtrl', ['FeaturestoreService', 'growl', function(FeaturestoreService, growl) {
        var self = this;
        
        self.projectId;
        self.featurestore;
        self.fg;

        self.storageType = "offline";
        self.limit = 20;
        self.partitionSelected = "ALL PARTITIONS"; 
        self.partitions = [];

        // Display
        self.columns = [];
        self.rows = [];
        self.loading = true;
        self.loadingText = "Fetching feature group preview";


        self.fetchPreview = function(projectId, featurestore, fg) {
            if (fg == null) {
                return;
            }

            // Init 
            self.fg = fg;
            self.projectId = projectId;
            self.featurestore = featurestore;

            self.refreshPreview();
            self.fetchPartitions();
        };

        self.refreshPreview = function() {
            self.loading = true;
            self.columns = [];
            self.rows = [];

            var partitionName = null;
            if (self.storageType == "offline" && self.partitionSelected != "ALL PARTITIONS") {
                partitionName = self.partitionSelected;
            }

            var limit = 20;
            if (self.limit !== undefined && self.limit != null) {
                limit = self.limit;
            } else {
                self.limit = 20;
            }

            FeaturestoreService.getFeaturegroupSample(self.projectId, self.featurestore, 
                                                      self.fg, self.storageType, limit, partitionName).then(
                function(success) {
                    self.processData(success.data);
                    self.loading = false;
                }, function(error) {
                    self.loading = false;
                    growl.error(error.data.errorMsg, {
                        title: 'Failed to fetch the data preview',
                        ttl: 15000
                    });
                });
        };

        self.processData = function(data) {
            var rawSample = data.items;

            if(rawSample.length > 0){
                for (var i = 0; i < rawSample[0].row.length; i++) {
                    self.columns.push(rawSample[0].row[i].columnName)
                }
            }
            for (var i = 0; i < rawSample.length; i++) {
                var sampleRow = {}
                for (var j = 0; j < rawSample[i].row.length; j++) {
                    sampleRow[rawSample[i].row[j].columnName] = rawSample[i].row[j].columnValue
                }
                self.rows.push(sampleRow)
            }
        };

        self.fetchPartitions = function() {
            self.partitions = ["ALL PARTITIONS"];
            FeaturestoreService.getFeaturegroupPartitions(self.projectId, self.featurestore, self.fg).then(
                function(success) {
                    success.data.items.forEach(function(part) {
                        self.partitions.push(decodeURIComponent(part.partitionName));
                    });
                }, function(error) {
                    growl.error(error.data.errorMsg, {
                        title: 'Failed to fetch feature group partitions',
                        ttl: 15000
                    });
                }
            )
        }
    }]);