'use strict';

angular.module('hopsWorksApp')
  .controller('HomeCtrl', ['ProjectService', 'ModalService', 'growl',
    function (ProjectService, ModalService, growl) {

      var self = this;
      self.projects = [];


      // Load all projects
      ProjectService.query().$promise.then(
        function(success) {
          self.projects = success;
        }, function (error){
          console.log('Error: ' + error);
        }
      );

      self.newProject = function () {
        ModalService.createProject('lg', 'New project', '').then(
          function (success) {
            self.projects = ProjectService.query();
            growl.success("Successfully created project: " + success.name, {title: 'Success', ttl: 5000});
          }, function () {
            growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
          });
      };

    }]);
