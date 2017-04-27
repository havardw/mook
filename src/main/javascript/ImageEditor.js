import React, { Component } from "react";

import Image from "./Image";

class ImageEditor extends Image {

    constructor(props) {
        super(props);

        let caption = props.image.caption === null ? "" : props.image.caption;

        this.state = {
            caption: caption
        };

        this.handleCaptionChange = this.handleCaptionChange.bind(this);
        this.handleRemove = this.handleRemove.bind(this);
    }

    handleCaptionChange(event) {
        this.props.onCaptionChange(event.target.value, this.props.index);
    }

    handleRemove() {
        this.props.onRemove(this.props.index);
    }

    componentWillReceiveProps(nextProps) {
        console.info("ImageEditor.componentWillReceiveProps", nextProps);
        this.setState({caption: nextProps.image.caption || ""});
    }


    render() {
        console.info("State in render: ", this.state);
        return (
            <div className="image">
                <div className="wrapper">
                    <img src={this.state.url} />
                    <img className="close" src="img/close.svg" width="20" height="20" onClick={this.handleRemove} />
                </div>

                <textarea value={this.state.caption} onChange={this.handleCaptionChange}
                          rows="3" placeholder="Beskriv bildet hvis du vil" />
            </div>
        );
    }
}

ImageEditor.propTypes = {
    image: React.PropTypes.object.isRequired,
    index: React.PropTypes.number.isRequired,
    userData: React.PropTypes.object.isRequired,
    onCaptionChange: React.PropTypes.func.isRequired,
    onRemove: React.PropTypes.func.isRequired
};

export default ImageEditor;