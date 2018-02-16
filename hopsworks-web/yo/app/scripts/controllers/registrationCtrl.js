/*
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
 *
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
