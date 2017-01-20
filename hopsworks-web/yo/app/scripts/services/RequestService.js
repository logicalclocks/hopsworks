'use strict';

angular.module('hopsWorksApp')
    .factory('RequestService', ['$http', function ($http) {
        var service = {
                accessRequest: function (dataSet) {
                    var regReq = {
                        method: 'POST',
                        url: '/api/request/access',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: dataSet
                    };

                    return $http(regReq);
                },
                joinRequest: function (dataSet) {
                    var regReq = {
                        method: 'POST',
                        url: '/api/request/join',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        data: dataSet
                    };

                    return $http(regReq);
                }
            };
        return service;
    }]);
