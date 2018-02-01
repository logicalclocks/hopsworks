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
         .controller('VizopsWorkerCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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
                /* It can be a specific host or all, e.g. ['dn0'](one) or ['dn0', 'dn1'](all)
                   It can either be set to hostsList or chosenHostToFilter
                */
                self.hostsToQuery;
                self.hostsList; // List of all the hosts
                self.nbOfHosts; // hostsList.length
                self.chosenHostToFilter; // two way binded with the chosen host to filter

                self.optionsPhysicalCPUUsage = vizopsWorkerPhysicalCpuOptions();
                self.optionsMemoryUsage = vizopsWorkerMemoryUsageOptions();
                self.optionsNetworkTraffic = vizopsWorkerNetworkTrafficOptions();
                self.optionsDiskUsage = vizopsWorkerDiskUsageOptions();
                self.optionsExecutorsPerHost = vizopsWorkerExecutorsPerHostOptions();
                self.optionsCompletedTasksPerHost = vizopsWorkerCompletedTasksPerHostOptions();

                self.templatePhysicalCPUUsage = [];
                self.templateMemoryUsage = [];
                self.templateNetworkTraffic = [];
                self.templateDiskUsage = [];
                self.templateExecutorsPerHost = [];
                self.templateCompletedTasksPerHost = [];

                self.startTimeMap = {
                    'physicalCpuUsage': -1,
                    'ramUsage': -1,
                    'networkUsage': -1,
                    'diskUsage': -1,
                    'completedTasksPerHost': -1
                };

                self.hasLoadedOnce = {
                    'physicalCpuUsage': false,
                    'ramUsage': false,
                    'networkUsage': false,
                    'diskUsage': false,
                    'completedTasksPerHost': false
                };

                self.lastMeasurement = {
                    'physicalCpuUsage': [],
                    'ramUsage': [],
                    'networkUsage': [],
                    'diskUsage': [],
                    'completedTasksPerHost': []
                };

                var updatePCpuUsage = function() {
                    if (!self.now && self.hasLoadedOnce['physicalCpuUsage']) {
                        if (self.lastMeasurement['physicalCpuUsage'].length > 0) {
                            self.templatePhysicalCPUUsage[0].values.push(self.lastMeasurement['physicalCpuUsage'][0]);
                            self.templatePhysicalCPUUsage[1].values.push(self.lastMeasurement['physicalCpuUsage'][1]);
                            self.templatePhysicalCPUUsage[2].values.push(self.lastMeasurement['physicalCpuUsage'][2]);

                            self.lastMeasurement['physicalCpuUsage'] = []; // clean up
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = 'cpu = \'cpu-total\' and ' + _getTimestampLimits('physicalCpuUsage') +
                               ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'mean(usage_user), mean(usage_iowait), mean(usage_idle)', 'cpu',
                                             tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['physicalCpuUsage'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['physicalCpuUsage'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        // save last measurement
                                        self.lastMeasurement['physicalCpuUsage'].push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.lastMeasurement['physicalCpuUsage'].push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                        self.lastMeasurement['physicalCpuUsage'].push({'x': +splitEntry[0], 'y': +splitEntry[3]});
                                    } else {
                                        self.templatePhysicalCPUUsage[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.templatePhysicalCPUUsage[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                        self.templatePhysicalCPUUsage[2].values.push({'x': +splitEntry[0], 'y': +splitEntry[3]});
                                    }
                                }

                                self.hasLoadedOnce['physicalCpuUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching physicalCpuUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateRAMUsage = function() {
                    if (!self.now && self.hasLoadedOnce['ramUsage']) {
                        if (self.lastMeasurement['ramUsage'].length > 0) {
                            self.templateMemoryUsage[0].values.push(self.lastMeasurement['ramUsage'][0]);
                            self.templateMemoryUsage[1].values.push(self.lastMeasurement['ramUsage'][1]);
                            
                            self.lastMeasurement['ramUsage'] = []; //clean up
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = _getTimestampLimits('ramUsage') + ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'mean(used), mean(available)',
                                             'mem', tags, 'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['ramUsage'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['ramUsage'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['ramUsage'].push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.lastMeasurement['ramUsage'].push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    } else {
                                        self.templateMemoryUsage[0].values.push({'x': +splitEntry[0],'y': +splitEntry[1]});
                                        self.templateMemoryUsage[1].values.push({'x': +splitEntry[0],'y': +splitEntry[2]});
                                    }
                                }

                                self.hasLoadedOnce['ramUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching ramUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateNetworkUsage = function() {
                    if (!self.now && self.hasLoadedOnce['networkUsage']) {
                        if (self.lastMeasurement['networkUsage'].length > 0) {
                            self.templateNetworkTraffic[0].values.push(self.lastMeasurement['networkUsage'][0]);
                            self.templateNetworkTraffic[1].values.push(self.lastMeasurement['networkUsage'][1]);

                            self.lastMeasurement['networkUsage'] = [];
                        }

                        return; // offline mode + we have loaded the information
                    }

                    var tags = _getTimestampLimits('networkUsage') + ' and host =~ /' + self.hostsToQuery.join('|') + '/';
                    var columns = '';
                    if (self.hostsToQuery.length === 1) {
                        columns = 'derivative(first(bytes_recv)), derivative(first(bytes_sent))';
                    } else {
                        columns = 'derivative(mean(bytes_recv)), derivative(mean(bytes_sent))';
                    }

                    VizopsService.getMetrics('telegraf', columns, 'net', tags,
                                             'time(' + VizopsService.getGroupByInterval() + ') fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['networkUsage'] = _getLastTimestampFromSeries(newData);
                                self.lastMeasurement['networkUsage'] = [];

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    if (i === (metrics.length - 1)) {
                                        self.lastMeasurement['networkUsage'].push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.lastMeasurement['networkUsage'].push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                    } else {
                                        self.templateNetworkTraffic[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                        self.templateNetworkTraffic[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                    }
                                }

                                self.hasLoadedOnce['networkUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching networkUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateDiskUsage = function() {
                    if (!self.now && self.hasLoadedOnce['diskUsage'])
                        return; // offline mode + we have loaded the information

                    var tags = _getTimestampLimits('diskUsage') + ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'last(used), last(free)','disk', tags, 'device, host').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;

                                self.startTimeMap['diskUsage'] = _getLastTimestampFromSeries(newData[0]);
                                self.templateDiskUsage[0].values = [];
                                self.templateDiskUsage[1].values = [];

                                if (self.hostsToQuery.length === 1) { // Only one host, show the devices used/free
                                    for(var i = 0; i < newData.length - 1; i++) { // loop over each device
                                        var device = newData[i].tags.entry[0].value;
                                        var usedSpace = +newData[i].values[0].split(' ')[1];
                                        var freeSpace = +newData[i].values[0].split(' ')[2];

                                        self.templateDiskUsage[0].values.push({'x': device, 'y': usedSpace});
                                        self.templateDiskUsage[1].values.push({'x': device, 'y': freeSpace});
                                    }
                                } else { // Many hosts, show the total used/total free
                                    var diskSpacePerHost = {};

                                    self.hostsList.forEach(function(host) {
                                        diskSpacePerHost[host] = [0, 0];
                                    });

                                    for(var i = 0; i < newData.length - 1; i++) { // loop over each host/device combo
                                        var hostname = newData[i].tags.entry[1].value;
                                        diskSpacePerHost[hostname][0] += +newData[i].values[0].split(' ')[1];
                                        diskSpacePerHost[hostname][1] += +newData[i].values[0].split(' ')[2];
                                    }

                                    self.hostsList.forEach(function(host) {
                                        self.templateDiskUsage[0].values.push({'x': host, 'y': diskSpacePerHost[hostname][0]});
                                        self.templateDiskUsage[1].values.push({'x': host, 'y': diskSpacePerHost[hostname][1]});
                                    });
                                }

                                self.hasLoadedOnce['diskUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching diskUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateExecutorsPerHost = function () {
                    // Not called from updateMetrics but every time we call the /appinfo
                    self.templateExecutorsPerHost = [];

                    var hostsNames = Object.keys(self.hostnames);
                    for(var i = 0; i < hostsNames.length; i++) {
                        var host = hostsNames[i], len = self.hostnames[host].length;
                        if (self.hostnames[host].indexOf(0) > -1) {
                            len -= 1; // remove driver
                        }

                        self.templateExecutorsPerHost.push({'x': host, 'y': len});
                    }
                };

                var updateCompletedTasksPerHost = function () {
                    if (!self.now && self.hasLoadedOnce['completedTasksPerHost'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('completedTasksPerHost');

                    VizopsService.getMetrics('graphite', 'last(threadpool_completeTasks)', 'spark', tags, 'service').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series; // array of series - per executor
                                self.startTimeMap['completedTasksPerHost'] = _getLastTimestampFromSeries(newData[0]);
                                self.templateCompletedTasksPerHost = [];
                                var taskAccumulator = {};

                                for(var i = 0; i < newData.length; i++) { // loop over each executor
                                    var executorID = newData[i].tags.entry[0].value;
                                    var completedTasks = +newData[i].values[0].split(" ")[1];

                                    var hostOfExecutor = _.findKey(self.hostnames, function(x) { return _.indexOf(x, +executorID) > -1; });
                                    if (hostOfExecutor in taskAccumulator)
                                        taskAccumulator[hostOfExecutor] += completedTasks;
                                    else
                                        taskAccumulator[hostOfExecutor] = completedTasks;
                                }

                                var keys = Object.keys(taskAccumulator);
                                for(var i = 0; i < keys.length; i++) {
                                    self.templateCompletedTasksPerHost.push({'x': keys[i], 'y': taskAccumulator[keys[i]]});
                                }

                                self.hasLoadedOnce['completedTasksPerHost'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching TotalCompletedTasksApp metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updatePCpuUsage();
                    updateRAMUsage();
                    updateNetworkUsage();
                    updateDiskUsage();
                    updateExecutorsPerHost();
                    updateCompletedTasksPerHost();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                      if (self.startTimeMap.hasOwnProperty(key)) {
                        self.startTimeMap[key] = self.startTime;
                        self.hasLoadedOnce[key] = false;
                      }
                    }

                    self.templatePhysicalCPUUsage = vizopsWorkerPhysicalCpuTemplate();
                    self.templateMemoryUsage = vizopsWorkerMemoryUsageTemplate();
                    self.templateNetworkTraffic = vizopsWorkerNetworkTrafficTemplate();
                    self.templateDiskUsage = vizopsWorkerDiskUsageTemplate();
                    self.templateExecutorsPerHost = vizopsWorkerExecutorsPerHostTemplate();
                    self.templateCompletedTasksPerHost = vizopsWorkerCompletedTasksPerHostTemplate();
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
                            self.hostsList = Object.keys(self.hostnames);
                            self.nbOfHosts = self.hostsList.length;
                            self.hostsToQuery = self.hostsList;

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
                                            self.hostsList = Object.keys(self.hostnames);
                                            self.nbOfHosts = self.hostsList.length;

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(worker).', ttl: 15000});
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

                self.onHostFilterChange = function() {
                    if (self.hostsList.length === 1) { // only one host, filtering doesn't change anything
                        return;
                    } else if (self.chosenHostToFilter === null) { // filter was emptied, reset to original
                        self.hostsToQuery = self.hostsList;
                    } else if (_.isEqual([self.chosenHostToFilter], self.hostsToQuery)) { // same choice, skip
                        return;
                    } else {
                        self.hostsToQuery = [self.chosenHostToFilter];
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