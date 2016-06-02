/*
 * This file is part of Hakbot Origin Controller.
 *
 * Hakbot Origin Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Hakbot Origin Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Hakbot Origin Controller. If not, see http://www.gnu.org/licenses/.
 */

/**
 * Constants
 */
var CONTENT_TYPE_JSON = 'application/json';
var CONTENT_TYPE_TEXT = 'text/plain';
var DATA_TYPE = "json";
var METHOD_GET = "GET";
var METHOD_POST = "POST";
var URL_ABOUT = "/version";
var URL_PROVIDERS = "/providers";
var URL_PUBLISHERS = "/publishers";
var STATE_CREATED = "CREATED";
var STATE_IN_QUEUE = "IN_QUEUE";
var STATE_IN_PROGRESS = "IN_PROGRESS";
var STATE_COMPLETED = "COMPLETED";
var STATE_PUBLISHED = "PUBLISHED";
var STATE_CANCELED = "CANCELED";
var STATE_UNAVAILABLE = "UNAVAILABLE";
var PLUGIN_PROVIDER = "provider";
var PLUGIN_PUBLISHER = "publisher";

/**
 * Variables
 */
var initialized = false;
var about;
var providers;
var publishers;
var selectedJob;


function contextPath() {
    //return $.cookie("CONTEXTPATH");
    return "/api";
}

/**
 * Retrieve a listing of all providers & publishers and stores JSON response in variables
 */
$(document).ready(function () {
    var successAbout=false, successProviders=false, successPublishers=false;
    $.ajax({
        url: contextPath() + URL_ABOUT,
        contentType: CONTENT_TYPE_JSON,
        dataType: DATA_TYPE,
        type: METHOD_GET,
        async: false,
        success: function (data) {
            about = data;
            successAbout = true;
        }
    });
    $.ajax({
        url: contextPath() + URL_PROVIDERS,
        contentType: CONTENT_TYPE_JSON,
        dataType: DATA_TYPE,
        type: METHOD_GET,
        async: false,
        success: function (data) {
            providers = data;
            successProviders = true;
        }
    });
    $.ajax({
        url: contextPath() + URL_PUBLISHERS,
        contentType: CONTENT_TYPE_JSON,
        dataType: DATA_TYPE,
        type: METHOD_GET,
        async: false,
        success: function (data) {
            publishers = data;
            successPublishers = true;
        }
    });

    if (successAbout && successProviders && successPublishers) {
        initialized = true;
    }
});

function resolvePluginByClass(pluginType, className) {
    if (PLUGIN_PROVIDER == pluginType) {
        for (var i=0; i<providers.length; i++) {
            if (providers[i].class == className) {
                return providers[i];
            }
        }
    }
    if (PLUGIN_PUBLISHER == pluginType) {
        for (i=0; i<publishers.length; i++) {
            if (publishers[i].class == className) {
                return publishers[i];
            }
        }
    }
    return null;
}

function getAppName() {
    return about.application;
}

function getAppVersion() {
    return about.version;
}

function formatJobTable(res) {
    for (var i=0; i<res.length; i++) {
        res[i].provider = resolvePluginByClass(PLUGIN_PROVIDER, res[i].provider).name;
        if (resolvePluginByClass(PLUGIN_PUBLISHER, res[i].publisher)) {
            res[i].publisher = resolvePluginByClass(PLUGIN_PUBLISHER, res[i].publisher).name;    
        }
        res[i].duration = getDuration(res[i].created, res[i].completed);
        res[i].created = timeConverter(res[i].created);
        res[i].started = timeConverter(res[i].started);
        res[i].completed = timeConverter(res[i].completed);
        res[i].successIcon = getSuccessIcon(res[i].success, res[i].state);
        res[i].successLabel = getSuccessLabel(res[i].success, res[i].state);
    }
    return res;
}

function timeConverter(timestamp) {
    if (timestamp == null || timestamp == "" || timestamp == 0) {
        return;
    }
    var a = new Date(timestamp);
    var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    var year = a.getFullYear();
    var month = months[a.getMonth()];
    var date = a.getDate();
    var hour = a.getHours();
    var min = a.getMinutes();
    var sec = a.getSeconds();
    var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
    return time;
}

function getDuration(startTimestamp, endTimestamp) {
    if (startTimestamp == null || startTimestamp == "" || startTimestamp == 0 || endTimestamp == null || endTimestamp == "" || endTimestamp == 0) {
        return;
    }
    var durationInMinutes = Math.ceil(((endTimestamp - startTimestamp) /1000) /60);
    if (durationInMinutes < 60)  {
        return durationInMinutes + " min";
    } else {
        return Math.floor(durationInMinutes / 60) + " hrs";
    }
}

function getSuccessIcon(success, state) {
    if (success) {
        return '<span class="glyphicon glyphicon glyphicon-ok-circle" style="color:seagreen" aria-hidden="true"></span>';
    } else if (state == STATE_CREATED || state == STATE_IN_QUEUE || state == STATE_IN_PROGRESS) {
        return '<span class="glyphicon glyphicon-hourglass" style="color:dimgrey" aria-hidden="true"></span>';
    } else if (state == STATE_COMPLETED || state == STATE_PUBLISHED){
        return '<span class="glyphicon glyphicon-warning-sign" style="color:darkred" aria-hidden="true"></span>';
    } else if (state == STATE_UNAVAILABLE ){
        return '<span class="glyphicon glyphicon-time" style="color:lightslategrey" aria-hidden="true"></span>';
    } else if (state == STATE_CANCELED ){
        return '<span class="glyphicon glyphicon-ban-circle" style="color:lightslategrey" aria-hidden="true"></span>';
    }
}

function getSuccessLabel(success, state) {
    if (success) {
        return '<span class="label label-success">' + getPrettyState(state) + '</span>';
    } else if (state == STATE_CREATED || state == STATE_IN_QUEUE || state == STATE_IN_PROGRESS) {
        return '<span class="label label-info">' + getPrettyState(state) + '</span>';
    } else if (state == STATE_COMPLETED || state == STATE_PUBLISHED){
        return '<span class="label label-danger">Failed</span>';
    } else if (state == STATE_UNAVAILABLE ){
        return '<span class="label label-warning">' + getPrettyState(state) + '</span>';
    } else if (state == STATE_CANCELED ){
        return '<span class="label label-default">' + getPrettyState(state) + '</span>';
    }
}

function getPrettyState(state) {
    if (state == STATE_CANCELED) {
        return "Canceled";
    } else if (state == STATE_COMPLETED) {
        return "Completed"
    } else if (state == STATE_CREATED) {
        return "Created";
    } else if (state == STATE_IN_PROGRESS) {
        return "In Progress";
    } else if (state == STATE_IN_QUEUE) {
        return "In Queue";
    } else if (state == STATE_PUBLISHED) {
        return "Published";
    } else if (state == STATE_UNAVAILABLE) {
        return "Unavailable";
    }
}

$('#jobsTable').on('click-row.bs.table', function (e, job, $element) {
    selectedJob = job;
    $('#main').removeClass("col-sm-12");
    $('#main').removeClass("col-md-12");
    $('#main').addClass("col-sm-9");
    //$('#main').addClass("col-md-10");
    $('#sidebar').css("display", "block");
    $('#jobsTable').bootstrapTable('resetView');
    $('#details-uuid').html(job.uuid);
    $('#details-name').html(job.name);
    $('#details-provider').html(job.provider);
    $('#details-publisher').html(job.publisher);
    $('#details-created').html(job.created);
    $('#details-started').html(job.started);
    $('#details-completed').html(job.completed);
    $('#details-duration').html(job.duration);
    $('#details-state').html(job.state);
    $('#details-success').html(job.success.toString());
    $('#details-successLabel').html(job.successLabel);
});

$('#jobsTable').on('click', 'tbody tr', function(event) {
    $(this).addClass('highlight').siblings().removeClass('highlight');
});

$('#modalTextDetail').on('show.bs.modal', function(e) {
    $('.modal-content').css('height',$( window ).height()*0.8);
    var title = $(e.relatedTarget).data('modal-title');
    $(e.currentTarget).find('h4[id="modalTitle"]').html(title);
    var height = $('.modal-content').height() - 170;

    var textarea = $('#details-result');
    textarea.height(height);
    textarea.val(null);

    var api = $(e.relatedTarget).data('api');

    // Show or hide the decode button
    if (api.lastIndexOf('/result', 0) === 0) {
        $('#decodeToggle').bootstrapToggle(); // initialize
        $('#decodeToggleLabel').css('display', 'inline-block');
        $('#decodeToggle').bootstrapToggle('off')
    } else {
        $('#decodeToggle').bootstrapToggle('destroy');
        $('#decodeToggle').css('display', 'none');
        $('#decodeToggleLabel').css('display', 'none');
    }

    var url = contextPath() + "/job/" + selectedJob.uuid + api;
    populateModalTextarea(url);
});

$(function() {
    $('#decodeToggle').change(function() {
        var url = contextPath() + "/job/" + selectedJob.uuid + "/result";
        if ($(this).prop('checked') == true) {
            url += ("?q=2");
        }
        populateModalTextarea(url);
    })
});

function populateModalTextarea(url) {
    $.ajax({
        url: url,
        contentType: CONTENT_TYPE_TEXT,
        type: METHOD_GET,
        success: function (data) {
            $('#details-result').val(data);
        }
    });
}

function downloadJobArtifact(api) {
    window.location = contextPath() + "/job/" + selectedJob.uuid + api;
}