import * as React from "react";
import axios, {AxiosRequestConfig} from "axios";
import {AuthenticationData, Image as ImageData} from "./domain";

interface ImageLoaderProps {
    userData: AuthenticationData,
    image: ImageData
}


interface ImageLoaderState {
    url: string;
}

export class ImageLoader extends React.Component<ImageLoaderProps, ImageLoaderState> {


    constructor(props: ImageLoaderProps) {
        super(props);
        this.state = {url: "img/ajax-loader.gif"};
    }

    componentDidMount() {
        this.load();
    }

    load = () => {
        let config: AxiosRequestConfig = {
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
    };

    render(): React.ReactNode {
        let alt;
        if (this.props.image.caption) {
            alt = "Bilde: " + this.props.image.caption;
        } else {
            alt = "Bilde uten beskrivelse";
        }

        return <img src={this.state.url} alt={alt} />
    }
}