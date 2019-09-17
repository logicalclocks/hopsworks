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
 * Controller for the process of selecting a feature type for defining schemas of featuregroup/training datasets
 */
angular.module('hopsWorksApp')
    .controller('selectFeatureTypeCtrl', ['$uibModalInstance', 'FeaturestoreService',
        'growl', 'ModalService', '$scope', 'settings',
        function ($uibModalInstance, FeaturestoreService, growl, ModalService, $scope, settings) {
            /**
             * Initialize state
             */
            var self = this;

            //Controller Inputs
            self.settings = settings

            //Constants
            self.hiveDataTypes = self.settings.suggestedHiveFeatureTypes
            self.mysqlDataTypes = self.settings.suggestedMysqlFeatureTypes

            //State
            self.customType;
            self.predefinedHiveType;
            self.predefinedMysqlType;
            self.duplicateTypeSelection = 1;
            self.noSelection = 1;
            self.wrong_values = 1;
            self.predefinedHiveType = self.hiveDataTypes[0]
            self.predefinedMysqlType = self.hiveDataTypes[0]



            /**
             * Function called when the "Save" button is pressed.
             * Validates parameters and then returns the selected feature type to the parent-modal
             */
            self.selectFeatureType = function () {
                self.duplicateTypeSelection = 1;
                self.noSelection = 1;
                self.wrong_values = 1;
                if(self.customType && self.predefinedHiveType != "None") {
                    self.duplicateTypeSelection = -1;
                    self.wrong_values = -1;
                }
                if(self.customType && self.predefinedMysqlType != "None") {
                    self.duplicateTypeSelection = -1;
                    self.wrong_values = -1;
                }
                if(self.predefinedHiveType != "None" && self.predefinedMysqlType != "None") {
                    self.duplicateTypeSelection = -1;
                    self.wrong_values = -1;
                }
                if(!self.customType && (self.predefinedHiveType == "None" || !self.predefinedHiveType) &&
                    (self.predefinedMysqlType == "None" || !self.predefinedMysqlType)) {
                    self.noSelection = -1;
                    self.wrong_values = -1;
                }
                if (self.wrong_values === -1) {
                    return;
                }
                if(self.customType){
                    $uibModalInstance.close(self.customType);
                }
                if(self.predefinedHiveType != "None"){
                    $uibModalInstance.close(self.predefinedHiveType);
                }
                if(self.predefinedMysqlType != "None") {
                    $uibModalInstance.close(self.predefinedMysqlType);
                }
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);