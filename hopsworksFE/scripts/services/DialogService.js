/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI')

.service('DialogService', ['$modal', 'dialogs', function ($modal, dialogs) {

    return{

        launch: function(which, content){
                switch(which){
                    case 'error': //content.header, content.body
                            dialogs.error(content.header, content.body);
                            break;
                    case 'wait':
                            var dlg = dialogs.wait(undefined,undefined,_progress);
                            _fakeWaitProgress();
                            break;
                    case 'customwait':
                            var dlg = dialogs.wait('Custom Wait Header','Custom Wait Message',_progress);
                            _fakeWaitProgress();
                            break;
                    case 'notify':
                            dialogs.notify(content.header, content.body);
                            break;
                    case 'confirm':
                            return dialogs.confirm(content.header, content.body);

                    case 'custom':
                            /*
                             * a dialog can be large: 'lg', medium: 'md', or small: 'sm'
                             */
                            return dialogs.create(content.view, content.controller, content.data,
                                                            {size:'md', keyboard: true, backdrop: false, windowClass: 'my-class'});

                    case 'custom2':
                            var dlg = dialogs.create('/dialogs/custom2.html','customDialogCtrl2',$scope.custom,{size:'lg'});
                            break;
                }
        }
    };
}]);