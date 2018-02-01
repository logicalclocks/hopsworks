/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
         .controller('VizopsStreamingCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'VizopsService',

            function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;

                self.appId;
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now = null; // is the application running now?
                // array of dictionaries: self.executorInfo.entry[executor].value[0: container, 1: hostname, 2: nm vcores]
                self.executorInfo;
                self.nbExecutors;
                // a list of objects holding as a key the hostname and value the number of executors
                self.hostnames = {};
                self.nrReceivers = 0;

                self.optionsLastReceivedBatchRecords = vizopsStreamingLastReceivedBatchRecordsOptions();
                self.optionsLastCompletedTotalDelay = vizopsStreamingTotalDelayOptions();
                self.optionsTotalReceivedProcessedRecords = vizopsStreamingTotalReceivedProcessedRecordsOptions();
                self.optionsBatchStatistics = vizopsStreamingBatchStatisticsOptions();

                self.templateLastReceivedBatchRecords = [];
                self.templateLastCompletedTotalDelay = [];
                self.templateTotalReceivedProcessedRecords = [];
                self.templateBatchStatistics = [];

                self.startTimeMap = {
                    'receivers': -1,
                    'lastReceivedBatchRecords': -1,
                    'lastCompletedTotalDelay': -1,
                    'totalReceivedProcessedRecords': -1,
                    'batchStatistics': -1
                };

                self.hasLoadedOnce = {
                    'receivers': false,
                    'lastReceivedBatchRecords': false,
                    'lastCompletedTotalDelay': false,
                    'totalReceivedProcessedRecords': false,
                    'batchStatistics': false
                };

                self.lastMeasurement = {
                    'receivers': [],
                    'lastReceivedBatchRecords': [],
                    'lastCompletedTotalDelay': [],
                    'totalReceivedProcessedRecords': [],
                    'batchStatistics': []
                };

                var updateReceiverTasksCard = function() {
                    if (!self.now && self.hasLoadedOnce['receivers'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('receivers') +
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'last(StreamingMetrics_streaming_receivers) as receivers',
                                             'spark', tags).then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['receivers'] = _getLastTimestampFromSeries(newData);

                            self.nrReceivers = +newData.values[0].split(' ')[1];

                            self.hasLoadedOnce['receivers'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching nrReceivers(streaming) metric.', ttl: 10000});
                        }
                    );
                };

                var updateLastReceivedBatchRecords = function() {
                    if (!self.now && self.hasLoadedOnce['lastReceivedBatchRecords']) {
                        if(self.lastMeasurement['lastReceivedBatchRecords'].length > 0) {
                            self.templateLastReceivedBatchRecords[0].values.push(self.lastMeasurement['lastReceivedBatchRecords'][0]);

                            self.lastMeasurement['lastReceivedBatchRecords'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('lastReceivedBatchRecords');

                    VizopsService.getMetrics('graphite', 'last(StreamingMetrics_streaming_lastReceivedBatch_records)', 'spark',
                        tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['lastReceivedBatchRecords'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['lastReceivedBatchRecords'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['lastReceivedBatchRecords'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    } else {
                                        self.templateLastReceivedBatchRecords[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    }
                                }

                                self.hasLoadedOnce['lastReceivedBatchRecords'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching lastReceivedBatchRecords(streaming) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateLastCompletedTotalDelay = function() {
                    if (!self.now && self.hasLoadedOnce['lastCompletedTotalDelay']) {
                        if (self.lastMeasurement['lastCompletedTotalDelay'].length > 0) {
                            self.templateLastCompletedTotalDelay[0].values.push(self.lastMeasurement['lastCompletedTotalDelay'][0]);

                            self.lastMeasurement['lastCompletedTotalDelay'] = [];
                        }
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('lastCompletedTotalDelay');

                    VizopsService.getMetrics('graphite', 'last(StreamingMetrics_streaming_lastCompletedBatch_totalDelay)', 'spark',
                        tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['lastCompletedTotalDelay'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['lastCompletedTotalDelay'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['lastCompletedTotalDelay'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    } else {
                                        self.templateLastCompletedTotalDelay[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    }
                                }

                                self.hasLoadedOnce['lastCompletedTotalDelay'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching LastCompletedTotalDelay(streaming) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateTotalReceivedProcessedRecords = function () {
                    if (!self.now && self.hasLoadedOnce['totalReceivedProcessedRecords']) {
                        if (self.lastMeasurement['totalReceivedProcessedRecords'].length > 0) {
                            self.templateTotalReceivedProcessedRecords[0].values.push(self.lastMeasurement['totalReceivedProcessedRecords'][0]);
                            self.templateTotalReceivedProcessedRecords[1].values.push(self.lastMeasurement['totalReceivedProcessedRecords'][1]);

                            self.lastMeasurement['totalReceivedProcessedRecords'] = [];
                        }
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalReceivedProcessedRecords');

                    VizopsService.getMetrics('graphite', 'last(StreamingMetrics_streaming_totalReceivedRecords),' +
                                             'last(StreamingMetrics_streaming_totalProcessedRecords)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['totalReceivedProcessedRecords'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['totalReceivedProcessedRecords'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['totalReceivedProcessedRecords'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['totalReceivedProcessedRecords'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    } else {
                                        self.templateTotalReceivedProcessedRecords[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.templateTotalReceivedProcessedRecords[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                    }
                                }

                                self.hasLoadedOnce['totalReceivedProcessedRecords'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching totalReceivedProcessedRecords(streaming) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateBatchStatistics = function () {
                    if (!self.now && self.hasLoadedOnce['batchStatistics']) {
                        if (self.lastMeasurement['batchStatistics'].length > 0) {
                            self.templateBatchStatistics[0].values.push(self.lastMeasurement['batchStatistics'][0]);
                            self.templateBatchStatistics[1].values.push(self.lastMeasurement['batchStatistics'][1]);
                            self.templateBatchStatistics[2].values.push(self.lastMeasurement['batchStatistics'][2]);

                            self.lastMeasurement['batchStatistics'] = [];
                        }
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('batchStatistics');

                    VizopsService.getMetrics('graphite', 'last(StreamingMetrics_streaming_runningBatches),' +
                                                         'last(StreamingMetrics_streaming_totalCompletedBatches),' +
                                                         'last(StreamingMetrics_streaming_unprocessedBatches)',
                            'spark', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['batchStatistics'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['batchStatistics'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['batchStatistics'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['batchStatistics'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                        self.lastMeasurement['batchStatistics'].push({'x': +splitEntry[0],'y': +splitEntry[3]});
                                    } else {
                                        self.templateBatchStatistics[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.templateBatchStatistics[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                        self.templateBatchStatistics[2].values.push({'x': +splitEntry[0], 'y': +splitEntry[3]});
                                    }
                                }

                                self.hasLoadedOnce['batchStatistics'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching batchStatistics(streaming) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updateReceiverTasksCard();
                    updateLastReceivedBatchRecords();
                    updateLastCompletedTotalDelay();
                    updateTotalReceivedProcessedRecords();
                    updateBatchStatistics();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                        if (self.startTimeMap.hasOwnProperty(key)) {
                            self.startTimeMap[key] = self.startTime;
                            self.hasLoadedOnce[key] = false;
                        }
                    }

                    self.templateLastReceivedBatchRecords = vizopsStreamingLastReceivedBatchRecordsTemplate();
                    self.templateLastCompletedTotalDelay = vizopsStreamingTotalDelayTemplate();
                    self.templateTotalReceivedProcessedRecords = vizopsStreamingTotalReceivedProcessedRecordsTemplate();
                    self.templateBatchStatistics = vizopsStreamingBatchStatisticsTemplate();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didnt use groupBy calls then it would be enough to upper limit the time with now()
                    var limits = 'time >= ' + self.startTimeMap[graphName] + 'ms';

                    if (!self.now) {
                        limits += ' and time < ' + self.endTime + 'ms';
                    } else {
                        limits += ' and time < now()';
                    }

                    return limits;
                };

                var _extractHostnameInfoFromResponse = function(response) {
                    // get the unique host names
                    var hosts = _.uniq(response.entry.map(function(item) { return item.value[1]; }));

                    var result = {};
                    for(var i = 0; i < hosts.length; i++) {
                        result[hosts[i]] = [];
                    }

                    // and add the executors running on them
                    for(var i = 0; i < response.entry.length; i++) {
                        result[response.entry[i].value[1]].push(response.entry[i].key);
                    }

                    return result;
                };

                var init = function() {
                    self.appId = VizopsService.getAppId();

                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                        function(success) {
                            var info = success.data;

                            self.nbExecutors = info.nbExecutors;
                            self.executorInfo = info.executorInfo;
                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);

                            if (self.now) { // only schedule the interval if app is running
                                self.appinfoInterval = $interval(function() { // update appinfo data
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            const info = success.data;

                                            self.nbExecutors = info.nbExecutors;
                                            self.executorInfo = info.executorInfo;
                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            // get the unique hostnames and the number of executors running on them
                                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(streaming).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                            resetGraphs();
                            updateMetrics();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                self.poller = $interval(function () {
                    updateMetrics();
                }, 10000);

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                  $interval.cancel(self.appinfoInterval);
                });

                $scope.$watch(function() { return VizopsService.getGroupByInterval(); }, function(newVal, oldVal) {
                    /* This happens only the first time, Service fires up this event the first time
                     as we set the group by interval to 10s but the UI has already started updating with that value
                     First time - newVal 10s, oldVal 10s, subsequent - newVal XX oldVal YY
                     */
                    if (newVal === oldVal) return;

                    resetGraphs();
                    updateMetrics();
                }, true);
            }
        ]
    );