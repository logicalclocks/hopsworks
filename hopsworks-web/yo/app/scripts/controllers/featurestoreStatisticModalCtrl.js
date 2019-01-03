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
 * Controller for the statistic-view
 */
angular.module('hopsWorksApp')
    .controller('featurestoreStatisticModalCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl', 'projectId', 'featuregroup', 'statisticType', 'statisticData', 'trainingDataset',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, featuregroup, statisticType, statisticData, trainingDataset) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.pageSize = 1000; //don't show pagination controls, let the user scroll instead
            self.reverse = false;
            self.statisticType = statisticType
            self.statisticData = statisticData
            self.trainingDataset = trainingDataset

            /**
             * Function to start the loading screen
             *
             * @param label the text to show to the user while loading
             */
            self.startLoading = function (label) {
                self.loading = true;
                self.loadingText = label;
            };

            /**
             * Function to stop the loading screen
             */
            self.stopLoading = function () {
                self.loading = false;
                self.loadingText = "";
            };

            /**
             * Render heatmap to visualize feature correlations
             */
            self.renderHeatmap = function () {
                self.startLoading("Rendering correlation matrix..")
                $uibModalInstance.rendered.then(function () {
                    self.heatmapChart = new ApexCharts(
                        document.querySelector("#heatmapchart"),
                        self.statisticData
                    );
                    self.heatmapChart.render()
                    setTimeout(function(){ self.stopLoading()}, 150);
                });
            };

            /**
             * Initialize UI with plots
             */
            self.init = function () {
                if (self.statisticType === 1) {
                    self.renderHeatmap()
                }
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()

        }]);
