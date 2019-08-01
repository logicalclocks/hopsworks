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
    .factory('ApiKeyService', ['$http', function ($http) {
        var toQueryParam = function (scopes) {
            return scopes.length > 0 ? '&scope=' + scopes.join('&scope=') : '';
        };
        var services = {
            getScopes: function () {
                return $http.get('/api/users/apiKey/scopes');
            },
            getAll: function () {
                return $http.get('/api/users/apiKey');
            },
            get: function (name) {
                return $http.get('/api/users/apiKey/' + name);
            },
            create: function (key) {
                return $http.post('/api/users/apiKey?name=' + key.name + toQueryParam(key.scope));
            },
            edit: function (key) {
                return $http.put('/api/users/apiKey?action=update&name=' + key.name + toQueryParam(key.scope));
            },
            delete: function (name) {
                return $http.delete('/api/users/apiKey/' + name);
            },
            deleteAll: function () {
                return $http.delete('/api/users/apiKey');
            }
        };
        return services;
    }]);