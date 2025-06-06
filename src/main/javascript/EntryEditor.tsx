import * as React from "react";
import axios, {AxiosError} from "axios";

import ImageEditor from "./ImageEditor";
import ImageUpload from "./ImageUpload";
import {AuthenticationData, Entry, Image} from "./domain";

const ENTRY_AUTOSAVE_PREFIX = "mook.entry.autosave.";

function createNewEntry() {
    return {
        date: isoDate(new Date()),
        text: "",
        images: [] as Image[]
    };
}

// Date object's isoDate doesn't use time zone, and Firefox Mobile doesn't support Intl API.
function isoDate(date: Date | string) {
    if (date instanceof Date) {
        let month = date.getMonth() < 9 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
        let day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();

        return date.getFullYear() + "-" + month + "-" + day;
    } else {
        return date;
    }
}

interface EntryEditorProps {
    userData: AuthenticationData;
    site: string;
    onHttpError(error: AxiosError): void;
    onEntryAdded(entry: Entry): void;
}


interface EntryEditorState {
    entry: Entry;
    uploads: File[],
    sending: boolean;
}

class EntryEditor extends React.Component<EntryEditorProps, EntryEditorState> {

    private autoSaveTimer?: number;

    constructor(props: EntryEditorProps) {
        super(props);

        // Check auto save for site
        const autoSave = window.localStorage.getItem(ENTRY_AUTOSAVE_PREFIX + this.props.site);

        let entry;
        if (autoSave !== null) {
            entry = JSON.parse(autoSave);
        } else {
            entry = createNewEntry();
        }

        this.state = {
            entry: entry,
            uploads: [],
            sending: false
        };
    }

    save = () => {
        // Only save entry if we have content, else we mess up the date when opening the app later
        if (this.state.entry.text !== "" || this.state.entry.images.length > 0) {
            window.localStorage.setItem(ENTRY_AUTOSAVE_PREFIX + this.props.site, JSON.stringify(this.state.entry));
        } else {
            window.localStorage.removeItem(ENTRY_AUTOSAVE_PREFIX + this.props.site);
        }
    };

    handleDateChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        let entry = this.state.entry;
        entry.date = event.target.value;
        this.setState({entry: entry});
    };

    handleTextChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
        let entry = this.state.entry;
        entry.text = event.target.value;
        this.setState({entry: entry});
    };

    handleCaptionChange = (text: string, index: number) => {
        // TODO Immutable?
        let entry = this.state.entry;
        let image = entry.images[index];
        image.caption = text;
        this.setState({entry: entry});
    };

    handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        console.info("Form submit");
        event.preventDefault();
        console.info("Entry ", this.state.entry);

        axios.post("api/entry/" + this.props.site, this.state.entry, { headers: { auth: this.props.userData.token }})
            .then((response) => {
                this.props.onEntryAdded(response.data);

                window.localStorage.removeItem("mook.entry.autosave");
                this.setState({sending: false, entry: createNewEntry()});
            }, (error) => {
                this.setState({sending: false});
                this.props.onHttpError(error);
            });
    };

    addImages = (event: React.ChangeEvent<HTMLInputElement>) => {
        let uploads: File[] = [];
        let files = event.target.files;
        if (files === null) {
            return;
        }

        console.info("Adding " + files.length + " images");
        for (let i = 0; i < files.length; i++) {
            let file = files.item(i);
            if (file !== null) {
                uploads.push(file);
            }
        }

        this.setState({uploads: this.state.uploads.concat(uploads)});
    };

    handleImageUploaded = (image: Image, fileName: string) => {
        let uploads = this.state.uploads.filter(u => u.name !== fileName);

        let images = this.state.entry.images.concat(image);

        let entry = {
            text: this.state.entry.text,
            date: this.state.entry.date,
            images: images
        };

        this.setState({entry: entry, uploads: uploads});
    };

    handleRemoveImage = (index: number) => {
        let image = this.state.entry.images[index];

        let images = this.state.entry.images.slice();
        images.splice(index, 1);

        let entry = this.state.entry;
        entry.images = images;

        this.setState({entry: entry});

        axios.delete("api/image/" + this.props.site + "/original/" + image.name, { headers: { auth: this.props.userData.token }})
            .then(function() {
                console.info("Deleted image " + image.name);
            })
            .catch(function(error) {
                console.warn("Failed to delete image", error);
            });

    };

    handleUploadFailed = (file: File) => {
        let uploads = this.state.uploads.slice();
        let index = uploads.indexOf(file);
        uploads.splice(index, 1);
        this.setState({uploads: uploads});
    };

    isEmpty = () => {
        return this.state.entry.text === "" && this.state.entry.images.length === 0;
    };

    componentDidMount() {
        this.startAutoSave();
    }

    componentWillUnmount() {
        this.stopAutoSave();
    }

    render() {
        let images = this.state.entry.images.map((image, index) => <ImageEditor key={image.id} image={image} index={index}
                                                                                site={this.props.site}
                                                                                userData={this.props.userData}
                                                                                onCaptionChange={this.handleCaptionChange}
                                                                                onRemove={this.handleRemoveImage}/>);

        let imageUploads = this.state.uploads.map((upload) => <ImageUpload key={upload.name} file={upload}
                                                                           site={this.props.site}
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
                              rows={8} placeholder="Skriv en melding"
                              value={this.state.entry.text} onChange={this.handleTextChange} /></p>
                {images}

                {imageUploads}

                <input type="file" accept="image/*" multiple={true} style={{display: 'none'}}
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

    startAutoSave = () => {
        this.autoSaveTimer = window.setInterval(this.save, 1000);
    };

    stopAutoSave = () => {
        clearInterval(this.autoSaveTimer);
    };
}

function showImageSelector(event: React.MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    let fileInput = (event.target as Element)?.parentElement?.parentElement?.querySelector("input[type='file']");
    (fileInput as HTMLButtonElement).click();
}

export default EntryEditor;