/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('BannerService', ['$http', function ($http) {
        var service = {

            findBanner: function () {
                return $http.get('/api/banner');
            }
        };
        return service;
    }]);
