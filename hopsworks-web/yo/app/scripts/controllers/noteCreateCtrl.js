/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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


