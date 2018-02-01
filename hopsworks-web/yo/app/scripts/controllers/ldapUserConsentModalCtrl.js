/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
  .controller('LdapUserConsentModalCtrl', ['$uibModalInstance', 'data', 'val',
    function ($uibModalInstance, data, val) {

      var self = this;
      self.data = data;
      self.user = {givenName: '', surname: '', chosenEmail: data.email[0], consent: ''};

      self.ok = function () {
        $uibModalInstance.close({val: self.user});
      };
      
      self.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      self.reject = function () {
        $uibModalInstance.dismiss('reject');
      };
    }]);


