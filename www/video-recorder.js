var VideoRecorder = function () {

};

VideoRecorder.prototype = {

    startRecording: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback, // success callback function
            errorCallback, // error callback function
            'VideoRecorderPlugin', // mapped to our native Java class called "CalendarPlugin"
            'startRecording', // with this action name
            [{                  // and this array of custom arguments to create our entry
               // "title": title,
            }]
        );  
    }

};

var plugin = new VideoRecorder();

module.exports = plugin;