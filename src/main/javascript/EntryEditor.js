import React, { Component } from "react";
import PropTypes from "prop-types";
import axios from "axios";

import ImageEditor from "./ImageEditor";
import ImageUpload from "./ImageUpload";

function createNewEntry() {
    return {
        date: new Date(),
        text: "",
        images: []
    };
}

// Date object's isoDate doesn't use time zone, and Firefox Mobile doesn't support Intl API.
function isoDate(date) {
    if (date instanceof Date) {
        let month = date.getMonth() < 9 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
        let day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();

        return date.getFullYear() + "-" + month + "-" + day;
    } else {
        return date;
    }
}

class EntryEditor extends Component {

    constructor(props) {
        super(props);

        let autoSave = window.localStorage.getItem("mook.entry.autosave");
        let entry;
        if (autoSave !== null) {
            entry = JSON.parse(autoSave);
            entry.date = new Date(entry.date);
        } else {
            entry = createNewEntry();
        }

        this.state = {
            entry: entry,
            uploads: [],
            sending: false
        };

        this.save = this.save.bind(this);
        this.handleDateChange = this.handleDateChange.bind(this);
        this.handleTextChange = this.handleTextChange.bind(this);
        this.handleCaptionChange = this.handleCaptionChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.addImages = this.addImages.bind(this);
        this.handleImageUploaded = this.handleImageUploaded.bind(this);
        this.handleUploadFailed = this.handleUploadFailed.bind(this);
        this.handleRemoveImage = this.handleRemoveImage.bind(this);
        this.isEmpty = this.isEmpty.bind(this);
    }

    save() {
        // Only save entry if we have content, else we mess up the date when opening the app later
        if (this.state.entry.text !== "" || this.state.entry.images.lenght > 0) {
            window.localStorage.setItem("mook.entry.autosave", JSON.stringify(this.state.entry));
        } else {
            window.localStorage.removeItem("mook.entry.autosave");
        }
    }

    handleDateChange(event) {
        let entry = this.state.entry;
        entry.date = event.target.value;
        this.setState({entry: entry});
    }

    handleTextChange(event) {
        let entry = this.state.entry;
        entry.text = event.target.value;
        this.setState({entry: entry});
    }

    handleCaptionChange(text, index) {
        // TODO Immutable?
        let entry = this.state.entry;
        let image = entry.images[index];
        image.caption = text;
        this.setState({entry: entry});
    }

    handleSubmit(event) {
        console.info("Form submit");
        event.preventDefault();
        console.info("Entry ", this.state.entry);

        axios.post("api/entry", this.state.entry, { headers: { auth: this.props.userData.token }})
            .then((response) => {
                this.props.onEntryAdded(response.data);

                window.localStorage.removeItem("mook.entry.autosave");
                this.setState({sending: false, entry: createNewEntry()});
            }, (error) => {
                this.setState({sending: false});
                this.props.onHttpError(error);
            });
    }

    addImages(event) {
        console.info("addImages", event);

        let uploads = [];
        let files = event.target.files;

        for (let i = 0; i < files.length; i++) {
            uploads.push({
                file: files.item(i)
            });
        }

        this.setState({uploads: this.state.uploads.concat(uploads)});
    }

    handleImageUploaded(image, index) {
        console.info("Image uploaded", image);

        let uploads = this.state.uploads.slice();
        uploads.splice(index, 1);

        let images = this.state.entry.images.concat(image);

        let entry = {
            text: this.state.entry.text,
            date: this.state.entry.date,
            images: images
        };

        this.setState({entry: entry, uploads: uploads});
    }

    handleRemoveImage(index) {
        let image = this.state.entry.images[index];

        let images = this.state.entry.images.slice();
        images.splice(index, 1);

        let entry = this.state.entry;
        entry.images = images;

        this.setState({entry: entry});

        axios.delete("api/image/original/" + image.name, { headers: { auth: this.props.userData.token }})
            .then(function() {
                console.info("Deleted image " + image.name);
            })
            .catch(function(error) {
                console.warn("Failed to delete image", error);
            });

    }

    handleUploadFailed(error, index) {
        let uploads = this.state.uploads.slice();
        uploads.splice(index, 1);
        this.setState({uploads: uploads});
    }

    isEmpty() {
        return this.state.entry.text === "" && this.state.entry.images.length === 0;
    }

    componentDidMount() {
        this.startAutoSave();
    }

    componentWillUnmount() {
        this.stopAutoSave();
    }

    render() {
        let images = this.state.entry.images.map((image, index) => <ImageEditor key={index} image={image} index={index}
                                                                                userData={this.props.userData}
                                                                                onCaptionChange={this.handleCaptionChange}
                                                                                onRemove={this.handleRemoveImage}/>);

        let uploads = this.state.uploads.map((upload, index) => <ImageUpload key={index} file={upload.file} index={index}
                                                                             userData={this.props.userData}
                                                                             onImageUpload={this.handleImageUploaded}
                                                                             onUploadFailed={this.handleUploadFailed}/>);


        return  (
            <form onSubmit={this.handleSubmit}>
                <p><label htmlFor="date">Dato</label>
                    <input type="date" disabled={this.state.sending}
                           value={isoDate(this.state.entry.date)} onChange={this.handleDateChange} /></p>
                <p><label htmlFor="text">Melding</label>
                    <textarea disabled={this.state.sending}
                              rows="8" placeholder="Skriv en melding"
                              value={this.state.entry.text} onChange={this.handleTextChange} /></p>
                {images}

                {uploads}

                <input type="file" accept="image/*" multiple="multiple" style={{display: 'none'}}
                       onChange={this.addImages}/>

                <div className="entry-buttons">
                    <input type="submit" value="Lagre"
                            disabled={this.isEmpty() || this.state.sending}
                            className={this.state.sending ? "sending" : ""} />
                    <button onClick={showImageSelector}>Legg til bilde</button>
                </div>
            </form>
        )
    }

    startAutoSave() {
        this.autoSaveTimer = setInterval(this.save, 1000);
    }

    stopAutoSave() {
        clearInterval(this.autoSaveTimer);
    }
}

EntryEditor.propTypes = {
    userData: PropTypes.object.isRequired,
    onHttpError: PropTypes.func.isRequired,
    onEntryAdded: PropTypes.func.isRequired
};

function showImageSelector(event) {
    event.preventDefault();
    let fileInput = event.target.parentElement.parentElement.querySelector("input[type='file']");
    fileInput.click();
}

export default EntryEditor;