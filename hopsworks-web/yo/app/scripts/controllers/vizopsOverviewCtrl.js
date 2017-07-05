'use strict';

/*  Group by calls have an issue right now that the last measurement has to be ignored. This happens because the last
    time bucket is not full yet and when the next backend call happens that bucket will be full and as such more accurate.
    In the 200ok case you should be ignoring the last measurement for now.
*/

angular.module('hopsWorksApp')
         .controller('VizopsOverviewCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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
                self.nbHosts; // number of machines that ran application executors
                self.clusterCPUUtilization = "0.0";
                self.nbLiveHosts = 0;
                self.appinfoInterval;
                self.uiShuffleRead = 0.0;
                self.uiShuffleWrite = 0.0;

                self.startTimeMap = {
                    'totalActiveTasksApp': -1,
                    'totalCompletedTasksApp': -1,
                    'rateOfTaskCompletion': -1,
                    'clusterCPUUtilizationCard': -1,
                    'hdfsReadRateTotal': -1,
                    'hdfsWriteRateTotal': -1,
                    'liveHostsCard': -1,
                    'containerMemoryUsedTotal': -1,
                    'applicationShuffleTotal': -1
                };

                self.hasLoadedOnce = {
                    'totalActiveTasksApp': false,
                    'totalCompletedTasksApp': false,
                    'rateOfTaskCompletion': false,
                    'clusterCPUUtilizationCard': false,
                    'hdfsReadRateTotal': false,
                    'hdfsWriteRateTotal': false,
                    'liveHostsCard': false,
                    'containerMemoryUsedTotal': false,
                    'applicationShuffleTotal': false
                };

                self.lastMeasurement = {
                    'totalActiveTasksApp': [],
                    'totalCompletedTasksApp': [],
                    'rateOfTaskCompletion': [],
                    'clusterCPUUtilizationCard': [],
                    'hdfsReadRateTotal': [],
                    'hdfsWriteRateTotal': [],
                    'liveHostsCard': [],
                    'containerMemoryUsedTotal': [],
                    'applicationShuffleTotal': []
                };

                self.optionsTotalActiveTasks = vizopsTotalActiveTasksOptions();
                self.optionsTotalCompletedTasks = vizopsTotalCompletedTasksOptions();
                self.optionsHDFSReadRateTotal = vizopsHDFSReadRateTotalOptions();
                self.optionsHDFSWriteRateTotal = vizopsHDFSWriteRateTotalOptions();
                self.optionsContainerMemoryUsedTotal = vizopsContainerMemoryUsedTotalOptions();
                self.optionsRateOfTaskCompletion = vizopsRateOfTaskCompletionOptions();

                self.templateTotalActiveTasks = [];
                self.templateTotalCompletedTasks = [];
                self.templateHDFSReadRateTotal = [];
                self.templateHDFSWriteRateTotal = [];
                self.templateContainerMemoryUsedTotal = [];
                self.templateRateOfTaskCompletion = [];

                var updateTotalActiveTasks = function() {
                    if (!self.now && self.hasLoadedOnce['totalActiveTasksApp']) {
                        if (self.lastMeasurement['totalActiveTasksApp'].length > 0) {
                            self.templateTotalActiveTasks[0].values.push(self.lastMeasurement['totalActiveTasksApp'][0]);

                            self.lastMeasurement['totalActiveTasksApp'] = []; //clean up
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalActiveTasksApp');

                    VizopsService.getMetrics('graphite',
                                               'sum(threadpool_activeTasks)', 'spark', tags,
                                               'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['totalActiveTasksApp'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['totalActiveTasksApp'] = []; // clean it, we need only the last request

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['totalActiveTasksApp'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    } else {
                                        self.templateTotalActiveTasks[0].values.push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                    }
                                }

                                self.hasLoadedOnce['totalActiveTasksApp'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching TotalActiveTasksApp metrics.', ttl: 10000});
                        }
                    );
                };

                var updateTotalCompletedTasks = function() {
                    if (!self.now && self.hasLoadedOnce['totalCompletedTasksApp']) {
                        if(self.lastMeasurement['totalCompletedTasksApp'].length > 0) {
                            self.templateTotalCompletedTasks[0].values.push(self.lastMeasurement['totalCompletedTasksApp'][0]);

                            self.lastMeasurement['totalCompletedTasksApp'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalCompletedTasksApp');

                    VizopsService.getMetrics('graphite', 'max(threadpool_completeTasks)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + '), service fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;
                                self.startTimeMap['totalCompletedTasksApp'] = _getLastTimestampFromSeries(newData[0]);
                                self.lastMeasurement['totalCompletedTasksApp'] = [];

                                for(var i = 0; i < newData[0].values.length; i++) {
                                    var timestamp = +newData[0].values[i].split(' ')[0];
                                    var totals = _.reduce(newData, function(sum, serie) {
                                        if (i === (newData[0].values.length - 1)) {
                                            sum[1] += +serie.values[i].split(' ')[1];
                                        } else {
                                            sum[0] += +serie.values[i].split(' ')[1];
                                            sum[1] += +serie.values[i].split(' ')[1];
                                        }
                                        return sum;
                                    }, [0, 0]); // sum, last measurement(sum)

                                    if (i === (newData[0].values.length - 1)) {
                                        self.lastMeasurement['totalCompletedTasksApp'].push({'x': +timestamp,'y': +totals[1]});
                                    } else {
                                        self.templateTotalCompletedTasks[0].values.push({'x': timestamp,'y': totals[0]}); // rate
                                    }
                                }

                                self.hasLoadedOnce['totalCompletedTasksApp'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching TotalCompletedTasksApp metrics.', ttl: 10000});
                        }
                    );
                };

                var updateRateOfTaskCompletion = function() {
                    if (!self.now && self.hasLoadedOnce['rateOfTaskCompletion']) {
                        if(self.lastMeasurement['rateOfTaskCompletion'].length > 0) {
                            self.templateRateOfTaskCompletion[0].values.push(self.lastMeasurement['rateOfTaskCompletion'][0]);

                            self.lastMeasurement['rateOfTaskCompletion'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('rateOfTaskCompletion');

                    VizopsService.getMetrics('graphite', 'non_negative_derivative(max(threadpool_completeTasks))',
                        'spark', tags, 'time(' + VizopsService.getGroupByInterval() + '), service fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;
                                self.startTimeMap['rateOfTaskCompletion'] = _getLastTimestampFromSeries(newData[0]);
                                self.lastMeasurement['rateOfTaskCompletion'] = [];

                                for(var i = 0; i < newData[0].values.length; i++) {
                                    var timestamp = +newData[0].values[i].split(' ')[0];
                                    var totals = _.reduce(newData, function(sum, serie) {
                                        if (i === (newData[0].values.length - 1)) {
                                            sum[1] += +serie.values[i].split(' ')[1];
                                        } else {
                                            sum[0] += +serie.values[i].split(' ')[1];
                                            sum[1] += +serie.values[i].split(' ')[1];
                                        }
                                        return sum;
                                    }, [0, 0]); // rate, last measurement(rate)

                                    if (i === (newData[0].values.length - 1)) {
                                        self.lastMeasurement['rateOfTaskCompletion'].push({'x': +timestamp,'y': +totals[1]});
                                    } else {
                                        self.templateRateOfTaskCompletion[0].values.push({'x': timestamp,'y': totals[0]});
                                    }
                                }

                                self.hasLoadedOnce['rateOfTaskCompletion'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching rateOfTaskCompletion(overview) metrics.', ttl: 10000});
                        }
                    );
                };

                var updateClusterCPUUtilization = function() {
                    if (!self.now && self.hasLoadedOnce['clusterCPUUtilizationCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'source != \'' + self.executorInfo.entry[0].value[0] + '\' and '
                                + _getTimestampLimits('clusterCPUUtilizationCard') + ' and ' +
                                'MilliVcoreUsageIMinMilliVcores <= ' + (+self.executorInfo.entry[0].value[2]*1000);

                    VizopsService.getMetrics('graphite',
                        'mean(MilliVcoreUsageIMinMilliVcores)/' + (+self.executorInfo.entry[0].value[2]*1000),
                        'nodemanager', tags).then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['clusterCPUUtilizationCard'] = _getLastTimestampFromSeries(newData);

                            self.clusterCPUUtilization = d3.format(".1%")(newData.values[0].split(' ')[1]);

                            self.hasLoadedOnce['clusterCPUUtilizationCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching clusterCPUUtilization metric.', ttl: 10000});
                        }
                    );
                };

                var updateHdfsReadRateTotal = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsReadRateTotal']) {
                        if(self.lastMeasurement['hdfsReadRateTotal'].length > 0) {
                            self.templateHDFSReadRateTotal[0].values.push(self.lastMeasurement['hdfsReadRateTotal'][0]);
                            self.templateHDFSReadRateTotal[1].values.push(self.lastMeasurement['hdfsReadRateTotal'][1]);

                            self.lastMeasurement['hdfsReadRateTotal'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }


                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsReadRateTotal');

                    VizopsService.getMetrics('graphite',
                                             'non_negative_derivative(max(filesystem_hdfs_read_bytes)),max(filesystem_hdfs_read_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + '), service fill(0)').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['hdfsReadRateTotal'] = _getLastTimestampFromSeries(newData[0]);
                            self.lastMeasurement['hdfsReadRateTotal'] = [];

                            for(var i = 0; i < newData[0].values.length; i++) { // each serie has the same number of values
                                var timestamp = +newData[0].values[i].split(' ')[0];
                                var totals = _.reduce(newData, function(sum, serie) {
                                    sum[0] += +serie.values[i].split(' ')[1];
                                    sum[1] += +serie.values[i].split(' ')[2];
                                    return sum;
                                }, [0, 0]);

                                if (i === (newData[0].length - 1)) {
                                    self.lastMeasurement['hdfsReadRateTotal'].push({'x': +timestamp,'y': +totals[0]});
                                    self.lastMeasurement['hdfsReadRateTotal'].push({'x': +timestamp,'y': +totals[1]});
                                } else {
                                    self.templateHDFSReadRateTotal[0].values.push({'x': timestamp, 'y': totals[0]}); // rate-read
                                    self.templateHDFSReadRateTotal[1].values.push({'x': timestamp, 'y': totals[1]}); // read
                                }
                            }

                            self.hasLoadedOnce['hdfsReadRateTotal'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching hdfsReadRateTotal metric.', ttl: 10000});
                        }
                    );

                };

                var updateHdfsWriteRateTotal = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsWriteRateTotal']) {
                        if(self.lastMeasurement['hdfsWriteRateTotal'].length > 0) {
                            self.templateHDFSWriteRateTotal[0].values.push(self.lastMeasurement['hdfsWriteRateTotal'][0]);
                            self.templateHDFSWriteRateTotal[1].values.push(self.lastMeasurement['hdfsWriteRateTotal'][1]);

                            self.lastMeasurement['hdfsWriteRateTotal'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsWriteRateTotal');

                    VizopsService.getMetrics('graphite',
                                             'non_negative_derivative(max(filesystem_hdfs_write_bytes)),max(filesystem_hdfs_write_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + '), service fill(0)').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['hdfsWriteRateTotal'] = _getLastTimestampFromSeries(newData[0]);
                            self.lastMeasurement['hdfsWriteRateTotal'] = [];

                            for(var i = 0; i < newData[0].values.length - 1; i++) {
                                var timestamp = +newData[0].values[i].split(' ')[0];
                                var totals = _.reduce(newData, function(sum, serie) {
                                    sum[0] += +serie.values[i].split(' ')[1];
                                    sum[1] += +serie.values[i].split(' ')[2];
                                    return sum;
                                }, [0, 0]);

                                if (i === (newData[0].length - 1)) {
                                    self.lastMeasurement['hdfsWriteRateTotal'].push({'x': +timestamp,'y': +totals[0]});
                                    self.lastMeasurement['hdfsWriteRateTotal'].push({'x': +timestamp,'y': +totals[1]});
                                } else {
                                    self.templateHDFSWriteRateTotal[0].values.push({'x': +timestamp, 'y': +totals[0]}); // rate-write
                                    self.templateHDFSWriteRateTotal[1].values.push({'x': +timestamp, 'y': +totals[1]}); // write
                                }
                            }

                            self.hasLoadedOnce['hdfsWriteRateTotal'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching hdfsWriteRateTotal metric.', ttl: 10000});
                        }
                    );
                };

                var updateLiveHosts = function() {
                    if (!self.now && self.hasLoadedOnce['liveHostsCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'host =~ /' + Object.keys(self.hostnames).join('|') + '/ and ' + _getTimestampLimits('liveHostsCard');

                    VizopsService.getMetrics('telegraf', 'last(usage_system)', 'cpu', tags, 'host').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['liveHostsCard'] = _getLastTimestampFromSeries(newData[0]);

                            var tempAliveHosts = 0;
                            for(var i = 0; i < newData.length; i++) { // loop over each executor
                                var host = newData[i].tags.entry[0].value; // if it's there, it's alive
                                // log it
                                tempAliveHosts += 1;
                            }
                            self.nbLiveHosts = tempAliveHosts;

                            self.hasLoadedOnce['liveHostsCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching liveHostsCard metric.', ttl: 10000});
                        }
                    );
                };

                var updateContainerMemoryUsedTotal = function() {
                    // Memory from spark is much easier to work with than nodemanager's metrics
                    if (!self.now && self.hasLoadedOnce['containerMemoryUsedTotal']) {
                        if (self.lastMeasurement['containerMemoryUsedTotal'].length > 0) {
                            self.templateContainerMemoryUsedTotal[0].values.push(self.lastMeasurement['containerMemoryUsedTotal'][0]);
                            self.templateContainerMemoryUsedTotal[1].values.push(self.lastMeasurement['containerMemoryUsedTotal'][1]);

                            self.lastMeasurement['containerMemoryUsedTotal'] = [];
                        }
                    }

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('containerMemoryUsedTotal'); // + sign encodes into space so....

                    VizopsService.getMetrics('graphite', 'mean(heap_used), max(heap_max)', 'spark', tags,
                                             'service, time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series;
                                  self.startTimeMap['containerMemoryUsedTotal'] = _getLastTimestampFromSeries(newData[0]);
                                  self.lastMeasurement['containerMemoryUsedTotal'] = [];

                                  var timestampDictionary = {}, timestampsOrder = []; // { timestamp: [used sum, total sum] }
                                  for(var i = 0; i < newData.length; i++) { // loop over each executor/driver and sum the memories
                                        for(var j = 0; j < newData[i].values.length; j++) { // go through each timestamp
                                            var split = newData[i].values[j].split(' ');
                                            var timestamp = +split[0];

                                            if (j === (newData[i].values.length - 1)) continue;

                                            /* add both used and max to the rest
                                               This if will only be activated by the first series's entries,
                                               so we can maintain a list to maintain insertion order since the Object
                                               keys order depends on the browser
                                            */
                                            if (!(timestamp in timestampDictionary)) {
                                                timestampDictionary[timestamp] = [0,0];
                                                timestampsOrder.push(timestamp);
                                            }

                                            timestampDictionary[timestamp][0] += +split[1];
                                            timestampDictionary[timestamp][1] += +split[2];
                                        }
                                  }

                                  for(var i = 0; i < timestampsOrder.length; i++) {
                                      var time = timestampsOrder[i];

                                      if (i === (timestampsOrder - 1)) {
                                          self.lastMeasurement['containerMemoryUsedTotal'].push({'x': time,'y': timestampDictionary[time][0]});
                                          self.lastMeasurement['containerMemoryUsedTotal'].push({'x': time,'y': timestampDictionary[time][1]});
                                      } else {
                                          self.templateContainerMemoryUsedTotal[0].values.push({
                                              'x': time,
                                              'y': timestampDictionary[time][0]
                                          });
                                          self.templateContainerMemoryUsedTotal[1].values.push({
                                              'x': time,
                                              'y': timestampDictionary[time][1]
                                          });
                                      }
                                  }

                                  self.hasLoadedOnce['containerMemoryUsedTotal'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching containerMemoryUsedTotal metric.', ttl: 10000});
                          }
                      );
                };

                var updateApplicationShuffleTotal = function() {
                    if (!self.now && self.hasLoadedOnce['applicationShuffleTotal'])
                        return; // offline mode + we have loaded the information

                    VizopsService.getAllExecutorMetrics('totalShuffleRead,totalShuffleWrite').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data;

                                  var totalShuffleWrite = 0, totalShuffleRead = 0;

                                  for(var i = 0; i < newData.length; i++) {
                                    totalShuffleWrite += newData[i].totalShuffleWrite;
                                    totalShuffleRead += newData[i].totalShuffleRead;
                                  }

                                  self.uiShuffleRead = d3.format(".2s")(totalShuffleRead);
                                  self.uiShuffleWrite = d3.format(".2s")(totalShuffleWrite);

                                  self.hasLoadedOnce['applicationShuffleTotal'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                            // if (error.status !== 500)
                            //     growl.error(error, {title: 'Error fetching shuffleData(overview) metric.', ttl: 10000});
                          }
                    );
                };

                var updateMetrics = function() {
                    updateTotalActiveTasks();
                    updateTotalCompletedTasks();
                    updateRateOfTaskCompletion();
                    updateClusterCPUUtilization();
                    updateHdfsReadRateTotal();
                    updateHdfsWriteRateTotal();
                    updateLiveHosts();
                    updateContainerMemoryUsedTotal();
                    updateApplicationShuffleTotal();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                      if (self.startTimeMap.hasOwnProperty(key)) {
                        self.startTimeMap[key] = self.startTime;
                        self.hasLoadedOnce[key] = false;
                      }
                    }

                    self.templateTotalActiveTasks = vizopsTotalActiveTasksTemplate();
                    self.templateTotalCompletedTasks = vizopsTotalCompletedTasksTemplate();
                    self.templateHDFSReadRateTotal = vizopsHDFSReadRateTotalTemplate();
                    self.templateHDFSWriteRateTotal = vizopsHDFSWriteRateTotalTemplate();
                    self.templateContainerMemoryUsedTotal = vizopsContainerMemoryUsedTotalTemplate();
                    self.templateRateOfTaskCompletion = vizopsRateOfTaskCompletionTemplate();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didn't use groupBy calls then it would be enough to upper limit the time with now()
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
                            self.nbHosts = Object.keys(self.hostnames).length; // Remove driver

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
                                            self.nbHosts = Object.keys(self.hostnames).length; // Remove driver

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
                            growl.error(error.data.errorMsg, {title: 'Error fetching Overview info.', ttl: 10000});
                        }
                    );
                }

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