/*CurrentProject is optional: we have to use it only when the store takes to long to update the components (which means the getProject is not up-to-date yet)*/
export function ajaxCall(relativeUrl, currentSuccess, currentError, additionalHeaders, operation, data, currentProject){
  var currentOperation = operation !== null ? operation: 'GET';
  var currentProject = currentProject ? currentProject : getProject();
  var headers = {
    'Authorization' : "Basic YWRtaW46YWRtaW4=",
    'If-None-Match': -1,
    'Accept' : 'application/json',
    'X-Datacore-Project': currentProject
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

export function synchronousCall(relativeUrl, currentSuccess, currentError, additionalHeaders, operation, data, currentProject){
  var currentOperation = operation !== null ? operation: 'GET';

  console.log("avant"+currentProject);
  var currentProject = currentProject ? currentProject : getProject();
  console.log("apres"+currentProject);

  var headers = {
    'Authorization' : "Basic YWRtaW46YWRtaW4=",
    'If-None-Match': -1,
    'Accept' : 'application/json',
    'X-Datacore-Project': currentProject //TODO: put in the store the current Project
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

export function getModelFromModel(url){
  var beginSearch = url.indexOf("dcmo:model_0/")+13;

  var modelName = "";
  for(var i = beginSearch; i <= url.length; i++){
    modelName = url.substring(beginSearch,i);
    if(url[i] === "/" || url[i] === "?"){
      break;
    }
  }
  return modelName;
}
