'use strict';
angular.module('hopsWorksApp')
    .controller('MessageCtrl', ['$cookies','$modalInstance','MessageService','RequestService','growl','md5','selected',
        function ($cookies, $modalInstance, MessageService, RequestService, growl, md5, selected) {

            var self = this;
            self.email = $cookies['email'];
            self.refreshing = false;
            self.loading = false;
            self.loadingMsg = false;
            self.filterText;
            self.selectedMsg = selected;
            self.trash;
            
            
            var message = {to:[{},{}], from:"", subject:"", date:"", msg:""};

            self.messages = MessageService.getMessages();
            self.trash = MessageService.getTrash();

            self.select = function(msg){
                msg.unread = false;
                self.selectedMsg = msg;
            };

             self.emailMd5Hash = function (email) {
                return md5.createHash(email || '');
            };

            self.close = function () {
                $modalInstance.dismiss('cancel');
            };
        }]);

