'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', ['$location', 'AuthService', 'ProjectService', 'ModalService',
        function ($location, AuthService, ProjectService, ModalService) {

        var self = this;

        self.logout = function () {
            AuthService.logout(self.user).then(
              function (success) {
                $location.url('/login');
            }, function (error) {
                self.errorMessage = error.data.msg;
            });
        };

        self.proj = function(){
            ModalService.confirm('', 'Title', 'msg').then(function(){
                console.log("ok");
            }, function(){
                console.log("cancel");
            });
        };

    // Load all projects
    self.projects = ProjectService.query();

  }]);
