import * as React from "react";
import {AuthenticationData, Image as ImageData} from "./domain";
import {ImageLoader} from "./ImageLoader";

interface ImageProps {
    userData: AuthenticationData,
    image: ImageData
}

class Image extends React.Component<ImageProps, {}> {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="image">
                <div className="wrapper">
                    <ImageLoader userData={this.props.userData} image={this.props.image} />
                </div>

                <div className="caption">{this.props.image.caption}</div>
            </div>
        );
    }
}

export default Image;