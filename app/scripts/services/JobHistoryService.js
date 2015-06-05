'use strict';

angular.module('hopsWorksApp')

    .factory('JobHistoryService', ['$http', function ($http) {
        var service = {
            getByProjectAndType: function (projectId, type) {
                return $http.get('/api/jobs/'+projectId+'/history/'+type.toUpperCase());
            }
        };
        return service;
    }]);
