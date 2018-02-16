/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

'use strict';

angular.module('hopsWorksApp')
         .controller('VizopsExecutorCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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

                // UI
                self.maxUsedExecutorMem = "0.0";
                self.maxAvailableExecutorMem = "0.0";
                self.maxAvailableExecutorMemValue = 0.0;
                self.hostsList; // List of all the hosts
                self.chosenFilter; // model for the actual select dropdown
                self.executorIDFromInput;
                self.executorQuery = '[0-9]%2B'; // the actual filter that will be applied to the query
                self.containerQuery;
                self.containerTemplate = '.*_APPID_.*_\\d{5}[2-9]'; // holds the original container regex - filled by init
                self.selectiveContainersTemplate = '.*_APPID_.*_'; // used for picking the containers running on the same host

                self.optionsAggregatedVCPUUsage = vizopsExecutorCPUOptions();
                self.optionsTaskDistribution = vizopsExecutorTaskDistributionOptions();
                self.optionsMemoryUsage = vizopsExecutorMemoryUsageOptions();
                self.optionsHDFSDiskRead = vizopsExecutorHDFSDiskReadOptions();
                self.optionsHDFSDiskWrite = vizopsExecutorHDFSDiskWriteOptions();
                self.optionsGCTime = vizopsExecutorGCTimeOptions();
                self.optionsPeakMemoryPerExecutor = vizopsExecutorPeakMemoryOptions();
                self.optionsShuffleReadWrite = vizopsApplicationShuffleOptions();

                self.templateAggregatedVCPUUsage = [];
                self.templateTaskDistribution = [];
                self.templateMemoryUsage = [];
                self.templateHDFSDiskRead = [];
                self.templateHDFSDiskWrite = [];
                self.templateGCTime = [];
                self.templatePeakMemoryPerExecutor = [];
                self.templateShuffleReadWrite = [];

                self.startTimeMap = {
                    'maxMemoryCard': -1,
                    'vcpuUsage': -1,
                    'memoryUsage': -1,
                    'taskDistribution': -1,
                    'hdfsDiskRead': -1,
                    'hdfsDiskWrite': -1,
                    'gcTime': -1,
                    'peakMemoryPerExecutor': -1,
                    'totalShuffle': -1
                };

                self.hasLoadedOnce = {
                    'maxMemoryCard': false,
                    'vcpuUsage': false,
                    'memoryUsage': false,
                    'taskDistribution': false,
                    'hdfsDiskRead': false,
                    'hdfsDiskWrite': false,
                    'gcTime': false,
                    'peakMemoryPerExecutor': false,
                    'totalShuffle': false
                };

                self.lastMeasurement = {
                    'maxMemoryCard': [],
                    'vcpuUsage': [],
                    'memoryUsage': [],
                    'taskDistribution': [],
                    'hdfsDiskRead': [],
                    'hdfsDiskWrite': [],
                    'gcTime': [],
                    'peakMemoryPerExecutor': [],
                    'totalShuffle': []
                };

                var updateMaxMemoryCard = function() {
                    if (!self.now && self.hasLoadedOnce['maxMemoryCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and service =~ /' + self.executorQuery +'/'; // + sign encodes into space so....

                    VizopsService.getMetrics('graphite',
                        'max(heap_used), heap_max, service', 'spark', tags).then(
                    function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['maxMemoryCard'] = _getLastTimestampFromSeries(newData);

                            self.maxUsedExecutorMem = d3.format(".4s")(newData.values[0].split(' ')[1]);
                            self.maxAvailableExecutorMem = d3.format(".4s")(newData.values[0].split(' ')[2]);
                            self.maxAvailableExecutorMemValue = +newData.values[0].split(' ')[2];

                            self.hasLoadedOnce['maxMemoryCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching MaxExecMemory metric.', ttl: 10000});
                        }
                    );
                };

                var updateAggregatedVCPUUsageGraph = function() {
                    if (!self.now && self.hasLoadedOnce['vcpuUsage']) {
                        if(self.lastMeasurement['vcpuUsage'].length > 0) {
                            self.templateAggregatedVCPUUsage[0].values.push(self.lastMeasurement['vcpuUsage'][0]);

                            self.lastMeasurement['vcpuUsage'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'source =~ /' + self.containerQuery + '/ and ' + _getTimestampLimits('vcpuUsage')
                               + ' and MilliVcoreUsageIMinMilliVcores <= ' + (+self.executorInfo.entry[0].value[2]*1000);

                    VizopsService.getMetrics('graphite',
                        'mean(MilliVcoreUsageIMinMilliVcores)/' + (+self.executorInfo.entry[0].value[2]*1000),
                        'nodemanager', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                self.startTimeMap['vcpuUsage'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['vcpuUsage'] = [];

                                var metrics = newData.values;

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['vcpuUsage'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    } else {
                                        self.templateAggregatedVCPUUsage[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    }
                                }

                                self.hasLoadedOnce['vcpuUsage'] = true;
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching ExecutorCPU metrics.', ttl: 10000});
                        }
                    );
                };

                var updateTaskDistribution = function() {
                      if (!self.now && self.hasLoadedOnce['taskDistribution'])
                          return; // offline mode + we have loaded the information

                      var tags = 'appid = \'' + self.appId + '\' and service =~ /' + self.executorQuery +'/' +
                                 ' and ' + _getTimestampLimits('taskDistribution');

                      VizopsService.getMetrics('graphite', 'last(threadpool_completeTasks)', 'spark', tags, 'service').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series;
                                  self.startTimeMap['taskDistribution'] = _getLastTimestampFromSeries(newData[0]);

                                  self.templateTaskDistribution[0].values = []; // we can afford to clear the data

                                  for(var i = 0; i < newData.length; i++) { // loop over each executor
                                      var executorID = newData[i].tags.entry[0].value;
                                      var totalTasks = +newData[i].values[0].split(" ")[1];

                                      self.templateTaskDistribution[0].values.push({'x': executorID, 'y': totalTasks});
                                  }

                                  self.hasLoadedOnce['taskDistribution'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching taskDistribution metrics.', ttl: 10000});
                          }
                      );
                  };

                var updateMemoryUsage = function() {
                    if (!self.now && self.hasLoadedOnce['memoryUsage']) {
                        if(self.lastMeasurement['memoryUsage'].length > 0) {
                            self.templateMemoryUsage[0].values.push(self.lastMeasurement['memoryUsage'][0]);

                            self.lastMeasurement['memoryUsage'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('memoryUsage') +
                               ' and service =~ /' + self.executorQuery +'/';

                    VizopsService.getMetrics('graphite', 'mean(heap_used), max(heap_max)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series[0];
                                  var metrics = newData.values;
                                  self.startTimeMap['memoryUsage'] = _getLastTimestampFromSeries(newData);
                                  self.lastMeasurement['memoryUsage'] = [];

                                  for(var i = 0; i < metrics.length; i++) {
                                      var splitEntry = metrics[i].split(' ');

                                      if (i === (metrics.length - 1)) {
                                          self.lastMeasurement['memoryUsage'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                      } else {
                                          self.templateMemoryUsage[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                      }
                                  }

                                  self.hasLoadedOnce['memoryUsage'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching memoryUsage metric.', ttl: 10000});
                          }
                      );
                };

                var updateHDFSDiskRead = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsDiskRead'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsDiskRead') +
                               ' and service =~ /' + self.executorQuery +'/'; // + sign encodes into space so....

                    VizopsService.getMetrics('graphite', 'last(filesystem_file_read_bytes), last(filesystem_hdfs_read_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series[0];
                                  var metrics = newData.values;
                                  self.startTimeMap['hdfsDiskRead'] = _getLastTimestampFromSeries(newData);

                                  for(var i = 0; i < metrics.length; i++) {
                                      var splitEntry = metrics[i].split(' ');

                                      self.templateHDFSDiskRead[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                      self.templateHDFSDiskRead[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                  }

                                  self.hasLoadedOnce['hdfsDiskRead'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching hdfsDiskRead metric.', ttl: 10000});
                          }
                      );
                };

                var updateHDFSDiskWrite = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsDiskWrite'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsDiskWrite') +
                               ' and service =~ /' + self.executorQuery +'/'; // + sign encodes into space so....

                    VizopsService.getMetrics('graphite', 'last(filesystem_file_write_bytes), last(filesystem_hdfs_write_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series[0];
                                  var metrics = newData.values;
                                  self.startTimeMap['hdfsDiskWrite'] = _getLastTimestampFromSeries(newData);

                                  for(var i = 0; i < metrics.length; i++) {
                                      var splitEntry = metrics[i].split(' ');

                                      self.templateHDFSDiskWrite[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                      self.templateHDFSDiskWrite[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                  }

                                  self.hasLoadedOnce['hdfsDiskWrite'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching hdfsDiskWrite metric.', ttl: 10000});
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

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsDiskWrite') +
                               ' and service =~ /' + self.executorQuery + '/'; // + sign encodes into space so....

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
                            growl.error(error.data.errorMsg, {title: 'Error fetching gcTime(executor) metrics.', ttl: 10000});
                        }
                    );
                };

                var updatePeakMemoryPerExecutor = function() {
                    if (!self.now && self.hasLoadedOnce['peakMemoryPerExecutor'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and service =~ /' + self.executorQuery +'/';

                    VizopsService.getMetrics('graphite', 'max(heap_used)', 'spark', tags, 'service').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;
                                self.startTimeMap['peakMemoryPerExecutor'] = _getLastTimestampFromSeries(newData[0]);

                                self.templatePeakMemoryPerExecutor[0].values = [];

                                for(var i = 0; i < newData.length; i++) { // loop over each executor
                                    var executorID = newData[i].tags.entry[0].value;
                                    var maxMemory = +newData[i].values[0].split(" ")[1];

                                    self.templatePeakMemoryPerExecutor[0].values.push({'x': executorID, 'y': maxMemory});
                                }

                                self.hasLoadedOnce['peakMemoryPerExecutor'] = true;
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching peakMemoryPerExecutor metrics.', ttl: 10000});
                        }
                    );
                };

                var updateShuffleReadWritePerExecutor = function() {
                    if (!self.now && self.hasLoadedOnce['totalShuffle'])
                        return; // offline mode + we have loaded the information

                    VizopsService.getAllExecutorMetrics('totalShuffleRead,totalShuffleWrite').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data;

                                self.templateShuffleReadWrite[0].values = [];
                                self.templateShuffleReadWrite[1].values = [];

                                for(var i = 0; i < newData.length; i++) {
                                    var entry = newData[i];
                                    var executorID = entry.id;
                                    var totalShuffleRead = entry.totalShuffleRead;
                                    var totalShuffleWrite = entry.totalShuffleWrite;

                                    if (executorID === 'driver') continue;

                                    if (self.executorQuery === '[0-9]%2B') { // all executors are included, just add it
                                        self.templateShuffleReadWrite[0].values.push({'x': executorID, 'y': totalShuffleRead});
                                        self.templateShuffleReadWrite[1].values.push({'x': executorID, 'y': totalShuffleWrite});
                                    } else {
                                        var executorsSplit = self.executorQuery.split('|');
                                        if (executorsSplit.indexOf(executorID) > -1) {
                                            self.templateShuffleReadWrite[0].values.push({'x': executorID, 'y': totalShuffleRead});
                                            self.templateShuffleReadWrite[1].values.push({'x': executorID, 'y': totalShuffleWrite});
                                        }
                                    }
                                }

                                self.hasLoadedOnce['totalShuffle'] = true;
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            // if (error.status !== 500)
                            //     growl.error(error.data.errorMsg, {title: 'Error fetching totalShuffle(executor) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updateMaxMemoryCard();
                    updateAggregatedVCPUUsageGraph();
                    updateTaskDistribution();
                    updateMemoryUsage();
                    updateHDFSDiskRead();
                    updateHDFSDiskWrite();
                    updateGCTime();
                    updatePeakMemoryPerExecutor();
                    updateShuffleReadWritePerExecutor();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                      if (self.startTimeMap.hasOwnProperty(key)) {
                        self.startTimeMap[key] = self.startTime;
                        self.hasLoadedOnce[key] = false;
                      }
                    }

                    self.templateAggregatedVCPUUsage = vizopsExecutorCPUDataTemplate();
                    self.templateTaskDistribution = vizopsExecutorTaskDistributionTemplate();
                    self.templateMemoryUsage = vizopsExecutorMemoryUsageTemplate();
                    self.templateHDFSDiskRead = vizopsExecutorHDFSDiskReadTemplate();
                    self.templateHDFSDiskWrite = vizopsExecutorHDFSDiskWriteTemplate();
                    self.templateGCTime = vizopsExecutorGCTimeTemplate();
                    self.templatePeakMemoryPerExecutor = vizopsExecutorPeakMemoryTemplate();
                    self.templateShuffleReadWrite = vizopsApplicationShuffleTemplate();
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

                            self.nbExecutors = info.nbExecutors - 1;
                            self.executorInfo = info.executorInfo;
                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            self.containerTemplate = self.containerTemplate.replace('APPID', self.appId.substring(12));
                            self.selectiveContainersTemplate = self.selectiveContainersTemplate.replace('APPID', self.appId.substring(12));
                            self.containerQuery = self.containerTemplate;

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                            self.hostsList = Object.keys(self.hostnames);
                            self.filterChoices = [].concat('by executor id', self.hostsList);

                            if (self.now) { // only schedule the interval if app is running
                                self.appinfoInterval = $interval(function() { // update appinfo data
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            var info = success.data;

                                            self.nbExecutors = info.nbExecutors - 1;
                                            self.executorInfo = info.executorInfo;
                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            // get the unique hostnames and the number of executors running on them
                                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                                            self.hostsList = Object.keys(self.hostnames);
                                            self.filterChoices = [].concat('by executor id', self.hostsList);

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(overview).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                            resetGraphs();
                            updateMetrics();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 10000});
                        }
                    );
                };

                init();

                self.onFilterChoiceChange = function() {
                    if (self.chosenFilter === null) {    // filter was emptied, reset to original
                        self.executorQuery = '[0-9]%2B'; // [0-9]+
                        self.containerQuery = self.containerTemplate;
                    } else if (self.chosenFilter === 'by executor id') {
                        self.executorIDFromInput = '';
                        return;
                    } else if (_.isEqual([self.chosenFilter], self.hostsToQuery)) { // same choice, skip
                        return;
                    } else {
                        // Remove the driver if there
                        var executorsOnHost = self.hostnames[self.chosenFilter];
                        var index = executorsOnHost.indexOf(0);
                        if (index > -1) {
                            executorsOnHost.splice(index, 1);
                        }
                        self.executorQuery = executorsOnHost.join('|');
                        self.containerQuery = self.selectiveContainersTemplate +
                                              '(?:' + executorsOnHost.map(function(x) { return _.padLeft(x, 6, '0'); }).join('|') + ')';
                    }

                    resetGraphs();
                    updateMetrics();
                };

                self.onExecutorIDFilterApply = function($event) {
                    if ($event.keyCode !== 13) {
                        return;
                    } else if (self.executorIDFromInput === null) {
                        self.executorQuery = '[0-9]%2B';
                        self.containerQuery = self.containerTemplate;
                    } else if ((self.executorIDFromInput < 1) || (self.executorIDFromInput > self.nbExecutors)) {
                        return;
                    } else if (self.executorIDFromInput === self.executorQuery) {
                        return;
                    } else {
                        self.executorQuery = self.executorIDFromInput.toString();
                        self.containerQuery = self.executorInfo.entry[self.executorIDFromInput].value[0];
                    }

                    resetGraphs();
                    updateMetrics();
                };

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