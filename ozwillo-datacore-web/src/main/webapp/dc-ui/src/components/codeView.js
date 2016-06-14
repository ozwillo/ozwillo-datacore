import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';

class CodeView extends React.Component{
  createMarkup = () => {
    return {__html: this.props.currentJson}
  }
  updateCurrentJson = (e) => {
    this.props.dispatch(actions.setEditable(e.target.value));
  }

  render() {
    {/*We need to do this trick in order to escape the RDF HTML*/}
    if(this.props.codeView === "plainText"){
      var view = <pre className="segmentpadding mydata">{this.props.currentJson}</pre>;
    }
    else if(this.props.codeView === "editable"){
      var view = <textarea className="segmentpadding mydata textareamydata" onChange={this.updateCurrentJson} defaultValue={JSON.stringify(this.props.currentJson,null,2)}></textarea>;
    }
    else{
      var view = <pre className="segmentpadding mydata" dangerouslySetInnerHTML={this.createMarkup()}></pre>;
    }
    
    return(
      <div className="row ui segment mydatacontainer">
        {view}
      </div>
    );
  }
}

export default CodeView = connect()(CodeView);
