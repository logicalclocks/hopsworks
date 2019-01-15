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
 * Controller for the featuregroup-dependencies-view
 */
angular.module('hopsWorksApp')
    .controller('featuregroupDependenciesCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl', 'projectId', 'featuregroup',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, featuregroup) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.pageSize = 1000; //don't show pagination controls, let the user scroll instead
            self.reverse = false;

            /**
             * Function for setting up the dependency chart that is to be rendered
             */
            self.setupDependencyChart = function () {
                var rows = []
                var index = 1
                rows.push(
                {"c": [
                    {"v": index, "f": self.featuregroup.name},
                    {"v": ""}
                ]})
                index = index+1
                var i
                for (i = 0; i < self.featuregroup.dependencies.length; i++) {
                    rows.push(
                        {"c": [
                            {"v": index, "f": self.featuregroup.dependencies[i].path},
                            {"v": 1}
                        ]}
                    )
                    index = index+1
                }

                self.dependencyChartData = {
                    "cols" : [
                        {"label": "Name", "pattern": "", "type": "string"},
                        {"label": "Dependency", "pattern": "", "type": "string"}
                    ],
                    "rows" : rows
                }

                self.dependencyChart = {
                    type: "OrgChart",
                    data: self.dependencyChartData
                };
            };

            /**
             * Initializes the plots
             */
            self.init = function () {
                self.setupDependencyChart()
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()

        }]);

