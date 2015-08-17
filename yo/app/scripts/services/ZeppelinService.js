'use strict';

angular.module('hopsWorksApp')
    .factory('ZeppelinService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
        return {
            interpreters: function () {
                return $http.get('/api/interpreter');
            },
            settings: function () {
                return $http.get('/api/interpreter/setting');
            },
            startInterpreter: function (settingId) {
                return $http.get('/api/interpreter/start/' + settingId);
            },
            stopInterpreter: function (settingId) {
                return $http.get('/api/interpreter/stop/' + settingId);
            },
            notebooks: function (projectId) {
                return $http.get('/api/notebook/'+ projectId);
            },
            tutorialNotebooks: function () {
                return $http.get('/api/notebook');
            },
            createNotebook: function (projectId) {
                return $http.get('/api/notebook/'+ projectId + '/new');
            }


        };
    }]);

