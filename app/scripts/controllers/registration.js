'use strict';

angular.module('hopsWorksApp')
  .controller('RegCtrl', ['RegisterService', function (RegisterService) {

    var self = this;

    self.newUser = {
      firstName: '',
      lastName: '',
      email: '',
      tel: '',
      chosenPassword: '',
      repeatedPassword: '',
      secQuestion: '',
      secAnswer: '',
      ToS: ''
    };

  }]);
