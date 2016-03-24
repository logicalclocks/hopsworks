'use strict';

angular.module('hopsWorksApp')
    .factory('ZeppelinService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
        return {
            settings: function () {
                return $http.get('/api/interpreter/setting');
            },
            restartInterpreter: function (settingId) {
                return $http.put('/api/interpreter/setting/restart/' + settingId);
            },
            notebooks: function () {
                return $http.get('/api/notebook/');
            },
            createNotebook: function (noteName) {
              var regReq = {
                    method: 'POST',
                    url: '/api/notebook/new',
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

