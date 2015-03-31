'use strict';

angular.module('hopsWorksApp')
  .controller('RegCtrl', ['AuthService','$location', function (AuthService,$location) {

    var self = this;

    self.newUser = {
      firstName: '',
      lastName: '',
      email: '',
      telephoneNum: '',
      chosenPassword: '',
      repeatedPassword: '',
      securityQuestion: '',
      securityAnswer: '',
      ToS: ''
    };


    self.register = function () {
      console.log(self.newUser);
      AuthService.register(self.newUser).then(function (success) {
        $location.path('/');
      }, function (error) {
        self.errorMessage = error.data.msg;
      })

    };




  }]);




