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
        .factory('UserService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            return {
              UpdateProfile: function (user) {
                return $http.post('/api/user/updateProfile', TransformRequest.jQueryStyle(user));
              },
              profile: function () {
                return $http.get('/api/user/profile');
              },
              changeLoginCredentials: function (newCredentials) {
                return $http.post('/api/user/changeLoginCredentials', TransformRequest.jQueryStyle(newCredentials));
              },
              allcards: function () {
                return $http.get('/api/user/allcards');
              },
              createProject: function (newProject) {
                return $http.post('/api/user/newProject', newProject);
              },
              getRole: function (projectId) {
                return $http.post('/api/user/getRole', "projectId=" + projectId);
              },
              changeTwoFactor: function (newCredentials) {
                return $http.post('/api/user/changeTwoFactor', TransformRequest.jQueryStyle(newCredentials));
              },
              getQR: function (pwd) {
                return $http.post('/api/user/getQRCode', "password=" + pwd);
              },
              addSshKey: function (sshKey) {
              //addSshKey: function (name, sshKey) {
                return $http({
                  method: 'post',
                  url: '/api/user/addSshKey',
                  headers: {'Content-Type': 'application/json'},
                  isArray: false,
                  data: sshKey
                });

                //return $http.post('/api/user/addSshKey', "name=" + name + "&sshKey=" + sshKey);
              },
              removeSshKey: function (name) {
                return $http.post('/api/user/removeSshKey', "name="+name);
              },
              getSshKeys: function () {
                return $http.get('/api/user/getSshKeys');
              }
            };
          }]);
