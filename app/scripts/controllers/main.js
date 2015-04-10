'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$location', 'AuthService', 'ProjectService', 'growl', function ($location, AuthService, ProjectService, growl) {

        var self = this;

        self.logout = function () {
            AuthService.logout(self.user).then(
              function (success) {
                $location.url('/login');
            }, function (error) {
                self.errorMessage = error.data.msg;
            });
        };



    // Load all projects
    self.projects = ProjectService.query();

  }]);
