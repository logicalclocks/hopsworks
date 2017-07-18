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