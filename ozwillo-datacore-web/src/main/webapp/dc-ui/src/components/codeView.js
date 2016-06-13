import React from 'react';

export default class CodeView extends React.Component{
  createMarkup = () => {
    return {__html: this.props.currentJson}
  }
  render() {
    {/*We need to do this trick in order to escape the RDF HTML*/}
    if(this.props.codeView === "plainText"){
      var view = <pre className="segmentpadding mydata">{this.props.currentJson}</pre>;
    }
    else if(this.props.codeView === "editable"){
      var view = <textarea className="segmentpadding mydata" defaultValue={JSON.stringify(this.props.currentJson,null,2)}></textarea>;
    }
    else{
      var view = <pre className="segmentpadding mydata" dangerouslySetInnerHTML={this.createMarkup()}></pre>;
    }

    return(
      <div className="row ui segment mydatacontainer form">
        {view}
      </div>
    );
  }
}
