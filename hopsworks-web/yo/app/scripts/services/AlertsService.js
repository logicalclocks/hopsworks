/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
    .factory('AlertsService', ['$http', function ($http) {
        return function (id) {
            var alertBaseUrl = '/api/project/' + id + '/alerts';
            var serviceAlertBaseUrl = '/api/project/' + id + '/service/alerts';
            var jobAlertBaseUrl = function (jobName) {
                return '/api/project/' + id + '/jobs/' + jobName + '/alerts';
            }
            var featureGroupAlertBaseUrl = function (fsId, fgId) {
                return '/api/project/' + id + '/featurestores/' + fsId + '/featuregroups/' + fgId + '/alerts';
            }
            var getQuery = function (query, queryName, first) {
                var c = typeof first === "undefined" || first === false? '&' : '?';
                return typeof query === "undefined"? '': c + queryName + '=' + query;
            };
            var getQueryArray = function (query, queryName, first) {
                var q = '';
                for (var i=0; i<query.length; i++) {
                    if (i === 0 && first) {
                        q = getQuery(query[i], queryName, true);
                    } else {
                        q += getQuery(query[i], queryName, false);
                    }
                }
                return q;
            }
            var serviceAlerts = {
                getAll: function () {
                    return $http.get(serviceAlertBaseUrl);
                },
                get: function (id) {
                    return $http.get(serviceAlertBaseUrl + '/' + id);
                },
                getValues: function () {
                    return $http.get(serviceAlertBaseUrl + '/values');
                },
                create: function (alert) {
                    var req = {
                        method: 'POST',
                        url: serviceAlertBaseUrl,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                update: function (id, alert) {
                    var req = {
                        method: 'PUT',
                        url: serviceAlertBaseUrl + '/' + id,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                delete: function (id) {
                    return $http.delete(serviceAlertBaseUrl + '/' + id);
                },
                test: function (id) {
                    return $http.post(serviceAlertBaseUrl + '/' + id + '/test');
                }
            };
            var jobAlerts = {
                getAll: function (jobName) {
                    return $http.get(jobAlertBaseUrl(jobName));
                },
                get: function (jobName, id) {
                    return $http.get(jobAlertBaseUrl(jobName) + '/' + id);
                },
                getValues: function (jobName) {
                    return $http.get(jobAlertBaseUrl(jobName) + '/values');
                },
                create: function (jobName, alert) {
                    var req = {
                        method: 'POST',
                        url: jobAlertBaseUrl(jobName),
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                update: function (jobName, id, alert) {
                    var req = {
                        method: 'PUT',
                        url: jobAlertBaseUrl(jobName) + '/' + id,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                delete: function (jobName, id) {
                    return $http.delete(jobAlertBaseUrl(jobName) + '/' + id);
                },
                test: function (jobName, id) {
                    return $http.post(jobAlertBaseUrl(jobName) + '/' + id + '/test');
                }
            };
            var featureGroupAlerts = {
                getAll: function (fsId, fgId) {
                    return $http.get(featureGroupAlertBaseUrl(fsId, fgId));
                },
                get: function (fsId, fgId, id) {
                    return $http.get(featureGroupAlertBaseUrl(fsId, fgId) + '/' + id);
                },
                getValues: function (fsId, fgId) {
                    return $http.get(featureGroupAlertBaseUrl(fsId, fgId) + '/values');
                },
                create: function (fsId, fgId, alert) {
                    var req = {
                        method: 'POST',
                        url: featureGroupAlertBaseUrl(fsId, fgId),
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                update: function (fsId, fgId, id, alert) {
                    var req = {
                        method: 'PUT',
                        url: featureGroupAlertBaseUrl(fsId, fgId) + '/' + id,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                },
                delete: function (fsId, fgId, id) {
                    return $http.delete(featureGroupAlertBaseUrl(fsId, fgId) + '/' + id);
                },
                test: function (fsId, fgId, id) {
                    return $http.post(featureGroupAlertBaseUrl(fsId, fgId) + '/' + id + '/test');
                }
            };
            var alerts = {
                getAll: function () {
                    return $http.get(alertBaseUrl);
                },
                get: function () {
                    return $http.get(alertBaseUrl + '/groups');
                },
                create: function (alert) {
                    var req = {
                        method: 'POST',
                        url: alertBaseUrl,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: alert
                    };
                    return $http(req);
                }
            };
            var routes = {
                getAll: function () {
                    return $http.get(alertBaseUrl + '/routes');
                },
                get: function (receiver, match, matchRe) {
                    var query = getQueryArray(match, "match", true);
                    query += getQueryArray(matchRe, "matchRe", query  === '');
                    return $http.get(alertBaseUrl + '/routes/' + receiver + query);
                },
                create: function (route) {
                    var req = {
                        method: 'POST',
                        url: alertBaseUrl + '/routes',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: route
                    };
                    return $http(req);
                },
                update: function (route, receiver, match, matchRe) {
                    var query = getQueryArray(match, "match", true);
                    query += getQueryArray(matchRe, "matchRe", query  === '');
                    var req = {
                        method: 'PUT',
                        url: alertBaseUrl + '/routes/' + receiver + query,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: route
                    };
                    return $http(req);
                },
                delete: function (receiver, match, matchRe) {
                    var query = getQueryArray(match, "match", true);
                    query += getQueryArray(matchRe, "matchRe", query  === '');
                    return $http.delete(alertBaseUrl + '/routes/' + receiver + query);
                }
            };
            var receivers = {
                getAll: function (includeGlobal) {
                    var global = typeof includeGlobal === 'undefined'? false : includeGlobal;
                    return $http.get(alertBaseUrl + '/receivers?global=' + global);
                },
                get: function (name) {
                    return $http.get(alertBaseUrl + '/receivers/' + name);
                },
                create: function (receiver) {
                    var req = {
                        method: 'POST',
                        url: alertBaseUrl + '/receivers',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: receiver
                    };
                    return $http(req);
                },
                update: function (name, receiver) {
                    var req = {
                        method: 'PUT',
                        url: alertBaseUrl + '/receivers/' + name,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: receiver
                    };
                    return $http(req);
                },
                delete: function (name) {
                    return $http.delete(alertBaseUrl + '/receivers/' + name);
                }
            };
            var silences = {
                getAll: function () {
                    return $http.get(alertBaseUrl + '/silences');
                },
                get: function (id) {
                    return $http.get(alertBaseUrl + '/silences/' + id);
                },
                create: function (silence) {
                    var req = {
                        method: 'POST',
                        url: alertBaseUrl + '/silences',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: silence
                    };
                    return $http(req);
                },
                update: function (id, silence) {
                    var req = {
                        method: 'PUT',
                        url: alertBaseUrl + '/silences/' + id,
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: silence
                    };
                    return $http(req);
                },
                delete: function (id) {
                    return $http.delete(alertBaseUrl + '/silences/' + id);
                }
            };
            return {
                serviceAlerts: serviceAlerts,
                jobAlerts: jobAlerts,
                featureGroupAlerts: featureGroupAlerts,
                alerts: alerts,
                routes: routes,
                receivers: receivers,
                silences: silences
            };
        }
    }]);