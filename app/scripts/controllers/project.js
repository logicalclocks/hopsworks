'use strict';

angular.module('hopsWorksApp')
    .controller('ProjectCtrl', ['$location','ProjectService',
        function ($location,ProjectService) {
            var self = this;

            self.read = function () {
                console.log();
                ProjectService.read().then(function (success) {
                    self.response = success.data.data.value;
                }, function (error) {
                    self.response = error.statusText;
                })

            };

            self.write = function () {
                console.log();
                ProjectService.write().then(function (success) {
                    self.response = success.data.value;
                }, function (error) {
                    self.response = error.statusText;
                })
            };

            self.doSomething = function () {
                console.log();
                ProjectService.doSomething().then(function (success) {
                    self.response = success.data.value;
                }, function (error) {
                    self.response = error.statusText;
                })
            };
        }]);
