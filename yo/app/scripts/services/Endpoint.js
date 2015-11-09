'use strict';

angular.module('hopsWorksApp')
    .factory('Endpoint', ['$http', function ($http) {
        return function () {
            var service = {

                findEndpoint: function () {
                    return $http.get('/api/endpoint');
                }
            };
            return service;
        }
    }]);
