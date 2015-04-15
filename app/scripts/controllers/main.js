'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$location', '$cookieStore', 'AuthService', 'ProjectService', 'ModalService', 'growl',
    function ($location, $cookieStore, AuthService, ProjectService, ModalService, growl) {

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


      self.newProject = function () {
        ModalService.createProject('lg', 'New project', '').then(
          function (success) {
            self.projects = ProjectService.query();
            growl.success("Successfully created project: " + success.name, {title: 'Success', ttl: 5000});
          }, function () {
            growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
          });
      };


      // Load all projects
      self.projects = ProjectService.query();

    }]);
