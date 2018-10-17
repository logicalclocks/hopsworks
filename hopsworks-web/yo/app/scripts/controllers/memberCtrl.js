/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('MemberCtrl', ['$scope', '$timeout', '$uibModalStack', '$location','MembersService', 'projectId', 'UserService', 'growl',
          function ($scope, $timeout, $uibModalStack, $location, MembersService, projectId, UserService, growl) {
            var self = this;
            self.roles = ["Data scientist", "Data owner"];
            self.newRole = "";
            self.projectId = projectId;
            self.members = [];
            self.projectOwner = "";
            
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
            self.myCard = {};
            self.cards = [];

            var getMembers = function () {
              MembersService.query({id: self.projectId}).$promise.then(
                      function (success) {
                        self.members = success;
                        if(self.members.length > 0){
                          self.projectOwner = self.members[0].project.owner;
                          UserService.allcards().then(
                                  function (success) {
                                    self.cards = success.data;
                                    // remove my own 'card' from the list of members
                                    // remove project owner as well, since he is always a 
                                    // member of the project
                                    var countRemoved = 0;
                                    var i = self.cards.length;
                                    while(i--) {
                                        if (self.cards[i].email === self.myCard.email ||
                                                self.cards[i].email === self.projectOwner.email ||
                                                self.cards[i].email === "agent@hops.io") {
                                          self.cards.splice(i, 1);
                                          countRemoved++;
                                          if(countRemoved === 3){
                                            break;
                                          }
                                        }
                                    }
                                  }, function (error) {
                            self.errorMsg = error.data.msg;
                          }
                          );
                          //Get current user team role
                          self.members.forEach(function (member) {
                            if (member.user.email === self.myCard.email) {
                              self.teamRole = member.teamRole;
                              return;
                            }
                          });
                        }                       
                      },
                      function (error) {
                      });
            };
            getMembers();
            
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


           
            
            var getCard = function () {
              UserService.profile().then(
                      function (success) {
                        self.myCard.email = success.data.email;
                        self.myCard.firstname = success.data.firstName;
                        self.myCard.lastname = success.data.lastName;
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                      });
            };

            getCard();


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
            };


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

            };



            self.addMembers = function () {
              MembersService.save({id: self.projectId}, self.newMembers).$promise.then(
                      function (success) {
                        //console.log(success);
                        self.newMembers = {'projectTeam': []};
                        getMembers();
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };

            self.deleteMemberFromBackend = function (email) {
              MembersService.delete({id: self.projectId, email: email}).$promise.then(
                      function (success) {
                        if(email === self.myCard.email){
                          self.close();
                          $location.path('/');
                          $location.replace();
                        } else {
                          getMembers();
                        }
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }

              });
            };


            self.updateRole = function (email, role) {
              MembersService.update({id: self.projectId, email: email}, 'role=' + role).$promise.then(
                      function (success) {
                        getMembers();
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 8000});
                      }
              });
            };



            var secondsToWaitBeforeSave = 1;
            var timeout = null;

            self.showThisIndex = -1;

            self.selectChanged = function (index, email, teamRole) {
              timeout = $timeout(function () {
                self.updateRole(email, teamRole);
                self.showThisIndex = index;
              }, secondsToWaitBeforeSave * 1000);

              timeout = $timeout(function () {
                self.showThisIndex = -1;
              }, secondsToWaitBeforeSave * 4000);
            };

            self.close = function () {
              $uibModalStack.getTop().key.dismiss();
            };
          }]);
