'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectCreatorCtrl', ['$modalInstance', '$scope', 'ProjectService', 'UserService', 'growl',
          function ($modalInstance, $scope, ProjectService, UserService, growl) {

            var self = this;
            self.working = false;
            self.card = {};
            self.cards = [];

            self.projectMembers = [];
            self.projectTeam = [];
            self.projectTypes = ['JOBS', 'ZEPPELIN']; 
//            self.projectTypes = ['JOBS', 'ZEPPELIN', 'BIOBANKING', 'CHARON', 'SSH']; 

            self.selectionProjectTypes = ['JOBS', 'ZEPPELIN'];
            self.projectName = '';
            self.projectDesc = '';

            self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#).*$/;

            UserService.allcards().then(
                    function (success) {
                      self.cards = success.data;
                    }, function (error) {
              self.errorMsg = error.data.msg;
            }
            );


            $scope.$watch('projectCreatorCtrl.card.selected', function (selected) {
              var projectTeamPK = {'projectId': "", 'teamMember': ""};
              var projectTeam = {'projectTeamPK': projectTeamPK};
              if (selected !== undefined) {
                projectTeamPK.teamMember = selected.email;
                if (self.projectMembers.indexOf(selected.email) == -1) {
                  self.projectMembers.push(selected.email);
                  self.projectTeam.push(projectTeam);
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
              self.working = true;
              $scope.newProject = {
                'projectName': self.projectName,
                'description': self.projectDesc,
                'retentionPeriod': "",
                'status': 0,
                'services': self.selectionProjectTypes,
                'projectTeam': self.projectTeam
              };

              ProjectService.save($scope.newProject).$promise.then(
                      function (success) {
                        self.working = false;
                        growl.success(success.successMessage, {title: 'Success', ttl: 2000});
                        if (success.errorMsg) {
                          growl.warning(success.errorMsg, {title: 'Error', ttl: 10000});
                        }
                        if (success.fieldErrors.length > 0) {
                          success.fieldErrors.forEach(function (entry) {
                            growl.warning(entry + ' could not be added', {title: 'Error', ttl: 10000});
                          });

                        }
                        $modalInstance.close($scope.newProject);
                      }, function (error) {
                        self.working = false;                        
              }
              );
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

          }]);
