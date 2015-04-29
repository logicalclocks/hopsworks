'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCreatorCtrl', ['$modalInstance', '$scope', 'ProjectService', 'UserService', 'growl',
    function ($modalInstance, $scope, ProjectService, UserService, growl) {

      var self = this;

      self.card = {};
      self.cards = [];

      self.projectMembers = [];
      self.projectTeam = [];
      // We could instead implement a service to get all the available types but this will do it for now
      self.projectTypes = ['CUNEIFORM','SAMPLES','STUDY_INFO', 'SPARK', 'ADAM', 'MAPREDUCE', 'YARN', 'ZEPPELIN'];



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
        var studyTeamPK = {'name':"", 'teamMember':""};
        var studyTeam = {'studyTeamPK': studyTeamPK};
        if (selected !== undefined) {
            studyTeamPK.name= self.projectName;
            studyTeamPK.teamMember=selected.email;
          if (self.projectMembers.indexOf(selected.email) == -1) {
            self.projectMembers.push(selected.email);
            self.projectTeam.push(studyTeam);
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
          'projectName': self.projectName,
          'description': self.projectDesc,
          'retentionPeriod':"",
          'status': 0,
          'services': self.selectionProjectTypes,
          'projectTeam': self.projectTeam
        };

        ProjectService.save($scope.newProject).$promise.then(
          function (success) {
              console.log(success)
              growl.success(success.data.successMessage , {title: 'Success', ttl: 5000});
            $modalInstance.close($scope.newProject);
          }, function (error) {
                growl.success(error.data.errorMsg, {title: 'Error', ttl: 5000});
            console.log('Error: ' + error)
          }
        );

      };


      self.close = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);
