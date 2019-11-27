/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
    .factory('TemplateService', ['$http', function ($http) {
        return function (projectId) {
            var services = {
                attachTemplate: function (fileTemplateData) {
                    var regReq = {
                        method: 'POST',
                        url: '/api/metadata/' + projectId + '/attachTemplate',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: fileTemplateData
                    };

                    return $http(regReq);
                },
                fetchTemplate: function (templateid, sender) {
                    return $http.get('/api/metadata/' + projectId + '/fetchtemplate/' + templateid + '/' + sender);
                },
                fetchTemplatesForInode: function (inodeid) {
                    return $http.get('/api/metadata/' + projectId + '/fetchtemplatesforinode/' + inodeid);
                },
                fetchAvailableTemplatesforInode: function (inodeid) {
                    return $http.get('/api/metadata/' + projectId + '/fetchavailabletemplatesforinode/' + inodeid);
                },
                detachTemplate: function (inodeid, templateid) {
                    return $http.get('/api/metadata/' + projectId + '/detachtemplate/' + inodeid + '/' + templateid);
                },
                fetchMetadata: function (inodePid, inodeName, tableId) {
                    return $http.get('/api/metadata/' + projectId + '/fetchmetadata/' + inodePid + '/' + inodeName + '/' + tableId);
                }
            };
            return services;
        }
    }]);