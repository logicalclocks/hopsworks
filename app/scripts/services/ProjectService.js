'use strict';

angular.module('hopsWorksApp')
    .factory('ProjectService', ['$http',
        function ($http) {
            return {
                read: function () {
                    return $http.get('/api/project/read');
                },
                write: function () {
                    return $http.get('/api/project/write');
                },
                doSomething: function () {
                    return $http.get('/api/project/doSomething');
                }
            };
        }]);

