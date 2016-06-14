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
