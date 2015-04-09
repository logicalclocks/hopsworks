'use strict';

angular.module('hopsWorksApp')
  .controller('ProjectCtrl', ['$location', 'ProjectService', function ($location, ProjectService) {
    var self = this;

    self.projects = function () {
      console.log();
      ProjectService.projects().then(function (success) {
        self.response = success.data.data.value;
      }, function (error) {
        self.response = error.statusText;
      })

    };


  }]);
