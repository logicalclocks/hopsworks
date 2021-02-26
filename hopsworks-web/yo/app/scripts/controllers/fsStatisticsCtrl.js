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
    .controller('FsStatisticsCtrl', ['$scope', 'growl', 'FsStatisticsService',
        function($scope, growl, FsStatisticsService) {
            var self = this;

            self.featureNames = [];
            self.selectedCorrelationFeatures = [];
            self.correlationChart = null;
            self.commitTimes = [];
            self.commitTime = null;
            self.stats = null;
            self.commitTimePrintFormat = "MMM D, YYYY hh:mm:ss A";
            self.entity = null;
            self.entityType = null;

            self.distributionPlotSettings = {
                chart: {
                  type: "pieChart",
                  height: 300,
                  width: 300,
                  donut: true,
                  x: function(d){return d['value'];},
                  y: function(d){return d['count'];},
                  showLabels: false,
                  duration: 500,
                  showLegend: false
                }
            };

            self.plotCorrelation = function(data) {
                var selectedFeatures = self.selectedCorrelationFeatures.map(function(f) {return f.name})
                if (data === 'all' ) {
                    selectedFeatures = self.featureNames.map(function(f) {return f.name})
                } else if (data === 'none') {
                    selectedFeatures = []
                }

                if (selectedFeatures.length == 0 && self.correlationChart != null) {
                    // If no features have been selected, just destroy everything
                    self.correlationChart.destroy()
                    self.correlationChart = null;
                    return;
                }

                // Filter all the features having the correlation field which have been selected by the user
                var correlations = self.stats.columns.filter(function(col) {
                    return selectedFeatures.includes(col.column)
                });

                var series = [];
                correlations.forEach(function(c) {
                    var data = [];

                    c.correlations.forEach(function(correlation) {
                        // Filter only the target features which have been selected by the user.
                        if (selectedFeatures.includes(correlation.column)) {
                            data.push({
                                x: correlation.column,
                                y: correlation.correlation
                            });
                        }
                    });

                    // sort data within series
                    data.sort(function(a, b) {
                        return a.x.localeCompare(b.x);
                    })

                    series.push({
                        'name': c.column,
                        'data': data
                    });
                })

                // sort series in same order as data within
                series.sort(function(a, b) {
                    return a.name.localeCompare(b.name)
                })

                var options = {
                    'series': series,
                    'chart': {
                        height: 700,
                        type: 'heatmap',
                    },
                    'dataLabels': {
                      enabled: false
                    },
                    colors: ["#1EB382"]
                }

                if (self.correlationChart == null) {
                    self.correlationChart = new ApexCharts(document.querySelector("#correlation"), options);
                    self.correlationChart.render();
                } else {
                    self.correlationChart.updateOptions(options, true);
                }
            };

            self.getFeatureCorrelationNames = function() {
                return self.stats.columns
                .filter(function(c) {
                    return "correlations" in c
                })
                .map(function(c) {
                    return {
                        'name': c.column,
                        'ticked': false
                    }
                });
            };

            self.prepareData = function(content) {
                self.stats = JSON.parse(content)
                self.stats.columns
                    .filter(function(column) {
                        return "histogram" in column
                    })
                    .forEach(function(column) {
                        column.histogram.sort(function(a, b) {
                            return a.value - b.value;
                        });
                    });
                self.featureNames = self.getFeatureCorrelationNames()
            }

            self.fetchStatistics = function() {
                FsStatisticsService.getStatisticsByCommit($scope.projectId,
                    self.entity.featurestoreId,
                    self.entity,
                    self.entityType,
                    new moment(self.commitTime, self.commitTimePrintFormat).valueOf())
                .then(
                    function (success) {
                        self.prepareData(success.data.items[0].content)

                        // if features were previously selected try reloading correlation with new data
                        if (self.correlationChart != null && self.selectedCorrelationFeatures.length != 0) {
                            var tmpSelected = self.selectedCorrelationFeatures.map(function(column) {return column.name;})
                            self.selectedCorrelationFeatures = []
                            // check if all previously selected features are still available
                            self.featureNames.forEach(function(feature) {
                                if (tmpSelected.includes(feature.name)) {
                                    self.selectedCorrelationFeatures.push({
                                        'name': feature.name,
                                        'ticked': true
                                    });
                                    feature.ticked = true;
                                }
                            })
                            if (self.selectedCorrelationFeatures.length != 0) {
                                self.plotCorrelation();
                            }
                        }
                    },
                    function (error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error fetching statistics for this entity and commit time',
                            ttl: 15000
                        });
                    });
            };

            self.initData = function() {
                // Fetch all the commits available
                FsStatisticsService.getStatistics($scope.projectId,
                    self.entity.featurestoreId,
                    self.entity,
                    self.entityType)
                    .then(
                        function (success) {
                            if (success.data.count > 0) {
                                self.commitTimes = success.data.items.map(function(item) {
                                    return moment(item.commitTime).format(self.commitTimePrintFormat).toString();
                                });
                            }
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error fetching statistics for this entity',
                                ttl: 15000
                            });
                        });

                // Fetch the latest available statistics
                FsStatisticsService.getStatisticsLastCommit($scope.projectId,
                    self.entity.featurestoreId,
                    self.entity,
                    self.entityType)
                    .then(
                        function (success) {
                            // should return only one item
                            if (success.data.count > 0) {
                                self.commitTime = moment(success.data.items[0].commitTime).format(self.commitTimePrintFormat).toString();
                                self.prepareData(success.data.items[0].content)
                            }
                        },
                        function (error) {
                            growl.error(error.data.errorMsg, {
                                title: 'Error fetching statistics for this entity',
                                ttl: 15000
                            });
                        });
            };

            self.init = function() {
                if ($scope.featureGroup !== null) {
                    self.entity = $scope.featureGroup;
                    self.entityType = "featuregroups";
                } else {
                    self.entity = $scope.trainingDataset
                    self.entityType = "trainingdatasets"
                }
                self.initData();
            };

        }]);