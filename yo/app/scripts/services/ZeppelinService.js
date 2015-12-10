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
            createNotebook: function (projectId, noteName) {
              var regReq = {
                    method: 'POST',
                    url: '/api/notebook/'+ projectId + '/new',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: noteName
                  };
                  return $http(regReq);             
            },
            interpreters: function () {
                return $http.get('/api/interpreter/interpretersWithStatus');
            }
        };
    }]);

