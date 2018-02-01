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

var services = angular.module("services", []);

services.factory("ClusterService", ['$http', '$httpParamSerializerJQLike', 
  function ($http, $httpParamSerializerJQLike) {
    var baseURL = getApiLocationBase();
    var service = {
      register: function (cluster) {
        var regReq = {
          method: 'POST',
          url: baseURL + '/cluster/register',
          headers: {
            'Content-Type': 'application/json'
          },
          data: cluster
        };
        return $http(regReq);
      },
      registerCluster: function (cluster) {
        var regReq = {
          method: 'POST',
          url: baseURL + '/cluster/register/existing',
          headers: {
            'Content-Type': 'application/json'
          },
          data: cluster
        };
        return $http(regReq);
      },
      confirmRegister: function (validationKey) {
        var regReq = {
          method: 'GET',
          url: baseURL + '/cluster/register/confirm/' + validationKey
        };
        return $http(regReq);
      },
      unregister: function (cluster) {
        var regReq = {
          method: 'POST',
          url: baseURL + '/cluster/unregister',
          headers: {
            'Content-Type': 'application/json'
          },
          data: cluster
        };
        return $http(regReq);
      },
      confirmUnregister: function (validationKey) {
        var regReq = {
          method: 'GET',
          url: baseURL + '/cluster/unregister/confirm/' + validationKey
        };
        return $http(regReq);
      },
      getAllClusters: function (cluster) {
        var regReq = {
          method: 'POST',
          url: baseURL + '/cluster/all',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
          },
          data: $httpParamSerializerJQLike ({
            email: cluster.email,
            pwd: cluster.chosenPassword
          })
        };
        return $http(regReq);
      },
      getCluster: function (cluster) {
        var regReq = {
          method: 'POST',
          url: baseURL + '/cluster',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
          },
          data: $httpParamSerializerJQLike ({
            email: cluster.email,
            pwd: cluster.chosenPassword,
            orgName: cluster.organizationName,
            orgUnitName: cluster.organizationalUnitName
          })
        };
        return $http(regReq);
      }
    };
    return service;
  }]);