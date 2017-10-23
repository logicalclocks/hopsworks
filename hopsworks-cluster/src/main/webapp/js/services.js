'use strict';

var services = angular.module("services", []);

services.factory("ClusterService", ['$http', function ($http) {
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
      confirmRegister: function (validationKey) {
        var regReq = {
          method: 'PUT',
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
          method: 'PUT',
          url: baseURL + '/cluster/unregister/confirm/' + validationKey
        };
        return $http(regReq);
      }
    };
    return service;
  }]);