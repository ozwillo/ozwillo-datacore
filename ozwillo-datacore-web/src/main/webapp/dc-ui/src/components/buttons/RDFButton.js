import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class RDFButton extends React.Component{
  RDFButton = () => {
    var relativeUrl = this.props.currentPath;
    ajaxCall(
      relativeUrl,
      (data) => {
        this.props.dispatch(actions.setCurrentDisplayRDF(data));
      },
      (xhr) => {
        this.props.setErrorMessage("Error accessing the current path", xhr.responseText);
      },
      {'Accept':'text/x-nquads'}
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.RDFButton} id="RDFButton" data-content="RDF N-QUADS representation">RDF</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default RDFButton = connect(mapStateToProps)(RDFButton);
