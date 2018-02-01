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
        .controller('NoteCreateCtrl', ['$uibModalInstance', 'title', 'msg', 'val',
          function ($uibModalInstance, title, msg, val) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.note = {};
            self.note.name = null;
            self.note.defaultInterpreter =  null;
            self.interpreterSettings = [];

            var getInterpreters = function () {
              angular.forEach(val, function (value) {
                self.interpreterSettings.push(value.interpreter);
                if(value.interpreter.defaultInterpreter === true){
                   self.note.defaultInterpreter = value.interpreter;
                }
//                if(value.interpreter.name === "livy"){
//                   self.note.defaultInterpreter = value.interpreter;
//                }
              });
            };

            getInterpreters();

            self.handleNameEnter = function () {
              self.ok();
            };

            self.ok = function () {
              var note = {};
              note.name = self.note.name;
              note.defaultInterpreterId = '';
              if (self.note.defaultInterpreter !== null) {
                note.defaultInterpreterId = self.note.defaultInterpreter.id;
              }

              $uibModalInstance.close({val: note});
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };

            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };
          }]);


