import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class DebugButton extends React.Component{
  debugButton = () => {
    var relativeUrl = this.props.currentPath;
    ajaxCall(
      relativeUrl,
      (data) => {
        var resResourcesOrText = displayJsonObjectResult(data.debug);
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      (xhr) => {
        this.props.setErrorMessage("Error accessing the current path", xhr.responseText);
      },
      {
        "X-Datacore-Debug": true
      }
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.debugButton} id="debugButton" data-content="Debug/explain query">?</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default DebugButton = connect(mapStateToProps)(DebugButton);
