import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall, getModel, synchronousCall} from '../../utils.js';

class HistoricButton extends React.Component{
  getHistoricalUri = (url) => {
    var beginSearch = url.indexOf("type/")+5;

    beginSearch = (beginSearch === 4) ? url.indexOf("h/")+2 : beginSearch;

    var historicalUri = "";
    for(var i = beginSearch; i <= url.length; i++){
      historicalUri = url.substring(beginSearch,i);
      if(url[i] === "?"){
        break;
      }
    }
    return "/dc/h/"+historicalUri;
  }

  getPreviousVersion = () => {
    //We first make a get request to have the resource
    var currentJson = "";

    synchronousCall(
      this.props.currentPath,
      (data) => {
        currentJson  = data;
      },
      (xhr) => {
        this.props.setErrorMessage("The current url is incorrect", xhr.responseText);
      }
    );

    if(Object.prototype.toString.call(currentJson) !== '[object Array]'){
      var previousVersion = currentJson["o:version"]-1;
      if (previousVersion > "-1")
      {
        var historicUrl = this.getHistoricalUri(this.props.currentPath)+'/'+previousVersion;
        ajaxCall(
          historicUrl,
          (data) => {
            this.props.setUrl(historicUrl, null);
            var resResourcesOrText = displayJsonObjectResult(data);
            this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
          },
          (xhr) => {
            this.props.setErrorMessage("Error accessing historic to this object", xhr.responseText);
          }
        );
      }
      else{
        this.props.setErrorMessage("There is no previous version for this resource", null);
      }
    }
    else{
      this.props.setErrorMessage("Select a single resource in order to get the historic", null);
    }
  }

  render() {
    return (
      <button className="small ui button" id="HButton" data-content="Previous version if history is enabled" onClick={this.getPreviousVersion}>H</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default HistoricButton = connect(mapStateToProps)(HistoricButton);
