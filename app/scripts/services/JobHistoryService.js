'use strict';

angular.module('hopsWorksApp')

    .factory('JobHistoryService', ['$http', function ($http) {
        var service = {
            getByProjectAndType: function (projectId, type) {
                return $http.get('/api/project/'+projectId+'/jobhistory/'+type.toUpperCase());
            }
        };
        return service;
    }]);
