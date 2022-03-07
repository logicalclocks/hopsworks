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

/**
 * Controller for the Serving-Info view
 */
angular.module('hopsWorksApp')
    .controller('modelViewInfoCtrl', ['$uibModalInstance', '$scope', '$location', '$window', 'ProjectService',
        'StorageService', 'FeaturestoreService', 'growl', 'projectId', 'model',
        function ($uibModalInstance, $scope, $location, $window, ProjectService, StorageService, FeaturestoreService, growl, projectId, model) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.model = model;

            self.errorPrint = function(error) {
                if (typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                } else {
                    growl.error('', {title: error.data.errorMsg, ttl: 8000});
                }
            };

            self.hasTDSource = function() {
                return self.tdLink.name !== undefined && self.tdLink.name !== null;
            };

            self.getLinkInfo = function(link) {
                /** get project id from project name */
                ProjectService.getProjectInfo({projectName: link.projName}).$promise.then(
                    function (success1) {
                        link.projId = success1.projectId;
                        /** get featurestore of fg */
                        FeaturestoreService.getFeaturestores(link.projId).then(
                            function (success2) {
                                /** get the project's main featurestore */
                                const fs = success2.data.filter(function(fs) {
                                    return fs.projectName === link.projName;
                                });
                                if (fs.length === 1) {
                                    link.fsId = fs[0].featurestoreId;
                                } else {
                                    console.log('featurestore not in project');
                                    growl.error('featurestore not in project', {title: 'provenance error', ttl: 8000});
                                }
                            }, self.errorPrint);
                    }, self.errorPrint);
            };

            self.setTDLink = function(modelTD, linkInfoFunc, link) {
                link.featureStoreName = modelTD.featurestoreName;
                link.projName = link.featureStoreName.substring(0, link.featureStoreName.lastIndexOf("_"));
                link.name = modelTD.name;
                link.version = modelTD.version;
                linkInfoFunc(link);
            };

            var getSourceTDLinks = function (modelTD) {
                self.tdLink = {};
                /** td <- app <- model */
                if(modelTD) {
                    self.setTDLink(modelTD, self.getLinkInfo, self.tdLink);
                }
            };

            self.goToExperiment = function () {
                StorageService.store(self.projectId + "_experiment", model.experimentId);
                $location.path('project/' + self.projectId + '/experiments');
                self.close();
            };

            self.goToModelVersion = function () {
                $location.path('project/' + self.projectId + '/datasets/Models/' + model.name + '/' + model.version);
                self.close();
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            getSourceTDLinks(model.trainingDataset);
        }]);

