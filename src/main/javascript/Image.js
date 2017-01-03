import React, { Component } from "react";
import axios from "axios";

class Image extends Component {

    constructor(props) {
        super(props);

        this.state = {url: "img/ajax-loader.gif"};

        this.load = this.load.bind(this);


    }

    componentDidMount() {
        this.load();
    }

    load() {
        let config = {
            headers: {
                auth: this.props.userData.token
            },
            responseType: "blob"
        };


        let size;
        const maxDimension = Math.max(window.screen.width, window.screen.height);
        if (maxDimension >= 600) {
            size = 800;
        } else if (maxDimension >= 400) {
            size = 600;
        } else {
            size = 400;
        }

        axios.get("api/image/resized/" + size + "/" + this.props.image.name, config)
            .then(response => this.setState({url: window.URL.createObjectURL(response.data)}))

            .catch(error => {
                console.warn("Failed to load image data: " + error.message);
                this.setState({url: ""});
            });
    }

    render() {
        return (
            <div className="image">
                <div className="wrapper">
                    <img src={this.state.url} />
                </div>

                <div className="caption">{this.props.image.caption}</div>
            </div>
        );
    }
}

Image.propTypes = {
    userData: React.PropTypes.object.isRequired
};

export default Image;