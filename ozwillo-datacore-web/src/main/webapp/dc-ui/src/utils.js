export function ajaxCall(relativeUrl, currentSuccess, currentError, additionalHeaders, operation, data){
  var currentOperation = operation !== null ? operation: 'GET';

  var headers = {
    'Authorization' : "Basic YWRtaW46YWRtaW4=",
    'If-None-Match': -1,
    'Accept' : 'application/json',
    'X-Datacore-Project': getProject() //TODO: put in the store the current Project
  };
  headers = $.extend(headers, additionalHeaders);

  $.ajax({
    url: relativeUrl,
    type: currentOperation,
    headers: headers,
    data: data,
    success: currentSuccess,
    error: currentError
  });
}

export function synchronousCall(relativeUrl, currentSuccess, currentError, additionalHeaders, operation, data){
  var currentOperation = operation !== null ? operation: 'GET';

  var headers = {
    'Authorization' : "Basic YWRtaW46YWRtaW4=",
    'If-None-Match': -1,
    'Accept' : 'application/json',
    'X-Datacore-Project': getProject() //TODO: put in the store the current Project
  };
  headers = $.extend(headers, additionalHeaders);

  $.ajax({
    url: relativeUrl,
    async: false,
    type: currentOperation,
    headers: headers,
    data: data,
    success: currentSuccess,
    error: currentError
  });
}

/*
Extract the modelName from an URL well formed
*/
export function getModel(url){
  var beginSearch = url.indexOf("type/")+5;

  beginSearch = (beginSearch === 4) ? url.indexOf("h/")+2 : beginSearch;

  var modelName = "";
  for(var i = beginSearch; i <= url.length; i++){
    modelName = url.substring(beginSearch,i);
    if(url[i] === "/" || url[i] === "?"){
      break;
    }
  }
  return modelName;
}
