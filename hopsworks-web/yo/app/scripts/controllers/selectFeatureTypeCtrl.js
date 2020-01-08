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
    .controller('selectFeatureTypeCtrl', ['$uibModalInstance', 'online', 'settings',
        function ($uibModalInstance, online, settings) {
            /**
             * Initialize state
             */
            var self = this;

            //Constants
            self.hiveDataTypes = settings.suggestedHiveFeatureTypes
            self.mysqlDataTypes = settings.suggestedMysqlFeatureTypes

            self.online = online;

            //State
            self.duplicateTypeSelection = false;
            self.noSelection = false;
            self.predefinedHiveType = self.hiveDataTypes[0];
            self.predefinedMysqlType = self.mysqlDataTypes[0];
            self.customType;

            /**
             * Function called when the "Save" button is pressed.
             * Validates parameters and then returns the selected feature type to the parent-modal
             */
            self.selectFeatureType = function() { 
                // Reset
                self.duplicateTypeSelection = false;
                self.noSelection = false

                var predefinedType = self.online ? self.predefinedMysqlType : self.predefinedHiveType;

                if (self.customType && predefinedType != "None") {
                    self.duplicateTypeSelection = true;
                    return;
                } else if (self.customType) {
                    $uibModalInstance.close(self.customType);
                } else if (predefinedType != "None") {
                    $uibModalInstance.close(predefinedType);
                } else {
                    self.noSelection = true;
                    return;
                } 
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);