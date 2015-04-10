'use strict'

angular.module('hopsWorksApp')
  .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'md5', 'growl',
    function (UserService, $location, $scope, md5, growl) {

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
            self.errorMsg = error.data.msg;
          })
      };

      self.profile();

      self.updateProfile = function () {
        UserService.UpdateProfile(self.user).then(
          function (success) {
            self.user = success.data;
            self.master = angular.copy(self.user);
            growl.success("Your profile is now saved.", {title: 'Success', ttl: 5000});
            $scope.profileForm.$setPristine();
          }, function (error) {
            self.errorMsg = error.data.msg;
            growl.error("Could not update your profile.", {title: 'Error', ttl: 5000});
          })
      };

      self.changeLoginCredentials = function () {
        UserService.changeLoginCredentials(self.loginCredes).then(
          function (success) {
            growl.success("Your password is now updated.", {title: 'Success', ttl: 5000});
          }, function (error) {
            self.errorMsg = error.data.msg;
            growl.error("Could not update your passowrd.", {title: 'Error', ttl: 5000});
          })
      };

      self.reset = function () {
        self.user = angular.copy(self.master);
        $scope.profileForm.$setPristine();
      };

    }]);
