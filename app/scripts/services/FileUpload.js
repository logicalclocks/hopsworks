'use strict';

angular.module('hopsWorksApp')

    .factory('FileUpload', ['$http', function ($http) {
        return function (projectId, path) {
            return new Flow({target:'/hopsworks/api/project/'+ projectId  +'/dataset/upload/'+ path});
        };
    }]);

