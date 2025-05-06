import * as React from "react";
import {AuthenticationData, Image as ImageData} from "./domain";
import {ImageLoader} from "./ImageLoader";
import closeSvg from "./assets/close.svg";

interface ImageEditorProps {
    image: ImageData;
    site: string;
    index: number;
    userData: AuthenticationData;
    onCaptionChange(value: string, index: number): void;
    onRemove(index: number): void;
}

interface ImageEditorState {
    caption: string;
}

class ImageEditor extends React.Component<ImageEditorProps, ImageEditorState> {

    constructor(props: ImageEditorProps) {
        super(props);

        let caption = props.image.caption === null ? "" : props.image.caption;

        this.state = {
            caption: caption
        };
    }

    handleCaptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        this.props.onCaptionChange(event.target.value, this.props.index);
    };

    handleRemove = () => {
        this.props.onRemove(this.props.index);
    };

    componentWillReceiveProps(nextProps: ImageEditorProps) {
        this.setState({caption: nextProps.image.caption || ""});
    }


    render() {
        return (
            <div className="image">
                <div className="wrapper">
                    <ImageLoader site={this.props.site} userData={this.props.userData} image={this.props.image} />
                    <img className="close" src={closeSvg} alt="Slett bilde" width="20" height="20" onClick={this.handleRemove} />
                </div>

                <textarea value={this.state.caption} onChange={this.handleCaptionChange}
                          rows={3} placeholder="Beskriv bildet hvis du vil" />
            </div>
        );
    }
}

export default ImageEditor;
