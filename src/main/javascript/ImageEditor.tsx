import * as React from "react";
import {AuthenticationData, Image as ImageData} from "./domain";
import {ImageLoader} from "./ImageLoader";

interface ImageEditorProps {
    image: ImageData,
    index: number;
    userData: AuthenticationData;
    onCaptionChange(value: string, index: number): void;
    onRemove(index: number): void;
}

interface ImageEditorState {
    caption: string;
}

class ImageEditor extends React.Component<ImageEditorProps, ImageEditorState> {

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
        this.setState({caption: nextProps.image.caption || ""});
    }


    render() {
        return (
            <div className="image">
                <div className="wrapper">
                    <ImageLoader userData={this.props.userData} image={this.props.image} />
                    <img className="close" src="img/close.svg" alt="Slett bilde" width="20" height="20" onClick={this.handleRemove} />
                </div>

                <textarea value={this.state.caption} onChange={this.handleCaptionChange}
                          rows={3} placeholder="Beskriv bildet hvis du vil" />
            </div>
        );
    }
}

export default ImageEditor;