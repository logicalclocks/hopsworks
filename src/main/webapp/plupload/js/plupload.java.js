/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*global plupload:false, escape:false, alert:false */
/*jslint evil: true */

(function($, plupload){

  var uploadInstances = {};

  plupload.applet = {

    pluploadjavatrigger : function(eventname, id, fileobjstring) {
      // FF / Safari mac breaks down if it's not detached here
      // can't do java -> js -> java
      setTimeout(function() {
          var uploader = uploadInstances[id], i, args;
          var file = fileobjstring ? eval('(' + fileobjstring + ')') : "";
          if (uploader) {
            uploader.trigger('applet:' + eventname, file);
          }
      }, 0);
    }
  };

  plupload.runtimes.Applet = plupload.addRuntime("java", {

    /**
     * Returns supported features for the Java runtime.
     *
     * @return {Object} Name/value object with supported features.
     */

    getFeatures : function() {

      return {
        java: applet.hasVersion('1.5'),
        chunks: true,
        progress: true
      };
    },

    init : function(uploader, callback) {

      var pluploadApplet,
          appletContainer,
          appletVars,
          lookup = {},
          initialized,
          waitCount = 0,
          container = document.body,
          features = this.getFeatures(),
          url = uploader.settings.java_applet_url,
          log_level = uploader.settings.log_level || /*LOG_LEVEL_ERROR*/5;

      if(!features.java){
        callback({success : false});
        return;
      }

      function getApplet() {
        if(!pluploadApplet){
          pluploadApplet = document.getElementById(uploader.id);
        }
        return pluploadApplet;
      }

      function waitForAppletToLoadIn5SecsErrorOtherwise() {
        // Wait for applet init in 5 secs.
        if (waitCount++ > 5000) {
          callback({success : false});
          return;
        }
        if (!initialized) {
          setTimeout(waitForAppletToLoadIn5SecsErrorOtherwise, 1);
        }
      }

      uploadInstances[uploader.id] = uploader;
      appletContainer = document.createElement('div');
      appletContainer.id = uploader.id + '_applet_container';
      appletContainer.className = 'plupload applet';

      plupload.extend(appletContainer.style, {
        // move the 1x1 pixel out of the way.
        position : 'absolute',
        left: '-9999px',
        zIndex : -1
      });

      uploader.bind("Applet:Init", function() {
        var filters = uploader.settings.filters;
        var extensions = [];
        var description = "";

        initialized = true;

        if(filters.length > 0){
          // On the Java side we can only set one filter
          // pick the first description and add all extensions
          description = filters[0].title;
          for(var i = 0, len = filters.length; i < len; i++){
              var filter = filters[i];
              var filterExtensions = filter.extensions.split(',');
              for(var j = 0; j < filterExtensions.length; j++){
                  extensions.push(filterExtensions[j]);
              }
          }
          getApplet().setFileFilter(description, extensions);
        }
        callback({success : true});
      });

      document.body.appendChild(appletContainer);

      applet.inject(appletContainer, {
        archive: url,
        cache_archive: url,
        cache_version: "20062012",
        id: escape(uploader.id),
        code: 'plupload.Plupload',
        callback: 'plupload.applet.pluploadjavatrigger',
        log_level: log_level
      });

      uploader.bind("UploadFile", function(up, file) {
          var settings = up.settings,
              abs_url = location.protocol + '//' + location.host;

          if(settings.url.charAt(0) === "/"){
            abs_url += settings.url;
          }
          else if(settings.url.slice(0,4) === "http"){
            abs_url = settings.url;
          }
          else{
            // relative
            abs_url += location.pathname.slice(0, location.pathname.lastIndexOf('/')) + '/' + settings.url;
          }

          getApplet().uploadFile(lookup[file.id] + "", abs_url, document.cookie, settings.chunk_size || 0, settings.retries || 3);
      });

      uploader.bind("SelectFiles", function(up){
        getApplet().openFileDialog();
      });

      uploader.bind("Applet:UploadProcess", function(up, javaFile) {
        var file = up.getFile(lookup[javaFile.id]),
            finished = javaFile.chunk === javaFile.chunks;

        if (file.status != plupload.FAILED) {
          file.loaded = javaFile.loaded;
          file.size = javaFile.size;
          up.trigger('UploadProgress', file);
        }
        else{
          alert("uploadProcess status failed");
        }

        if (finished) {
          file.status = plupload.DONE;
          up.trigger('FileUploaded', file, {
            response : "File uploaded"
          });
        }
      });

      uploader.bind("Applet:SelectFiles", function(up, file) {
        var i, files = [], id;
        id = plupload.guid();
        lookup[id] = file.id;
        lookup[file.id] = id;

        files.push(new plupload.File(id, file.name, file.size));

        // Trigger FilesAdded event if we added any
        if (files.length) {
          uploader.trigger("FilesAdded", files);
        }
      });

      uploader.bind("Applet:GenericError", function(up, err) {
        uploader.trigger('Error', {
          code : plupload.GENERIC_ERROR,
          message : 'Generic error.',
          details : err.message,
          file : uploader.getFile(lookup[err.id])
        });
      });

      uploader.bind("Applet:IOError", function(up, err) {
        uploader.trigger('Error', {
          code : plupload.IO_ERROR,
          message : 'IO error.',
          details : err.message,
          file : uploader.getFile(lookup[err.id])
        });
      });

      uploader.bind("FilesRemoved", function(up, files) {
        for (var i = 0, len = files.length; i < len; i++) {
          getApplet().removeFile(lookup[files[i].id]);
        }
      });

      waitForAppletToLoadIn5SecsErrorOtherwise();

    }// end object arg
  });// end add runtime
})(window, plupload);
