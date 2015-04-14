'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$location', '$cookieStore', 'AuthService', 'ProjectService', 'ModalService',
    function ($location, $cookieStore, AuthService, ProjectService, ModalService) {

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

      self.newProject = function(){
        ModalService.project('lg', 'New project', 'My message here...');
      }



      // Load all projects
      self.projects = ProjectService.query();

    }]);
