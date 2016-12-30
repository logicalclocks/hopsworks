'use strict';

angular.module('hopsWorksApp')
        .factory('MembersService', ['$resource', function ($resource) {
            return $resource("/api/project/:id/projectMembers/:email",
                    {id: "@id", email: "@email"},
            {
              'save': {
                method: 'POST',
                headers: {'Content-Type': 'application/json; charset=UTF-8'}
              },
              'update': {method: 'POST'}
            });
          }]);
