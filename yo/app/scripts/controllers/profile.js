'use strict'

angular.module('hopsWorksApp')
        .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl', '$modalInstance',
          function (UserService, $location, $scope, md5, growl, $modalInstance) {

            var self = this;

            self.emailHash = '';
            self.master = {};

            self.user = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              registeredon: ''
            };

            self.loginCredes = {
              oldPassword: '',
              newPassword: '',
              confirmedPassword: ''
            };

            self.profile = function () {
              UserService.profile().then(
                      function (success) {
                        self.user = success.data;
                        self.emailHash = md5.createHash(self.user.email || '');
                        self.master = angular.copy(self.user);
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                      });
            };

            self.profile();

            self.updateProfile = function () {
              UserService.UpdateProfile(self.user).then(
                      function (success) {
                        self.user = success.data;
                        self.master = angular.copy(self.user);
                        growl.success("Your profile is now saved.", {title: 'Success', ttl: 5000, referenceId: 1});
                        $scope.profileForm.$setPristine();
                      }, function (error) {
                self.errorMsg = error.data.errorMsg;
                growl.error("Could not update your profile.", {title: 'Error', ttl: 5000, referenceId: 1});
              });
            };

            self.changeLoginCredentials = function () {

              if ($scope.credentialsForm.$valid) {
                UserService.changeLoginCredentials(self.loginCredes).then(
                        function (success) {
                          growl.success("Your password is now updated.", {title: 'Success', ttl: 5000, referenceId: 1});
                        }, function (error) {
                  self.errorMsg = error.data.errorMsg;
                  growl.error("Could not update your passowrd.", {title: 'Error', ttl: 5000, referenceId: 1});
                });
              }
            };

            self.reset = function () {
              self.user = angular.copy(self.master);
              $scope.profileForm.$setPristine();
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

          }]);
