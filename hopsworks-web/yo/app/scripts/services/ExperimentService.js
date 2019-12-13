'use strict';

angular.module('hopsWorksApp')
    .factory('ExperimentService', ['$http', function($http) {
        return {
            getAll: function(projectId, query) {
                return $http.get('/api/project/' + projectId + '/experiments' + query);
            },
            get: function(projectId, mlId, query) {
                console.log('query?')
                console.log(query);
                return $http.get('/api/project/' + projectId + '/experiments/' + mlId + query);
            },
            deleteExperiment: function(projectId, id) {
                return $http.delete('/api/project/' + projectId + '/experiments/' + id);
            }
        }
    }]);