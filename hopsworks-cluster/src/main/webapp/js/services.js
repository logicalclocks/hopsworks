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