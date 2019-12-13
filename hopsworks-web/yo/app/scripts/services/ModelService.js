'use strict';

angular.module('hopsWorksApp')
    .factory('ModelService', ['$http', function($http) {
        return {
            getAll: function(projectId, query) {
                return $http.get('/api/project/' + projectId + '/models' + query);
            },
            get: function(projectId, mlId, query) {
                console.log('query?')
                console.log(query);
                return $http.get('/api/project/' + projectId + '/models/' + mlId + query);
            },
            deleteModel: function(projectId, id) {
                return $http.delete('/api/project/' + projectId + '/models/' + id);
            }
        }
    }]);