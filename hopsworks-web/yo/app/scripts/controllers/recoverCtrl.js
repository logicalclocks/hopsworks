/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('RecoverCtrl', ['$location', 'AuthService', '$scope', 'SecurityQuestions',
          function ($location, AuthService, $scope, SecurityQuestions) {

            var self = this;
            self.working = false;
            self.securityQuestions = SecurityQuestions.getQuestions();
            self.user = {email: '',
              securityQuestion: '',
              securityAnswer: '',
              newPassword: '1234',
              confirmedPassword: '1234'};
            var empty = angular.copy(self.user);
            self.send = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.recoveryForm.$valid) {
                self.working = true;
                AuthService.recover(self.user).then(
                        function (success) {
                          self.user = angular.copy(empty);
                          $scope.recoveryForm.$setPristine();
                          self.working = false;
                          self.successMessage = success.data.successMessage;
                          //$location.path('/login');
                        }, function (error) {
                          self.working = false;
                          self.errorMessage = error.data.errorMsg;
                          console.log(self.errorMessage);
                });
              }
            };


          }]);
