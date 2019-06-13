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
 * Controller for the featuregroup-statistics-view
 */
angular.module('hopsWorksApp')
    .controller('featuregroupStatisticsCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'ModalService',
        'growl', 'projectId', 'featuregroup', 'projectName', 'featurestore',
        function ($uibModalInstance, $scope, FeaturestoreService, ModalService, growl, projectId, featuregroup,
                  projectName, featurestore) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.projectName = projectName;
            self.featurestore = featurestore;

            /**
             * Function for preprocessing the spark descriptive statistics to a format that is suitable to
             * display in a modal (few columns to avoid horizontal scroll)
             */
            self.preProcessStats = function () {
                if(!self.stats)
                    return []
                var rows = []
                var i;
                var j;
                for (i = 0; i < self.stats.descriptiveStats.length; i++) {
                    for (j = 0; j < self.stats.descriptiveStats[i].metricValues.length; j++) {
                        var featureName = self.stats.descriptiveStats[i].featureName
                        var metric = self.stats.descriptiveStats[i].metricValues[j].metricName
                        var value = self.stats.descriptiveStats[i].metricValues[j].value
                        rows.push({"feature": featureName, "metric": metric, "value": value})
                    }
                }
                self.preProcessedStats = rows
            }
            if('descriptiveStatistics' in featuregroup){
                self.stats = featuregroup.descriptiveStatistics
                self.preProcessStats()
            } else {
                self.stats = []
                self.preprocessedStats = []
            }
            if('featureCorrelationMatrix' in featuregroup){
                self.featureMatrix = featuregroup.featureCorrelationMatrix
            } else {
                self.featureMatrix = null
            }
            if('featuresHistogram' in featuregroup){
                self.featureHistograms = featuregroup.featuresHistogram
            } else {
                self.featureHistograms = []
            }
            if('clusterAnalysis' in featuregroup){
                self.clusterAnalysis = featuregroup.clusterAnalysis
            } else {
                self.clusterAnalysis = null
            }
            //All values that are non null/undefined etc will evaluate to true in JS..
            if(!self.stats && !self.featureMatrix && !self.featureHistograms && !self.clusterAnalysis){
                self.noStats = true
            } else {
                self.noStats = false
            }


            /**
             * Setup the plotting options for a heatmap to visualize the feature correlations
             */
            self.setupHeatmap = function () {
                var series = []
                var i;
                var j;
                var featureOrder = []
                for (i = 0; i < self.featureMatrix.featureCorrelations.length; i++) {
                    featureOrder.push(self.featureMatrix.featureCorrelations[i].featureName)
                }
                for (i = 0; i < self.featureMatrix.featureCorrelations.length; i++) {
                    var data = new Array(featureOrder.length)
                    for (j = 0; j < self.featureMatrix.featureCorrelations[i].correlationValues.length; j++) {
                        var dataPoint = {
                            x: self.featureMatrix.featureCorrelations[i].correlationValues[j].featureName,
                            y: self.featureMatrix.featureCorrelations[i].correlationValues[j].correlation
                        }
                        var index = featureOrder.indexOf(self.featureMatrix.featureCorrelations[i].correlationValues[j].featureName)
                        data[index] = dataPoint
                    }
                    var serie = {
                        name: self.featureMatrix.featureCorrelations[i].featureName,
                        data: data.reverse()
                    }
                    series.push(serie)
                }
                var heatmapConfig = {
                    chart: {
                        height: 450 + ((series.length)/10)*150,
                        width: 450 + ((series.length)/10)*150,
                        type: 'heatmap',
                    },
                    dataLabels: {
                        enabled: false
                    },
                    colors: ["#008FFB"],
                    series:  series,
                    title: {
                        text: "Feature correlation for feature group: " + self.featuregroup.name,
                        align: 'center',
                        margin: 10,
                        offsetX: 0,
                        offsetY: 0,
                        floating: false,
                        style: {
                            fontSize:  '16px',
                            color:  '#263238'
                        }
                    }
                }
                self.heatmapOptions = heatmapConfig
            };

            /**
             * Setup plotting options for feature histograms
             */
            self.setupHistograms = function () {
                var histogramPlotsData = []
                var histogramPlotsOptions = []
                var histogramFeatures = []
                var i;
                var j;
                for (i = 0; i < self.featureHistograms.featureDistributions.length; i++) {
                    var barchartOptions = {
                        chart: {
                            type: 'historicalBarChart',
                            height: 450,
                            margin : {
                                top: 20,
                                right: 20,
                                bottom: 65,
                                left: 50
                            },
                            x: function(d){return d[0];},
                            y: function(d){return d[1];},
                            showValues: true,
                            duration: 100,
                            xAxis: {
                                axisLabel: 'Bins',
                                tickFormat: function(d) {
                                    return d3.format(',.1f')(d);
                                },
                                rotateLabels: 30,
                                showMaxMin: false
                            },
                            yAxis: {
                                axisLabel: 'Frequency',
                                axisLabelDistance: -10,
                                tickFormat: function(d){
                                    return d
                                }
                            },
                            tooltip: {
                                keyFormatter: function(d) {
                                    return d3.format(',.1f')(d);
                                }
                            },
                            zoom: {
                                enabled: true,
                                scaleExtent: [1, 10],
                                useFixedDomain: false,
                                useNiceScale: false,
                                horizontalOff: false,
                                verticalOff: true,
                                unzoomEventType: 'dblclick.zoom'
                            }
                        },
                        title: {
                            enable: true,
                            text: 'Distribution of values for feature: ' + self.featureHistograms.featureDistributions[i]["featureName"]},
                        css: {
                            'text-align': 'center',
                            'color': 'black'
                        }
                    }
                    var barChartDataKey = "Quantity"
                    var barChartDataBar = true
                    var barChartValues = []
                    for (j = 0; j < self.featureHistograms.featureDistributions[i].frequencyDistribution.length; j++) {
                        var bin = self.featureHistograms.featureDistributions[i].frequencyDistribution[j].bin
                        var frequency = self.featureHistograms.featureDistributions[i].frequencyDistribution[j].frequency
                        barChartValues.push([parseFloat(bin), frequency])
                    }
                    var barchartData = [{
                        "key": barChartDataKey,
                        "bar": barChartDataBar,
                        "values": barChartValues
                    }]
                    histogramPlotsOptions.push(barchartOptions)
                    histogramPlotsData.push(barchartData)
                    histogramFeatures.push(self.featureHistograms.featureDistributions[i]["featureName"])
                }
                self.histogramPlotsData = histogramPlotsData
                self.histogramPlotsOptions = histogramPlotsOptions
                self.histogramFeatures = histogramFeatures
            }

            /**
             * Setup plotting options for cluster analysis
             */
            self.setupClusterAnalysis = function () {
                var clusterAnalysisPlotOption = {
                    chart: {
                        type: 'scatterChart',
                        height: 450,
                        color: d3.scale.category10().range(),
                        scatter: {
                            onlyCircles: true
                        },
                        showDistX: false,
                        showDistY: false,
                        tooltipContent: function(key) {
                            return '<h3>' + key + '</h3>';
                        },
                        duration: 100,
                        xAxis: {
                            axisLabel: 'Dimension 1',
                            tickFormat: function(d){
                                return null
                            }
                        },
                        yAxis: {
                            axisLabel: 'Dimension 2',
                            tickFormat: function(d){
                                return null
                            },
                            axisLabelDistance: -5
                        },
                        zoom: {
                            enabled: true,
                            scaleExtent: [1, 10],
                            useFixedDomain: false,
                            useNiceScale: false,
                            horizontalOff: false,
                            verticalOff: true,
                            unzoomEventType: 'dblclick.zoom'
                        }
                    },
                    title: {
                        enable: true,
                        text: 'Cluster analysis for feature group: ' + self.featuregroup.name},
                    css: {
                        'text-align': 'center',
                        'color': 'black'
                    }
                };
                var data = []
                var clusters = self.clusterAnalysis.clusters
                var points = self.clusterAnalysis.dataPoints
                var distinctClusters = []
                var pointToCluster = {}
                var i;
                for (i = 0; i < clusters.length; i++) {
                    if (distinctClusters.indexOf(clusters[i].cluster) === -1)
                        distinctClusters.push(clusters[i].cluster)
                    pointToCluster[clusters[i].datapointName] = clusters[i].cluster
                }
                var pointsPerCluster = {}
                for (var i = 0; i < distinctClusters.length; i++) {
                    pointsPerCluster[distinctClusters[i]] = []
                }
                for (var i = 0; i < points.length; i++) {
                    var point = {
                        x: points[i].firstDimension,
                        y: points[i].secondDimension,
                        shape: "circle",
                        size: 1
                    }
                    pointsPerCluster[pointToCluster[points[i].datapointName]].push(point)
                }
                for (var i = 0; i < distinctClusters.length; i++) {
                    var clusterPoints = {
                        key: "cluster " + distinctClusters[i],
                        values: pointsPerCluster[distinctClusters[i]]
                    }
                    data.push(clusterPoints)
                }
                self.clusterAnalysisPlotOptions = clusterAnalysisPlotOption
                self.clusterAnalysisPlotData = data
            };


            /**
             * Opens a modal with a feature distribution plot
             */
            self.showFeatureHistogram = function(plotOptions, plotData){
                var statisticType = 3
                var statisticData = {"plotOptions" : plotOptions, "plotData": plotData}
                ModalService.viewFeaturestoreStatistic('lg', self.projectId, featuregroup, statisticType, statisticData, false).then(
                    function (success) {
                    }, function (error) {
                    });
            }

            /**
             * Opens a modal with the descriptive statistics
             */
            self.showDescriptiveStatistics = function(){
                var statisticType= 0
                ModalService.viewFeaturestoreStatistic('lg', self.projectId, featuregroup, statisticType, self.preProcessedStats, false).then(
                    function (success) {
                    }, function (error) {
                    });
            }

            /**
             * Opens a modal with the feature correlation plot
             */
            self.showCorrelationMatrix = function(){
                var statisticType= 1
                ModalService.viewFeaturestoreStatistic('lg', self.projectId, featuregroup, statisticType, self.heatmapOptions, false).then(
                    function (success) {
                    }, function (error) {
                    });
            }

            /**
             * Opens a modal with the cluster analysis plot
             */
            self.showClusterAnalysis = function(){
                var statisticType= 2
                var statisticData = {"plotOptions" : self.clusterAnalysisPlotOptions, "plotData": self.clusterAnalysisPlotData}
                ModalService.viewFeaturestoreStatistic('lg', self.projectId, featuregroup, statisticType, statisticData, false).then(
                    function (success) {
                    }, function (error) {
                    });
            }

            /**
             * Opens the modal for updating featuregroup statistics
             */
            self.updateStatistics = function(){
                ModalService.updateFeaturestoreStatistic('lg', self.projectId, featuregroup, false, self.projectName,
                    self.featurestore).then(
                    function (success) {
                        $uibModalInstance.close(success)
                    }, function (error) {
                    });
            }

            /**
             * Initialize plot configs
             */
            self.init = function () {
                if(self.featureMatrix !== null)
                    self.setupHeatmap()
                if(self.featureHistograms)
                    self.setupHistograms()
                if(self.clusterAnalysis !== null)
                    self.setupClusterAnalysis()
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.init()

        }]);
