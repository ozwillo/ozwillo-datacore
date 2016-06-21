import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class HistoricButton extends React.Component{
  render() {
    return (
      <button className="small ui button" id="HButton" data-content="Previous version if history is enabled">H</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentJson: state.currentJson,
  currentPath: state.currentPath,
  codeView: state.codeView
})
export default HistoricButton = connect(mapStateToProps)(HistoricButton);
