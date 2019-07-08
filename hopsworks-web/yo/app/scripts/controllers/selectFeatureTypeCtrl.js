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

            //State
            self.customType;
            self.predefinedType;
            self.duplicateTypeSelection = 1;
            self.noSelection = 1;
            self.wrong_values = 1;
            $scope.selected = self.hiveDataTypes[0]

            //Constants
            self.hiveDataTypes = self.settings.suggestedFeatureDataTypes()


            /**
             * Function called when the "Save" button is pressed.
             * Validates parameters and then returns the selected feature type to the parent-modal
             */
            self.selectFeatureType = function () {
                self.duplicateTypeSelection = 1;
                self.noSelection = 1;
                self.wrong_values = 1;
                if(self.customType && self.predefinedType != "None") {
                    self.duplicateTypeSelection = -1;
                    self.wrong_values = -1;
                }
                if(!self.customType && (self.predefinedType == "None" || !self.predefinedType)) {
                    self.noSelection = -1;
                    self.wrong_values = -1;
                }
                if (self.wrong_values === -1) {
                    return;
                }
                if(self.customType){
                    $uibModalInstance.close(self.customType);
                } else {
                    $uibModalInstance.close(self.predefinedType);
                }
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);