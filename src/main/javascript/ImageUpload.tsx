import * as React from "react";
import axios, {AxiosError, AxiosRequestConfig} from "axios";
import {AuthenticationData, Image} from "./domain";

interface ImageUploadProps {
    userData: AuthenticationData;
    site: string;
    file: File;
    onUploadFailed(file: File): void;
    onImageUpload(image: Image, name: string): void;
}

interface ImageUploadState {
    url: string;
    percentage: number;
}

class ImageUpload extends React.Component<ImageUploadProps, ImageUploadState> {

    constructor(props: ImageUploadProps) {
        super(props);

        this.state = {
            url: "img/ajax-loader.gif",
            percentage: 0
        };
    }

    componentDidMount() {
        this.setState({url: window.URL.createObjectURL(this.props.file)});
        this.upload();
    }

    upload = () => {
        let config: AxiosRequestConfig = {
            headers: {
                auth: this.props.userData.token,
                "Content-Type": "application/octet-stream"
            },
            onUploadProgress: (event) => {
                let percent = event.total ? (event.loaded / event.total) * 100 : 0;
                this.setState({percentage: percent});
            }
        };



        axios.post("api/image/" + this.props.site, this.props.file, config)
            .then((response) => {
                console.log("Image uploaded: " + JSON.stringify(response.data));
                this.props.onImageUpload(response.data, this.props.file.name);
            },
            (error) => {
                console.log("Upload error", error);
                let retry = window.confirm("Kunne ikke legge til bilde.\nVil du prøve på nytt?");
                if (retry) {
                    this.upload();
                } else {
                    this.props.onUploadFailed(this.props.file);
                }
            });
    };

    render() {
        return (
            <div className="image">
                <div className="upload-text">Laster opp bilde</div>
                <div className="wrapper">
                    <img src={this.state.url} className="upload" />
                    <div className="upload-progress-frame">
                        <div className="upload-progress-bar" style={{width: this.state.percentage + '%'}}></div>
                    </div>
                </div>
            </div>
        )
    }
}

export default ImageUpload;