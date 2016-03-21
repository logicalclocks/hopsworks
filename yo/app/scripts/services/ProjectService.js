'use strict';

angular.module('hopsWorksApp')
        .factory("ProjectService", ['$resource', function ($resource) {
            return $resource(
                    "/api/project/:id",
                    {id: "@id", projectName: "@projectName", inodeId: "@inodeId"},
            {
              "save": {
                method: "POST",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "example": {
                'method': 'POST',
                 url: '/api/project/starterProject'
              },
              "delete": {
                url: '/api/project/:id/delete',  
                method: 'POST'
              },
              "remove": {
                url: '/api/project/:id/remove',
                method: 'POST'
              },
              "update": {
                method: "PUT",
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              "projects": {
                'method': 'GET',
                isArray: true
              },
              "getAll": {
                url: '/api/project/getAll',
                'method': 'GET',
                isArray: true
              },
              "getQuotas":{
               url: '/api/project/:id/quotas',
               'method': 'GET'
                      },
              "getProjectInfo": {
                url: '/api/project/getProjectInfo/:projectName',
                'method': 'GET'
              },
              "getDatasetInfo": {
                url: '/api/project/getDatasetInfo/:inodeId',
                'method': 'GET'                  
              }
              
            }
            );
          }]);
