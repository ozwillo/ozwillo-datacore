
////////////////////////////////////////////////
// USERINFO LIB

// see http://stackoverflow.com/questions/13353352/use-this-javascript-to-load-cookie-into-variable
function readCookie(key) {
   var result = (result = new RegExp('(?:^|; )' + encodeURIComponent(key) + '=([^;]*)').exec(document.cookie)) ? (result[1]) : null;
   if (result && result.length  != 0 && result[0] == '"' && result[result.length - 1] == '"') {
      // it is quoted so unquote it first
      result = result.substring(1, result.length - 1);
   }
   return result;
}
function deleteCookie(key) {
   document.cookie = encodeURIComponent(key) + '=; Path=/; expires=' + new Date(-1).toGMTString();
}
function getAuthHeader() {
   var authCookie = readCookie("authorization");
   if (authCookie && authCookie.length != 0) {
      return authCookie;
   }
   return 'Basic YWRtaW46YWRtaW4=';
}


////////////////////////////////////////////////
// USERINFO UI

function initUserInfo() {
   var userinfo;
   var userinfoString = readCookie('userinfo');
   if (userinfoString && userinfoString !== null) {
      userinfo = JSON.parse(userinfoString);
      if (!userinfo || userinfo === null) {
         console.log('Unable to parse userinfo ' + userinfoString);
      }
      $('#logout').show();
   } else {
      if (dcConf.localauthdevmode) {
         // log test admin :
         userinfo = {
               nickname : 'Administrator',
               sub_groups : ['admin', 'u_john', 'tutor_jim',
                             'rm_altTourism.place.SofiaMonastery_readers'],
               test : true,
               anonymous : true
         };
      } else {
         // redirect to login :
         window.location.href = "/dc/playground/login"; // .href simulates a link http://stackoverflow.com/questions/1655065/redirecting-to-a-relative-url-in-javascript
      }
      $('#logout').hide();
   }
   var welcomeHtml = 'Welcome <span style="text-decoration:underline;" title="'
      + ((userinfo.sub) ? 'sub: ' + userinfo.sub : 'none') + "\n"
      + ((userinfo.sub_groups) ? 'groups: ' + userinfo.sub_groups.join(', ') : 'no groups')
      + '">'
      + userinfo.nickname + '</span>'
      + ((userinfo.test) ? ' (test account)' : '')
      + ((userinfo.email) ? ' (' + userinfo.email + ')' : '')
      + ((userinfo.anonymous) ? ' - <a href="/dc/playground/login">login</a>' : '');
   $('#userinfo').html(welcomeHtml);
}

function initUserInfoUi() {
   initUserInfo();
   
   $('#logout').on('click', function(e) {
      e.preventDefault();
      
      var userinfo = JSON.parse(readCookie("userinfo"));
      if (!userinfo) {
         initUserInfo(); // relog
      }
      var id_client = userinfo['id_token'];
      deleteCookie('authorization');
      deleteCookie('userinfo');
      window.location.assign(dcConf.accountsBaseUrl + '/a/logout?id_token_hint='
            + id_client + '&post_logout_redirect_uri=' + dcConf.baseUrl);
      return ;//if no return don't work window.location.assign
   });

   $.ajax({
      url:"/dc/type/dcmp:Project_0?dcmpv:name=%2B&limit=100", // sorted on dcmpv:name (%2B = + ; would allow range pagination)
      headers: { Authorization:getAuthHeader() }, // , 'X-Datacore-View':'dcmpv:PointOfView_0' (any project should do)
      success: function(userProjects) {
         var optionsHtml = '';
         for (var i in userProjects) {
            var projectName = userProjects[i]['dcmpv:name'];
            optionsHtml += '<option onclick="javascript:'
               + 'setProject($(this).attr(\'value\'));return false;" value="'
               + projectName + '">' + projectName;
            var visibleProjectNames = userProjects[i]['dcmp:visibleProjectNames'];
            if (visibleProjectNames && visibleProjectNames.length !== 0) {
               var startsByItself = visibleProjectNames[0] === projectName;
               if (!startsByItself || visibleProjectNames.length !== 1) {
                  var vpnJoined = visibleProjectNames.join(', ');
                  if (startsByItself) {
                     vpnJoined = vpnJoined.substring(projectName.length + 2);
                  }
                  optionsHtml += ' (visible : ' + vpnJoined;
                  var forkedUris = userProjects[i]['dcmp:forkedUris'];
                  if (forkedUris && forkedUris.length !== 0) {
                     optionsHtml += ', forked URIs : ' + forkedUris.join(', ');
                  }
                  optionsHtml += ')';
               }
            }
            optionsHtml += '</option>\n';
         }
         $('#project').html(optionsHtml);
         $('#project').val(getProject());
      }
   });
}

////////////////////////////////////////////////
// PLAYGROUND UI (without Swagger UI)

function initPlayground() {
   // getting dc playground conf ex. localauthdevmode :
   initDcConf(function(result){
      window.dcConf = JSON.parse(result);
      console.log("conf", dcConf);

      completePlaygroundInit();

      initUserInfoUi();
   });
}


////////////////////////////////////////////////
//PLAYGROUND UI - CONF & COMPLETE

function initDcConf(success) {
   // getting dc playground conf ex. localauthdevmode :
   $.ajax({
      url:"/dc/playground/configuration",
      headers: { Authorization:getAuthHeader() },
      success: success,
      error: function(jqXHR, textStatus, errorThrown) { // such as when 401 Not Authorized
          if (jqXHR.status == 500) {
              var msg = "Error getting /dc/playground/configuration (maybe Spring Security configuration problem ?) : " + jqXHR.responseText; // such as when Spring Security configuration error ex. IllegalArgumentException: There is no PasswordEncoder mapped for the id &quot;null&quot;
              console.log(msg);
              var unusedBlockingAnswer = confirm(msg); // (confirm() is blocking while alert() isn't)
              // DON'T redirect to / in devmode else RootRedirectResource
          } else {
              window.location.assign('/'); // which will require auth and redirect to login page
          }
      }
   });
}
function initDcConfReplacements() {
   for (var key in dcConf) {
      $(".dcConf_" + key).html("" + dcConf[key]); // converting to string first
      $(".dcConf_" + key + "_link").html('<a href="' + dcConf[key] + '">' + dcConf[key] + '</a>');
   }
}

function completePlaygroundInit() {
   initDcConfReplacements(); // Ozwillo
}