/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('EndpointService', ['$http', function ($http) {
            var service = {

                findEndpoint: function () {
                    return $http.get('/api/endpoint');
                }
            };
            return service;
    }]);
