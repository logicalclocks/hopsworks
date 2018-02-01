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
         .controller('VizopsDriverCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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

                self.nbExecutorsOnDriverHost = 0;
                self.maxUsedDriverMem = "0.0";
                self.maxAvailableDriverMem = "0.0";
                self.uiShuffleRead = 0.0;
                self.uiShuffleWrite = 0.0;

                self.optionsMemorySpace = vizopsMemorySpaceDriverOptions();
                self.optionsVCPU = vizopsVCPUDriverOptions();
                self.optionsRDDCacheDiskSpill = vizopsRDDCacheDiskSpillOptions();
                self.optionsGCTime = vizopsGCTimeOptions();

                self.templateMemorySpace = [];
                self.templateVCPU = [];
                self.templateRDDCacheDiskSpill = [];
                self.templateGCTime = [];

                self.startTimeMap = {
                    'vcpuUsage': -1,
                    'memorySpace': -1,
                    'maxMemory': -1,
                    'rddCacheDiskSpill': -1,
                    'gcTime': -1,
                    'totalShuffle': -1
                };

                self.hasLoadedOnce = {
                    'vcpuUsage': false,
                    'memorySpace': false,
                    'maxMemory': false,
                    'rddCacheDiskSpill': false,
                    'gcTime': false,
                    'totalShuffle': false
                };

                self.lastMeasurement = {
                    'vcpuUsage': [],
                    'memorySpace': [],
                    'maxMemory': [],
                    'rddCacheDiskSpill': [],
                    'gcTime': [],
                    'totalShuffle': []
                };

                var updateMemorySpace = function() {
                    if (!self.now && self.hasLoadedOnce['memorySpace']) {
                        if(self.lastMeasurement['memorySpace'].length > 0) {
                            self.templateMemorySpace[0].values.push(self.lastMeasurement['memorySpace'][0]);
                            self.templateMemorySpace[1].values.push(self.lastMeasurement['memorySpace'][1]);

                            self.lastMeasurement['memorySpace'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('memorySpace') +
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'mean(heap_used), max(heap_used)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['memorySpace'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['memorySpace'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['memorySpace'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['memorySpace'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    } else {
                                        self.templateMemorySpace[0].values.push({
                                            'x': +splitEntry[0],
                                            'y': +splitEntry[1]
                                        });
                                        self.templateMemorySpace[1].values.push({
                                            'x': +splitEntry[0],
                                            'y': +splitEntry[2]
                                        });
                                    }
                                }

                                self.hasLoadedOnce['memorySpace'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching memorySpaceDriver metrics.', ttl: 10000});
                        }
                    );
                };

                var updateGraphVCPU = function() {
                    if (!self.now && self.hasLoadedOnce['vcpuUsage']) {
                        if(self.lastMeasurement['vcpuUsage'].length > 0) {
                            self.templateVCPU[0].values.push(self.lastMeasurement['vcpuUsage'][0]);

                            self.lastMeasurement['vcpuUsage'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }


                    var tags = 'source =~ /' + self.executorInfo.entry[0].value[0] + '/' + ' and ' + _getTimestampLimits('vcpuUsage')
                               + ' and MilliVcoreUsageIMinMilliVcores <= ' + (+self.executorInfo.entry[0].value[2]*1000);

                    VizopsService.getMetrics('graphite',
                                               'mean(MilliVcoreUsageIMinMilliVcores)/' + (+self.executorInfo.entry[0].value[2]*1000),
                                               'nodemanager', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['vcpuUsage'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['vcpuUsage'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['vcpuUsage'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    } else {
                                        self.templateVCPU[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    }
                                }

                                self.hasLoadedOnce['vcpuUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching VcpuUsageDriver metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMaxMemory = function() {
                    if (!self.now && self.hasLoadedOnce['maxMemory'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and service =~ /driver/';

                    VizopsService.getMetrics('graphite',
                        'max(heap_used), heap_max', 'spark', tags).then(
                    function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['maxMemory'] = _getLastTimestampFromSeries(newData);

                            self.maxUsedDriverMem = d3.format(".4s")(newData.values[0].split(' ')[1]);
                            self.maxAvailableDriverMem = d3.format(".4s")(newData.values[0].split(' ')[2]);

                            self.hasLoadedOnce['maxMemory'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching maxMemoryDriver metric.', ttl: 10000});
                        }
                    );
                };

                var updateRDDCacheDiskSpill = function() {
                    if (!self.now && self.hasLoadedOnce['rddCacheDiskSpill']) {
                        if(self.lastMeasurement['rddCacheDiskSpill'].length > 0) {
                            self.templateRDDCacheDiskSpill[0].values.push(self.lastMeasurement['rddCacheDiskSpill'][0]);
                            self.templateRDDCacheDiskSpill[1].values.push(self.lastMeasurement['rddCacheDiskSpill'][1]);

                            self.lastMeasurement['rddCacheDiskSpill'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('rddCacheDiskSpill') +
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'mean(memory_memUsed_MB), last(disk_diskSpaceUsed_MB)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['rddCacheDiskSpill'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['rddCacheDiskSpill'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['rddCacheDiskSpill'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['rddCacheDiskSpill'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    } else {
                                        self.templateRDDCacheDiskSpill[0].values.push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.templateRDDCacheDiskSpill[1].values.push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    }
                                }

                                self.hasLoadedOnce['rddCacheDiskSpill'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching rddCacheDiskSpill metrics.', ttl: 10000});
                        }
                    );
                };

                var updateGCTime = function() {
                    if (!self.now && self.hasLoadedOnce['gcTime']) {
                        if(self.lastMeasurement['gcTime'].length > 0) {
                            self.templateGCTime[0].values.push(self.lastMeasurement['gcTime'][0]);
                            self.templateGCTime[1].values.push(self.lastMeasurement['gcTime'][1]);

                            self.lastMeasurement['gcTime'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('gcTime') +
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'non_negative_derivative(mean(\"PS-MarkSweep_time\"), 1s),' +
                                             'non_negative_derivative(mean(\"PS-Scavenge_time\"), 1s)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['gcTime'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['gcTime'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['gcTime'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['gcTime'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    } else {
                                        self.templateGCTime[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.templateGCTime[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                    }
                                }

                                self.hasLoadedOnce['gcTime'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching gcTime metrics.', ttl: 10000});
                        }
                    );
                };

                var updateShuffleReadWriteDriver = function() {
                    if (!self.now && self.hasLoadedOnce['totalShuffle'])
                        return; // offline mode + we have loaded the information

                    VizopsService.getAllExecutorMetrics('totalShuffleRead,totalShuffleWrite').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data;

                                for(var i = 0; i < newData.length; i++ ) {
                                    var entry = newData[i];
                                    if (entry.id === 'driver') {
                                        self.uiShuffleRead = d3.format(".2s")(entry.totalShuffleRead);
                                        self.uiShuffleWrite = d3.format(".2s")(entry.totalShuffleWrite);
                                        break;
                                    }
                                }

                                self.hasLoadedOnce['totalShuffle'] = true;
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            // if (error.status !== 500)
                            //     growl.error(error.data.errorMsg, {title: 'Error fetching totalShuffle(driver) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updateMemorySpace();
                    updateMaxMemory();
                    updateShuffleReadWriteDriver();
                    updateGraphVCPU();
                    updateRDDCacheDiskSpill();
                    updateGCTime();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                        if (self.startTimeMap.hasOwnProperty(key)) {
                            self.startTimeMap[key] = self.startTime;
                            self.hasLoadedOnce[key] = false;
                        }
                    }

                    self.templateMemorySpace = vizopsMemorySpaceDriverTemplate();
                    self.templateVCPU = vizopsVCPUDriverTemplate();
                    self.templateRDDCacheDiskSpill = vizopsRDDCacheDiskSpillTemplate();
                    self.templateGCTime = vizopsGCTimeTemplate();
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
                            self.nbExecutorsOnDriverHost = self.hostnames[self.executorInfo.entry[0].value[1]].length - 1;

                            if (self.now) { // only schedule the interval if app is running
                                self.appinfoInterval = $interval(function() { // update appinfo data
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            var info = success.data;

                                            self.nbExecutors = info.nbExecutors;
                                            self.executorInfo = info.executorInfo;
                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            // get the unique hostnames and the number of executors running on them
                                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                                            self.nbExecutorsOnDriverHost = self.hostnames[self.executorInfo.entry[0].value[1]].length - 1;

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(driver).', ttl: 15000});
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