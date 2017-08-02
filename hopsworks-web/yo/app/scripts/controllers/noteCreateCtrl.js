'use strict';

angular.module('hopsWorksApp')
        .controller('NoteCreateCtrl', ['$uibModalInstance', 'title', 'msg', 'val',
          function ($uibModalInstance, title, msg, val) {

            var self = this;
            self.title = title;
            self.msg = msg;
            self.note = {};
            self.note.name = null;
            self.note.defaultInterpreter = null;
            self.interpreterSettings = [];

            var getInterpreters = function () {
              angular.forEach(val, function (value) {
                self.interpreterSettings.push(value.interpreter);
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


