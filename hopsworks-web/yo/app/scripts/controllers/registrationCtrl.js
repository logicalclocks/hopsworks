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
        .controller('RegCtrl', ['AuthService', '$location', '$scope', 'SecurityQuestions', '$routeParams', '$cookies',
          function (AuthService, $location, $scope, SecurityQuestions, $routeParams, $cookies) {
          
            var self = this;
            self.securityQuestions = SecurityQuestions.getQuestions();
            self.working = false;
            self.otp = $cookies.get('otp');
            self.newUser = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              chosenPassword: '',
              repeatedPassword: '',
              securityQuestion: '',
              securityAnswer: '',
              ToS: '',
              authType: 'Mobile',
              twoFactor: false,
              toursEnabled: true,
              orgName: '',
              dep: '',
              street: '',
              city: '',
              postCode: '',
              country: ''
            };
            
            self.userEmail ='';
            
            self.QR = $routeParams.QR;
            var empty = angular.copy(self.user);
            self.register = function () {
              self.successMessage = null;
              self.errorMessage = null;
              if ($scope.registerForm.$valid) {
                self.working = true;
                if (self.otp === 'false' || !self.newUser.twoFactor) {
                  AuthService.register(self.newUser).then(
                          function (success) {
                            self.user = angular.copy(empty);
                            $scope.registerForm.$setPristine();
                            self.successMessage = success.data.successMessage;
                            self.working = false;
                            //$location.path('/login');
                          }, function (error) {
                             self.working = false;
                             self.errorMessage = error.data.errorMsg;
                  });
                }else  if (self.newUser.authType === 'Mobile') {
                  AuthService.register(self.newUser).then(
                          function (success) {
                            self.user = angular.copy(empty);                         
                            $scope.registerForm.$setPristine();
                            self.successMessage = success.data.successMessage;
                            self.working = false;
                            $location.path("/qrCode/" + success.data.QRCode);
                            $location.replace();
                            //$location.path('/login');
                          }, function (error) {
                    self.working = false;
                    self.errorMessage = error.data.errorMsg;
                  });
                }
              };
            };
            self.countries = getAllCountries();
          }]);
