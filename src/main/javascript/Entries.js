import React, { Component } from "react";
import PropTypes from "prop-types";
import axios from "axios";
import Image from "./Image";
import EntryEditor from "./EntryEditor";

/** Page size for entry data requests. */
const PAGE_SIZE = 10;

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

        this.state = {entries: [], loading: false, offset: 0, complete: false};

        this.load = this.load.bind(this);
        this.handleEntryAdded = this.handleEntryAdded.bind(this);
        this.handleScroll = this.handleScroll.bind(this);
    }

    componentDidMount() {
        window.addEventListener('scroll', this.handleScroll);
        this.load();
    }

    componentWillUnmount() {
        window.removeEventListener('scroll', this.handleScroll);
    };

    load() {
        console.info("Loading entries from offset " + this.state.offset);
        this.setState({loading: true});
        axios.get("api/entry?limit=" + PAGE_SIZE + "&offset=" + this.state.offset, { headers: { auth: this.props.userData.token }})
            .then(response => {
                if (response.data.length === 0) {
                    this.setState({loading: false, complete: true});
                    return;
                }

                // Reverse sort from server to get newest first
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

                // Flag duplicates, e.g. if an entry was written after page was loaded
                // Duplicates should be at the start of the sorted array
                let index = 0;
                while (index < response.data[index].length &&
                       this.state.entries.some(entry => entry.id === response.data[index].id)) {
                    console.log("Removing duplicate ID " + response.data[index].id);
                    index++;
                }

                // Merge old and new entries, skip duplicates
                let mergedEntries = this.state.entries.concat(response.data.slice(index));

                // Assume that data is complete if we get fewer entries than we asked for
                let complete = response.data.length < PAGE_SIZE;
                this.setState({entries: mergedEntries,
                               loading: false,
                               offset: this.state.offset + PAGE_SIZE,
                               complete: complete});
            }, this.props.onHttpError);
    }

    handleEntryAdded(entry) {
        let entries = this.state.entries;
        entries = [ entry ].concat(entries);
        this.setState({entries: entries});
    }

    handleScroll() {
        if (!this.state.complete && !this.state.loading) {
            // Find length of remaining content. "scrollheight" is total document length, "scrollTop"
            // is current position, and "innerHeight" is viewport size.
            let remaining = document.documentElement.scrollHeight - document.documentElement.scrollTop - window.innerHeight;
            if (remaining < 1000) {
                console.log("Triggering new load on scroll");
                this.load();
            }
        }
    }

    render() {
        const entries = this.state.entries.map((entry) =>
           <Entry key={entry.id} entry={entry} userData={this.props.userData} />
        );

        return (
            <div onScroll={this.handleScroll}>
                <h2>Skriv en melding</h2>

                <EntryEditor userData={this.props.userData} onHttpError={this.props.onHttpError} onEntryAdded={this.handleEntryAdded} />

                <div id="entryContainer">{entries}</div>

                {this.state.loading && <div className="loading" style={{marginTop: 2 + 'em'}}>Henter logg</div>}
            </div>
        );
    }
}

Entries.propTypes = {
    userData: PropTypes.object.isRequired,
    onHttpError: PropTypes.func.isRequired
};


export default Entries;
