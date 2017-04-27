import React, { Component, PropTypes } from "react";
import axios from "axios";
import Image from "./Image";
import EntryEditor from "./EntryEditor";

function Entry(props) {

    const images = props.entry.images.map((image) => <Image key={image.id} image={image} userData={props.userData} />);

    return (
        <article className="entry">
            <h2>{friendlyDate(props.entry.date)}<span className="author">Skrevet av {props.entry.author}</span></h2>
            <div className="usertext">{props.entry.text}</div>
            {images}
        </article>
    );
}

function friendlyDate(date) {
    let inDate = new Date(date);
    inDate.setHours(0);
    inDate.setMinutes(0);
    inDate.setSeconds(0);
    inDate.setMilliseconds(0);

    let now = new Date();
    now.setHours(0);
    now.setMinutes(0);
    now.setSeconds(0);
    now.setMilliseconds(0);

    let elapsed = now.getTime() - inDate.getTime();
    let days = elapsed / (24 * 60 * 60 * 1000);

    if (days === 0) {
        return "I dag";
    } else if (days === 1) {
        return "I går";
    } else if (days === 2) {
        return "I forigårs";
    } else if (days < 7 && days > 2) {
        switch (inDate.getDay()) {
            case 0: return "Søndag";
            case 1: return "Mandag";
            case 2: return "Tirsdag";
            case 3: return "Onsdag";
            case 4: return "Torsdag";
            case 5: return "Fredag";
            case 6: return "Lørdag";
        }
    } else {
        // More than a week ago
        let options;
        if (inDate.getFullYear() === now.getFullYear()) {
            options = {day: "numeric", month: "long"};
        } else {
            options = {day: "numeric", month: "long", year: "numeric"};
        }
        return inDate.toLocaleDateString("nb", options);
    }
}


class Entries extends Component {

    constructor(props) {
        super(props);

        this.state = {entries: null};

        this.load = this.load.bind(this);
        this.handleEntryAdded = this.handleEntryAdded.bind(this);

    }

    componentDidMount() {
        this.load();
    }

    load() {
        console.info("Loading entries");
        axios.get("api/entry", { headers: { auth: this.props.userData.token }})
            .then(response => {
                // Reverse sort for date
                response.data.sort(function(a, b) {
                    if (a.date > b.date) {
                        return -1;
                    } else if (b.date > a.date) {
                        return 1;
                    } else {
                        // Secondary sort by ID
                        return b.id - a.id;
                    }
                });
                this.setState({entries: response.data});
            }, this.props.onHttpError);
    }

    handleEntryAdded(entry) {
        let entries = this.state.entries;
        entries = [ entry ].concat(entries);
        this.setState({entries: entries});
    }

    render() {
        if (this.state.entries === null) {
            return (<div className="loading" style={{marginTop: 2 + 'em'}}>Henter logg</div>);
        } else {
            const entries = this.state.entries.map((entry) =>
               <Entry key={entry.id} entry={entry} userData={this.props.userData} />
            );

            return (
                <div>
                    <h2>Skriv en melding</h2>

                    <EntryEditor userData={this.props.userData} onHttpError={this.props.onHttpError} onEntryAdded={this.handleEntryAdded} />

                    <div>{entries}</div>
                </div>
            );
        }
    }
}

Entries.propTypes = {
    userData: PropTypes.object.isRequired,
    onHttpError: PropTypes.func.isRequired
};


export default Entries;
