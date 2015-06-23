'use strict';

angular.module('hopsWorksApp')
        .controller('MemberCtrl', ['$scope', '$timeout', '$modalStack', 'MembersService', 'projectId', 'UserService',
          function ($scope, $timeout, $modalStack, MembersService, projectId, UserService) {
            var self = this;
            self.roles = ["Data scientist", "Data owner"];
            self.newRole = "";
            self.projectId = projectId;
            self.members = [];

            self.newMember = {
              'projectTeamPK':
                      {
                        'projectId': self.projectId,
                        'teamMember': ""
                      },
              'teamRole': ""
            };

            self.newMembers = {'projectTeam': []};
            self.card = {};
            self.cards = [];


            UserService.allcards().then(
                    function (success) {
                      self.cards = success.data;
                    }, function (error) {
              self.errorMsg = error.data.msg;
            }
            );


            $scope.$watch('memberCtrl.card.selected', function (selected) {
              if (selected !== undefined) {
                var index = -1;

                for (var i = 0, len = self.newMembers.projectTeam.length; i < len; i++) {
                  if (self.newMembers.projectTeam[i].projectTeamPK.teamMember === selected.email) {
                    index = i;
                    break;
                  }
                }

                if (index == -1) {
                  self.addNewMember(selected.email, self.roles[0]);
                }
                self.card.selected = undefined;
              }
            });


            var getMembers = function () {
              MembersService.query({id: self.projectId}).$promise.then(
                      function (success) {
                        self.members = success;
                      },
                      function (error) {
                      })
            }

            getMembers();

            self.addNewMember = function (user, role) {
              self.newMembers.projectTeam.push(
                      {'projectTeamPK':
                                {
                                  'projectId': self.projectId,
                                  'teamMember': user
                                },
                        'teamRole': role
                      }
              );
            }


            self.removeMember = function (email) {
              console.log('Removing; ' + email);

              var index = -1;

              for (var i = 0, len = self.newMembers.projectTeam.length; i < len; i++) {
                if (self.newMembers.projectTeam[i].projectTeamPK.teamMember === email) {
                  index = i;
                  break;
                }
              }

              if (index !== -1) {
                self.newMembers.projectTeam.splice(index, 1);
              }

            }



            self.addMembers = function () {
              MembersService.save({id: self.projectId}, self.newMembers).$promise.then(
                      function (success) {
                        //console.log(success);
                        self.newMembers = {'projectTeam': []};
                        getMembers();
                      }, function (error) {
                console.log(error);
              });
            }

            self.deleteMemberFromBackend = function (email) {
              MembersService.delete({id: self.projectId, email: email}).$promise.then(
                      function (success) {
                        getMembers();
                      }, function (error) {

              });
            }


            self.updateRole = function (email, role) {
              MembersService.update({id: self.projectId, email: email}, 'role=' + role).$promise.then(
                      function (success) {
                        getMembers();
                      }, function (error) {
                console.log(error);
              });
            }



            var secondsToWaitBeforeSave = 1;
            var timeout = null;

            self.showThisIndex = -1;

            self.selectChanged = function (index, email, teamRole) {
              timeout = $timeout(function () {
                self.updateRole(email, teamRole)
                self.showThisIndex = index;
              }, secondsToWaitBeforeSave * 1000);

              timeout = $timeout(function () {
                self.showThisIndex = -1;
              }, secondsToWaitBeforeSave * 4000);
            }

            self.close = function () {
              $modalStack.getTop().key.dismiss();
            };
          }]);

