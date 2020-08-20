/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Controller for the Storage-Connector-Info view
 */
angular.module('hopsWorksApp')
    .controller('storageConnectorViewInfoCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl', 'projectId',
        'storageConnector', 'featurestore', 'settings',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, storageConnector, featurestore, settings) {

            /**
             * Initialize controller state
             */
                //Controller Inputs
            var self = this;
            self.projectId = projectId;
            self.storageConnector = storageConnector
            self.featurestore = featurestore;
            self.settings = settings
            //Constants
            self.hopsfsConnectorType = self.settings.hopsfsConnectorType
            self.s3ConnectorType = self.settings.s3ConnectorType
            self.jdbcConnectorType = self.settings.jdbcConnectorType
            self.preProcessedArgs = []
            self.hasPwd = false;
            self.showPwd = false;
            self.DEFAULT_PASSWORD_TEXT = "<SECRETPASSWORD>";
            self.pwdToShow = self.DEFAULT_PASSWORD_TEXT;
            self.connectorPwd = "";
            /**
             * PreProcess connector
             */
            self.preProcessConnector = function () {
                if (self.storageConnector.storageConnectorType == self.jdbcConnectorType) {
                    var args = self.storageConnector.arguments
                    args = args + ''
                    var argsList = args.split(",")
                    var newArgs = []
                    for (var j = 0; j < argsList.length; j++) {
                        var argValue = argsList[j].split("=")
                        var argPair = {
                            "name": argValue[0],
                            "value": argValue.length > 1 ? argValue[1] : "DEFAULT"
                        }
                        newArgs.push(argPair);
                        if(argPair.name == "password") {
                            self.hasPwd = true;
                            self.connectorPwd = argPair.value;
                        }
                    }
                    self.preProcessedArgs = newArgs
                }
            }

            /**
             * Initialization function
             */
            self.init = function () {
                self.preProcessConnector()
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };

            self.showPlainConnectorPassword = function () {
                self.showPwd = !self.showPwd;
                if(self.showPwd) {
                    self.pwdToShow = self.connectorPwd;
                } else {
                    self.pwdToShow = self.DEFAULT_PASSWORD_TEXT;
                }
            }

            self.init()
        }]);

