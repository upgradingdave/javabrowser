
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
    	doSearch();
    }
}

var typingTimeout;
function startTypingTimer(input_field) {	
    if (typingTimeout != undefined) {
	clearTimeout(typingTimeout);
    }
    typingTimeout = setTimeout(
	function(){
	    doSearch($('#searchbox').val());
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
    ajaxRequest(
	{
	    url: '/rest/search?search='+search,
	    success: onSuccess ? function(response) {onSuccesseval(response);} : 
		function(response){
                    showSearchResults(response);
		}
	});
}

function showSearchResults(json){
    var html = "<ul>";
    for(i in json){
        html += '<li><a href=\"/methods?classname='+json[i]+'\">'+json[i]+'</a></li>';
    }
    html += "</ul>";

    $('#search-results').html(html);
}
