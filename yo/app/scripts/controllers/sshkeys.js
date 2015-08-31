'use strict'

angular.module('hopsWorksApp')
        .controller('SshkeysCtrl', ['UserService', '$location', '$scope', 'growl', '$modalInstance',
          function (UserService, $location, $scope, growl, $modalInstance) {

            var self = this;
            self.working = false;

            self.keys = [];

            self.key = {
              name: '',
              publicKey: '',
              status: false
            };

            self.addSshKey = function () {
              self.working = true;
              UserService.addSshKey(key.name, key.publicKey).then(
                      function (success) {
                        self.keys.push(success.data);
                        self.keys[self.keys.length-1].status = false;
                        growl.success("Your ssh key has been added.", {title: 'Success', ttl: 5000, referenceId: 1});
                        self.working = false;
                      },
                      function (error) {
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Could not add your ssh keys.", {title: 'Error', ttl: 5000, referenceId: 1});
                      });
            };

            self.removeSshKey = function (keyName) {
              self.working = true;
              UserService.removeSshKey(keyName).then(
                      function (success) {
                        growl.success("Your ssh key has been removed.", {title: 'Success', ttl: 5000, referenceId: 1});
                        self.working = false;
                      }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Could not remove your ssh keys.", {title: 'Error', ttl: 5000, referenceId: 1});
              });
            };

            self.getSshKeys = function () {
              self.working = true;
              UserService.getSshKeys().then(
                  function (success) {
                    self.working = false;
                    self.keys = success.data;
                  }, function (error) {
                    self.working = false;
                    self.errorMsg = error.data.errorMsg;
                    growl.error("Could not find any ssh keys.", {title: 'Error', ttl: 5000, referenceId: 1});
                  });
            };



            self.reset = function () {
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };


            self.getSshKeys();

          }]);
