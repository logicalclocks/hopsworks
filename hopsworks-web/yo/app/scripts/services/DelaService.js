'use strict';
angular.module('hopsWorksApp')
        .factory('DelaService', ['$http', function ($http) {
            var service = {
              search: function (searchTerm) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/search?query=' + searchTerm});
              },
              getDetails: function (publicDSId) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/transfers/' + publicDSId});
              },
              getReadme: function (publicDSId, bootstrap) {
                var peers = {"bootstrap": bootstrap};
                return $http({
                  method: 'POST',
                  url: '/api/dela/datasets/' + publicDSId + '/readme',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: peers
                });
              },
              getUserContents: function () {
                return $http({
                  method: 'GET',
                  url: '/api/dela/transfers?filter=USER'});
              }
            };
            return service;
          }]);
