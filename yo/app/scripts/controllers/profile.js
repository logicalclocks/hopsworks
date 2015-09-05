'use strict'

angular.module('hopsWorksApp')
        .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl', '$modalInstance',
          function (UserService, $location, $scope, md5, growl, $modalInstance) {

            var self = this;
            self.working = false;
            self.credentialWorking = false;

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
              self.working = true;
              UserService.UpdateProfile(self.user).then(
                      function (success) {
                        self.working = false;
                        self.user = success.data;
                        self.master = angular.copy(self.user);
                        growl.success("Your profile is now saved.", {title: 'Success', ttl: 5000, referenceId: 1});
                        $scope.profileForm.$setPristine();
                      }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Could not update your profile.", {title: 'Error', ttl: 5000, referenceId: 1});
              });
            };

            self.changeLoginCredentials = function () {
              self.credentialWorking = true;
              if ($scope.credentialsForm.$valid) {
                UserService.changeLoginCredentials(self.loginCredes).then(
                        function (success) {
                          self.credentialWorking = false;
                          growl.success("Your password is now updated.", {title: 'Success', ttl: 5000, referenceId: 1});
                        }, function (error) {
                          self.credentialWorking = false;
                          self.errorMsg = error.data.errorMsg;
                          growl.error("Could not update your password.", {title: 'Error', ttl: 5000, referenceId: 1});
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
