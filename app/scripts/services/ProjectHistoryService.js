'use strict';

angular.module('hopsWorksApp')

    .factory('ProjectHistoryService', ['$http', function ($http) {
        var service = {
            getByUser: function () {
                return $http.get('/api/history')
                    .then(function (response) {
                        return response;
                    });
            },
            getByProjectId: function (id) {
                return $http.get('/api/history/id')
                    .then(function (response) {
                        return response;
                    });
            }
        };
        return service;
    }]);
