'use strict';

angular.module('hopsWorksApp')
        .factory("ProjectService", ['$resource', function ($resource) {
            return $resource(
                    "/api/project/:id",
                    {id: "@id"},
            {
              "save": {
                method: "POST",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "delete": {
                method: 'DELETE'
              },
              "remove": {
                url: '/api/project/:id/remove',
                method: 'DELETE'
              },
              "update": {
                method: "PUT",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "projects": {
                'method': 'GET',
                isArray: true
              }
            }
            );
          }]);
