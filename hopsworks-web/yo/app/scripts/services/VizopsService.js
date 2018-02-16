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

/*
    An HTTP response contains the following properties:
    - data {string|Object} – The response body transformed with the transform functions.
    - status – {number} – HTTP status code of the response.
    - headers – {function([headerName])} – Header getter function.
    - config – {Object} – The configuration object that was used to generate the request.
    - statusText – {string} – HTTP status text of the response.
*/

angular.module('hopsWorksApp')
    .factory('VizopsService', ['$http', '$interval', function ($http, $interval) {
        var self = this;
        var groupByInterval = '10s';

        self.projectId = '';
        self.appId = '';
        self.endTime;
        self.now = null;

        var service = {
            init: function(projectId, appId) {
                self.projectId = projectId;
                self.appId = appId;
                self.groupByInterval = groupByInterval;
            },

            getAppId : function() { return self.appId; },
            getProjectId : function() { return self.projectId; },
            getGroupByInterval : function() { return self.groupByInterval; }, // The controllers 'watch' this value
            setGroupByInterval : function(groupBy) { self.groupByInterval = groupBy; },

            getMetrics: function(database, columns, measurement, tags, groupBy) {
                var query = '/api/project/' + self.projectId + '/jobs/' + self.appId + '/influxdb/' + database + '?' +
                            'columns=' + columns + '&measurement=' + measurement + '&tags=' + tags;
                if (groupBy) {
                    query += '&groupBy=' + groupBy;
                }
                return $http.get(query);
            },

            // filters = string e.g. totalShuffleRead,totalShuffleWrite,completedTasks no need to pass "id", empty for all
            getAllExecutorMetrics: function(filters) {
                var query = '/api/project/' + self.projectId + '/jobs/' + self.appId + '/influxdb/allexecutors?filters=' + filters;
                return $http.get(query);
            }
        };

        return service;
    }]);