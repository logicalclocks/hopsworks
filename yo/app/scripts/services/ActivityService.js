'use strict';

angular.module('hopsWorksApp')

    .factory('ActivityService', ['$http', function ($http) {
        var service = {
            getByUser: function () {
                return $http.get('/api/activity');
            },
            getByProjectId: function (id) {
                return $http.get('/api/activity/'+ id);
            }
        };
        return service;
    }]);
