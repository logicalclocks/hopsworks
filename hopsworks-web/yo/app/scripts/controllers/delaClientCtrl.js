angular.module('hopsWorksApp')
        .controller('DelaClientCtrl', ['DelaService', '$routeParams', '$scope', '$interval', 'growl', 'ModalService',
          function (DelaService, $routeParams, $scope, $interval, growl, ModalService) {
            var self = this;
            self.clientType = "NO_CLIENT";
            
            self.hopsUploadEnabled = function() {
              if(self.clientType === "FULL_CLIENT") {// DelaClientType.java
                return true;
              } else {
                return false;
              }
            };
            
            var getClientType = function() {
              DelaService.getClientType().then(
                  function (success) {                   
                    self.clientType = success.data.clientType;
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 1500});
                });
            };
            
            getClientType();
          }]);
