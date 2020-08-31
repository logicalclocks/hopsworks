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

'use strict';

angular.module('hopsWorksApp')
    .controller('SharedWithCtrl', ['$uibModalInstance', '$routeParams', 'DataSetService', 'growl', 'datasetPath', 'dsType',
        function($uibModalInstance, $routeParams, DataSetService, growl, datasetPath, dsType) {
            var self = this;
            self.dataset = undefined;
            self.searchTerm = undefined;
            self.working = {};
            var projectId = $routeParams.projectID;
            var dataSetService = DataSetService(projectId);
            var init = function () {
                dataSetService.getDatasetStat(datasetPath, dsType).then( function (success) {
                    self.dataset = success.data;
                }, function (error) {
                    var errorMsg = (typeof error.data.usrMsg !== 'undefined')? error.data.usrMsg : "Failed to get" +
                        " shared with projects.";
                    growl.error(errorMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1234});
                });
            };
            init();

            self.unshareDataset = function (project) {
                self.working[project.name] = true;
                dataSetService.unshare(self.dataset.attributes.path, project.name, self.dataset.datasetType)
                    .then(function (success) {
                            init();
                            self.working[project.name] = false;
                        }, function (error) {
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                            } else {
                                growl.error("Failed to unshare.", {title: error.data.errorMsg, ttl: 8000});
                            }
                            self.working[project.name] = false;
                        });
            };

            self.changePermission = function (project) {
                self.working[project.name] = true;
                dataSetService.sharePermissions(self.dataset.attributes.path, project.name, project.permission, self.dataset.datasetType)
                    .then( function(success) {
                        self.working[project.name] = false;
                }, function (error) {
                    if (typeof error.data.usrMsg !== 'undefined') {
                        growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                    } else {
                        growl.error("Failed to change share permission.", {title: error.data.errorMsg, ttl: 8000});
                    }
                    self.working[project.name] = false;
                });
            };

            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);