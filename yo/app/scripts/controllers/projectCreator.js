'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectCreatorCtrl', ['$uibModalInstance', '$scope', 'ProjectService', 'UserService', 'growl',
          function ($uibModalInstance, $scope, ProjectService, UserService, growl) {

            var self = this;

            self.working = false;
            self.card = {};
            self.myCard = {};
            self.cards = [];

            self.projectMembers = [];
            self.projectTeam = [];
//            self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'WORKFLOWS'];
//            self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'TENSORFLOW'];
            self.projectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA'];
//            self.selectionProjectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'WORKFLOWS'];
//            self.selectionProjectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA', 'TENSORFLOW'];
            self.selectionProjectTypes = ['JOBS', 'ZEPPELIN', 'KAFKA'];

            self.projectName = '';
            self.projectDesc = '';

            self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#|.*?å|.*?Å|.*?ö|.*?Ö|.*?ä|.*?Ä|.*?ü|.*?U|.*?à|.*?á|.*?é|.*?è|.*?â|.*?ê|.*?î|.*?ï|.*?ë).*$/;


            UserService.profile().then(
                    function (success) {
                      if (success.data.email != undefined) {
                        self.myCard.email = success.data.email;
                        if (success.data.firstName != undefined) {
                          self.myCard.firstname = success.data.firstName;
                          if (success.data.email != undefined) {
                            self.myCard.lastname = success.data.lastName;
                            UserService.allcards().then(
                                    function (success) {
                                      self.cards = success.data;
                                      // remove my own 'card' from the list of members
                                      for (var i = 0, len = self.cards.length; i < len; i++) {
                                        if (self.cards[i].email === self.myCard.email) {
                                          self.cards.splice(i, 1);
                                          break;
                                        }
                                      }
                                      for (var i = 0, len = self.cards.length; i < len; i++) {
                                        if (self.cards[i].email === "agent@hops.io") {
                                          self.cards.splice(i, 1);
                                          break;
                                        }
                                      }
                                    }, function (error) {
                              self.errorMsg = error.data.msg;
                            });
                          }
                        }
                      }

                    },
                    function (error) {
                      self.errorMsg = error.data.errorMsg;
                    });


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


            self.addSelected = function exists(projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              if (idx > -1) {
                self.selectionProjectTypes.splice(idx, 1);
              } else {
                self.selectionProjectTypes.push(projectType);
              }
            };
            
            self.exists = function exists(projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              return idx > -1;
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
                        $uibModalInstance.close($scope.newProject);
                      }, function (error) {
                          self.working = false;
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
              }
              );
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);
