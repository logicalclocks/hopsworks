'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$location', '$cookieStore', 'AuthService', 'ProjectService',
    function ($location, $cookieStore, AuthService, ProjectService) {

      var self = this;

      self.email = $cookieStore.get('email');

      self.logout = function () {
        AuthService.logout(self.user).then(
          function (success) {
            $location.url('/login');
            $cookieStore.remove('email');
          }, function (error) {
            self.errorMessage = error.data.msg;
          });
      };

      // Load all projects
      self.projects = ProjectService.query();

    }]);
