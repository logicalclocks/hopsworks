angular.module('hopsWorksApp')
        .controller('EditDeviceCtrl', ['$uibModalInstance', 'DeviceManagementService',
        'growl', 'projectId', 'device',
          function ($uibModalInstance, DeviceManagementService, growl, projectId, device) {

            var self = this;
            self.projectId = projectId;
            self.device = device;
            self.deviceUuid = device.deviceUuid;
            self.alias = device.alias;
            self.state = device.state;
            self.working = false;
            self.states = [ 'Pending' , 'Approved', 'Disabled'];
            
            self.init = function() {
 
            };
            
            self.init();            

            self.editDevice = function () {
              self.working = true;
              
              self.device.alias=self.alias;
              self.device.state = self.state;                                

              DeviceManagementService.putDevice(self.projectId, device).then(
                      function (success) {
                        self.working = false;
                        $uibModalInstance.close(success);
                      }, function (error) {
                        self.working = false;
                        growl.error(error.data.errorMsg, {title: 'Failed to edit device.', ttl: 5000});
                      });      
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);
