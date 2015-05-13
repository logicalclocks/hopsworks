'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCreatorCtrl', ['$modalInstance', '$scope', 'ProjectService', 'UserService',
    function ($modalInstance, $scope, ProjectService, UserService) {

      var self = this;

      self.card = {};
      self.cards = [];
      self.projectMembers = [];
      // We could instead implement a service to get all the available types but this will do it for now
      self.projectTypes = ['Cuneiform', 'Spark', 'Adam', 'Mapreduce', 'Zeppelin'];
      self.selectionProjectTypes = [];
      self.projectName = '';
      self.projectDesc = '';


      UserService.allcards().then(
        function (success) {
          self.cards = success.data;
        }, function (error) {
          self.errorMsg = error.data.msg;
        }
      );


      $scope.$watch('projectCreatorCtrl.card.selected', function (selected) {
        if (selected !== undefined) {
          if (self.projectMembers.indexOf(selected.email) == -1) {
            self.projectMembers.push(selected.email);
          }
          self.card.selected = undefined;
        }
      });


      self.exists = function exists(projectType) {
        var idx = self.selectionProjectTypes.indexOf(projectType);

        if (idx > -1) {
          self.selectionProjectTypes.splice(idx, 1);
        } else {
          self.selectionProjectTypes.push(projectType);
        }
      };


      self.removeMember = function (member) {
        self.projectMembers.splice(self.projectMembers.indexOf(member), 1);
      };


      self.createProject = function () {

        $scope.newProject = {
          'description': self.projectDesc,
          'name': self.projectName,
          'status': 0,
          'types': self.selectionProjectTypes,
          'members': self.projectMembers
        };

        ProjectService.save($scope.newProject).$promise.then(
          function (success) {
            $modalInstance.close($scope.newProject);
          }, function (error) {
            console.log('Error: ' + error);
          }
        );

      };


      self.close = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);