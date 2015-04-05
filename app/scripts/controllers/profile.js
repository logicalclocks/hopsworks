/**
 * Created by Ermias on 2015-04-03.
 */
angular.module('hopsWorksApp')
    .controller('ProfileCtrl', ['UserService', '$location', '$scope', 'TransformRequest',
        function (UserService, $location, $scope, TransformRequest) {
        var self = this;
        self.master = {};
        self.user = {firstName: '',
                     lastName: '',
                     email: '',
                     telephoneNum: '',
                     registeredon: ''};
        self.loginCredes = {oldPassword:'',
                           newPassword:'',
                           confirmedPassword:''};
        self.profile = function () {
            UserService.profile().then(function (success) {
                console.log('Response to profile:-' + success.data);
                self.user = success.data;
                self.master = angular.copy(self.user);
                console.log('Response to profile:-' + self.user.firstName);
            }, function (error) {
                self.errorMsg = error.data.msg;
            })
        };
        self.profile();

        self.updateProfile = function () {
            UserService.UpdateProfile(self.user).then(function (success) {
                console.log(success.data);
                self.master = angular.copy(self.user);
                $scope.profileForm.$setPristine();
            }, function (error) {
                self.errorMsg = error.data.msg;
            })
        };

        self.changeLoginCredentials = function () {
            UserService.changeLoginCredentials(self.loginCredes).then(function (success){
                console.log(success.status);
            }, function (erroe){
                self.errorMsg = error.data.msg;
            })
        };

        self.reset = function () {
            self.user = angular.copy(self.master);
            $scope.profileForm.$setPristine();
        };

    }]);