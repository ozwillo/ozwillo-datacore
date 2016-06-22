import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall, getModel} from '../../utils.js';

class ModelButton extends React.Component{
  modelButton = () => {
    var url = this.props.currentPath;

    var modelName = getModel(url);
    var relativeUrl = buildRelativeUrl("dcmo:model_0/"+modelName);

    ajaxCall(
      relativeUrl,
      (data) => {
        this.props.setUrl(relativeUrl, null);
        var resResourcesOrText = displayJsonObjectResult(data);
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      (xhr) => {
        this.props.setErrorMessage("Error accessing the current path", xhr.responseText);
      }
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.modelButton} id="MButton" data-content="Go to model">M</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default ModelButton = connect(mapStateToProps)(ModelButton);
