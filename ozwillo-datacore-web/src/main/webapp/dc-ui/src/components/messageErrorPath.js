import React from 'react';
import { connect } from 'react-redux';


export default class MessageErrorPath extends React.Component{
  constructor(props) {
    super(props);
    this.state = {
      seeDetails: false
    };
  }

  closeMessage = (e) => {
    this.setState({seeDetails: false});
    $('.errorPath').transition('fade');
  }

  componentDidMount = () => {
    var component = this;
    $('.message .close')
    .on('click', function() {
      $(this)
        .closest('.errorPath')
        .transition('fade')
      ;
      component.props.setParentState({errorMessage: false});
    });
  }

  render() {
    if(this.props.messageErrorDetails != undefined){
      var moreDetails = <a onClick={() => {this.setState({seeDetails: true})}}>More details</a>;
    }
    if(this.state.seeDetails === false){
      var message = <span>
          {this.props.message}
          <br/>
          {moreDetails}
        </span>;
    }
    else{
      var message = this.props.messageErrorDetails;
    }

    return(
      <div className="ui icon error message errorPath">
      <i className="close icon"></i>
      <i className="warning circle icon" onClick={this.closeMessage}></i>
      <p>
        {message}
      </p>
      </div>
    );
  }
}
