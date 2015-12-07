'use strict';

angular.module('hopsWorksApp')
    .factory('ZeppelinService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
        return {
            settings: function () {
                return $http.get('/api/interpreter/setting');
            },
            startInterpreter: function (projectId, settingId) {
                return $http.get('/api/interpreter/'+projectId+'/start/'+settingId);
            },
            stopInterpreter: function (settingId) {
                return $http.get('/api/interpreter/stop/' + settingId);
            },
            notebooks: function (projectId) {
                return $http.get('/api/notebook/'+ projectId);
            },
            tutorialNotebooks: function () {
                return $http.get('/api/notebook/tutorial');
            },
            createNotebook: function (projectId) {
                return $http.get('/api/notebook/'+ projectId + '/new');
            },
            interpreters: function () {
                return $http.get('/api/interpreter/interpretersWithStatus');
            }
        };
    }]);

