/**
 * Created by Ermias on 2015-04-03.
 */
'use strict';

angular.module('hopsWorksApp')

    .factory('UserService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
        return {
            UpdateProfile: function (user) {
                return $http.post('/api/user/updateProfile', TransformRequest.jQueryStyle(user));
            },
            profile: function () {
                return $http.get('/api/user/profile');
            },
            changeLoginCredentials: function (newCredentials) {
                return $http.post('/api/user/changeLoginCredentials', TransformRequest.jQueryStyle(newCredentials));
            }
        };
    }]);