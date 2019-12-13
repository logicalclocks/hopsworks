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
 * Controller for the experimentDetailed view
 */
angular.module('hopsWorksApp')
    .controller('experimentDetailCtrl', ['$uibModalInstance', '$scope', '$location', 'ProjectService', 'ExperimentService',
        'growl', 'projectId', 'experiment',
        function($uibModalInstance, $scope, $location, ProjectService, ExperimentService, growl, projectId, experiment) {

            /**
             * Initialize controller state
             */
            var self = this;

            self.projectId = projectId;

            self.experiment = experiment;

            self.sortType = self.experiment.optimizationKey;

            if(self.experiment.optimizationKey) {
                if(self.experiment.direction === 'max') {
                    self.orderBy = 'desc';
                    self.reverse = true;
                } else if(self.experiment.direction === 'min') {
                    self.orderBy = 'asc';
                    self.reverse = false;
                }
            }

            self.showProvenanceView = false;

            self.loading = false;
            self.loadingText = "";

            self.modelLink = null;

            self.firstLoading = true;

            self.pageSize = 12;
            self.currentPage = 1;
            self.totalItems = 0;

            self.query = "";

            self.experimentsSubset = [];

            var startLoading = function(label) {
                if(label) {
                    self.loading = true;
                    self.loadingText = label;
                };
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };

            self.buildQuery = function() {
                var offset = self.pageSize * (self.currentPage - 1);
                var sortBy = "";
                self.query = "";

                if(self.sortType && self.orderBy) {
                    sortBy = ';sort_by=' + self.sortType + ':' + self.orderBy + ')';
                } else {
                    sortBy = ')';
                }

                if (self.showProvenanceView) {
                    self.query = "?expand=provenance"
                }
            };

            self.order = function () {
                if (self.reverse) {
                    self.orderBy = "desc";
                } else {
                    self.orderBy = "asc";
                }
            };

            self.sortBy = function(type) {
                if(self.sortType !== type) {
                    self.reverse = true;
                } else {
                    self.reverse = !self.reverse; //if true make it false and vice versa
                }
                self.sortType = type;
                self.order();
                self.getExperiment(null);
            };

            self.showProvenance = function() {
                self.showProvenanceView = !self.showProvenanceView;
                if(self.showProvenanceView) {
                    self.getExperiment('Fetching provenance information');
                }
            };

            self.getExperiment = function(loadingText) {
                self.buildQuery();
                self.buildModelLink();
                    startLoading(loadingText);
                    ExperimentService.get(self.projectId, self.experiment.id, self.query).then(
                        function(success) {
                            if(self.firstLoading) {
                                self.firstLoading = false;
                            }
                            stopLoading();
                            self.experiment = success.data;
                            self.buildModelLink();
                        },
                        function(error) {
                            stopLoading();
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {
                                    title: error.data.errorMsg,
                                    ttl: 8000
                                });
                            } else {
                                growl.error("", {
                                    title: error.data.errorMsg,
                                    ttl: 8000
                                });
                            }
                        });
            };

            self.countKeys = function(obj) {
                var count = 0;
                for (var prop in obj) {
                    if (obj.hasOwnProperty(prop)) {
                        ++count;
                    }
                }
                return count;
            };

            self.getNewExperimentPage = function() {
                self.getExperiment(null);
            };

            self.buildQuery();
            self.getExperiment('Loading detailed information');
            /**
             * Closes the modal
             */
            self.close = function() {
                $uibModalInstance.dismiss('cancel');
            };
        }
    ]);