/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').controller('WSController', ['ngWebSocket', function ($websocket) {

    }])
        .controller('MessengerController', function ($scope, Messages) {
            $scope.username = 'anonymous';

            $scope.Messages = Messages;

            $scope.submit = function (new_message) {
                if (!new_message) {
                    return;
                }
                Messages.send({
                    username: $scope.username,
                    message: new_message
                });
                $scope.new_message = '';
            };

        })
        .filter('capitalize', function () {
            function capWord(txt) {
                return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
            }
            return function (input, isEveryWord) {
                return (!input) ? '' : (!isEveryWord) ? capWord(input) : input.replace(/([^\W_]+[^\s-]*) */g, capWord);
            };
        });