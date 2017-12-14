/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('BannerService', ['$http', function ($http) {
        var service = {

            findBanner: function () {
                return $http.get('/api/banner');
            },
            findUserBanner: function () {
                return $http.get('/api/banner/user');
            }
        };
        return service;
    }]);
