'use strict';

angular.module('hopsWorksApp')
    .controller('LoginCtrl', ['$location', '$rootScope', 'AuthService', function ($location, $rootScope, AuthService) {


        var self = this;
        self.user = {email: '', password: ''};

        $rootScope.isLoggedIn = false;

        self.login = function () {
            console.log(self.user);
            AuthService.login(self.user).then(function (success) {
                $location.path('/');
                $rootScope.isLoggedIn = true;
            }, function (error) {
                self.errorMessage = error.data.msg;
                $rootScope.isLoggedIn = false;
            })

        };
    }]);





