/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
  .factory('CertService', ['$http', function ($http) {
      var services = {
        downloadProjectCert: function (id, password) {
          var req = {
            method: 'POST',
            url: '/api/project/' + id + '/downloadCert',
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            data: $.param({password: password})
          };
          return $http(req);
        }
      };
      return services;
    }]);


