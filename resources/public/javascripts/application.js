var searchTypes = ["classes", "jars"];
var searchType = searchTypes[0];

var options = {};

$(document).ready(
    function(){
	init();
});

function init(){
    //Setup listener on searchbox
    $('#searchbox').live(
    	"keypress", 
    	function(e) {		
    	    startTypingTimer($(e.target));
    	}); 	

    //Do initial search
    if($('#searchbox').val()){
    	doSearch($('#searchbox').val());
    } else {
    	doSearch('.*');
    }
}

var typingTimeout;
function startTypingTimer(input_field) {	
    if (typingTimeout != undefined) {
	clearTimeout(typingTimeout);
    }
    typingTimeout = setTimeout(
	function(){
            if(searchType == searchTypes[0]){
	        doSearch($('#searchbox').val());
            } else {
	        doJarSearch($('#searchbox').val());                
            }
	}, 500);
}

/**
 * Convenience method for making jqery ajax call
 */
function ajaxRequest (opt_options) {
    // console.log("Attempting "+(opt_options['type']||'get')+
    // 	  " request to "+opt_options['url']);
    $.ajax(
	$.extend (
	    {
                //username: this.settings.crowdAppUsername,
                //password: this.settings.crowdAppPassword,
		//crossDomain: true,
		headers: {
		    // 'Authorization': 
		    // basicAuthHeader(settings.crowdAppUsername, 
		    //     	    settings.crowdAppPassword),
		    'Accept': 'application/json'
		},
		type: 'get',
		error: function(jqXHR, textStatus, errorThrown){
		    // console.log("Error occurred making request");
		    //console.log("Error in request: "+textStatus);
		    //console.log("Error thrown: "+errorThrown);
		},
		complete: function(jqXHR, textStatus){
		    // console.log("Request completed with status: "+jqXHR.status);
		    // resultJqXHR=jqXHR;
		}, 
		cache: false,
		ifModified: true
	    }, opt_options));
}

/**
 * Do a search for java classes. 
 */
function doSearch(search, onSuccess) {
    searchType = searchTypes[0];
    $('#searchbox').val(search);
    ajaxRequest(
	{
	    url: '/rest/search?search='+search,
	    success: onSuccess ? function(response) {onSuccesseval(response);} : 
		function(response){
                    showSearchResults(response);
		}
	});
}

/**
 * Do a search for jars 
 */
function doJarSearch(search, onSuccess) {
    searchType = searchTypes[1];
    $('#searchbox').val(search);
    ajaxRequest(
	{
	    url: '/rest/jars?search='+search,
	    success: onSuccess ? function(response) {onSuccesseval(response);} : 
		function(response){
                    showJarSearchResults(response);
		}
	});
}

/**
 * Get all classes in a jar
 */
function getClassesInJar(path_to_jar, onSuccess) {
    searchType = searchTypes[0];
    $('#searchbox').val('');
    ajaxRequest(
	{
	    url: '/rest/jars?jar='+path_to_jar,
	    success: onSuccess ? function(response) {onSuccesseval(response);} : 
		function(response){
                    showSearchResults(response);
		}
	});
}

function showSearchResults(json){
    var html = "<ul>";
    for(i in json){
        html += '<li><a href=\"/methods?classname='+json[i]+'\" '
            + 'alt="'+json[i]+'">'
            +truncString(json[i], 30)
            +'</a></li>';
    }
    html += "</ul>";

    $('#search-results').html(html);
}

function showJarSearchResults(json){
    var html = "<ul>";
    for(i in json){
        html += '<li><a href="javascript:getClassesInJar(\''+json[i]+'\')" '
            + 'alt="'+json[i]+'">'
            + truncString(getFileName(json[i]), 30)
            +'</a></li>';
    }
    html += "</ul>";

    $('#search-results').html(html);
}

function truncString(str, max_length){
    var max = max_length || 10;
    var result = str.length > max ? str.substring(0, max-3) + "..." : str;
    return result;
}

/**
 * TODO: find os dependent path separator somehow
 * Given file path return file name
 */
function getFileName(filepath){
    return filepath.slice((1 + filepath.lastIndexOf("/")),filepath.length);
}
