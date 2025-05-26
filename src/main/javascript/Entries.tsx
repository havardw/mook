import * as React from "react";
import axios, {AxiosError} from "axios";
import Image from "./Image";
import EntryEditor from "./EntryEditor";
import {AuthenticationData, Entry as EntryData} from "./domain";
import DateTimeFormatOptions = Intl.DateTimeFormatOptions;

/** Page size for entry data requests. */
const PAGE_SIZE = 10;

interface EntryProps {
    entry: EntryData;
    site: string;
    userData: AuthenticationData;
}

function Entry(props: EntryProps) {

    const images = props.entry.images.map((image) => <Image key={image.id} site={props.site} image={image} userData={props.userData} />);

    return (
        <article className="entry">
            <h2>{friendlyDate(props.entry.date)}<span className="author">Skrevet av {props.entry.author}</span></h2>
            <div className="usertext">{props.entry.text}</div>
            {images}
        </article>
    );
}

function friendlyDate(date: string) {
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
            default: throw new Error("8 days a week"); // Shouldn't happen, but needs to be here so that every path has a return
        }
    } else {
        // More than a week ago
        let options: DateTimeFormatOptions;
        if (inDate.getFullYear() === now.getFullYear()) {
            options = {day: "numeric", month: "long"};
        } else {
            options = {day: "numeric", month: "long", year: "numeric"};
        }
        return inDate.toLocaleDateString("nb", options);
    }
}

interface EntriesProps {
    userData: AuthenticationData;
    site: string;
    onHttpError(error: AxiosError): void
}

interface EntriesState {
    offset: number;
    entries: EntryData[];
    loading: boolean;
    complete: boolean;
}

class Entries extends React.Component<EntriesProps, EntriesState> {

    constructor(props: EntriesProps) {
        super(props);

        this.state = {entries: [], loading: false, offset: 0, complete: false};
    }

    componentDidMount() {
        window.addEventListener('scroll', this.handleScroll);
        this.load();
    }

    componentDidUpdate(prevProps: EntriesProps) {
        // If the site has changed, reset state and load new data
        if (prevProps.site !== this.props.site) {
            this.setState({
                entries: [],
                loading: false,
                offset: 0,
                complete: false
            }, () => {
                // After state is reset, load new data
                this.load();
            });
        }
    }

    componentWillUnmount() {
        window.removeEventListener('scroll', this.handleScroll);
    };

    load = () => {
        console.info("Loading entries from offset " + this.state.offset);
        this.setState({loading: true});
        axios.get("api/entry/" + this.props.site + "?limit=" + PAGE_SIZE + "&offset=" + this.state.offset, { headers: { auth: this.props.userData.token }})
            .then(response => {
                if (response.data.length === 0) {
                    this.setState({loading: false, complete: true});
                    return;
                }

                // Assume that data is complete if we get fewer entries than we asked for
                let complete = response.data.length < PAGE_SIZE;

                // Remove duplicates, e.g. if an entry was written after page was loaded
                // Loop from the end to avoid problems with re-indexing array
                let i = response.data.length;
                while (i--) {
                    if (this.state.entries.some(entry => entry.id === response.data[i].id)) {
                        console.log("Removing duplicate ID " + response.data[i].id);
                        response.data.splice(i, 1);
                    }
                }

                // Reverse sort from server to get newest first
                response.data.sort((a: EntryData, b: EntryData) => {
                    if (a.date > b.date) {
                        return -1;
                    } else if (b.date > a.date) {
                        return 1;
                    } else {
                        // Secondary sort by ID
                        return (b.id !== undefined ? b.id: 0) - (a.id !== undefined ? a.id : 0);
                    }
                });

                // Merge old and new entries
                let mergedEntries = this.state.entries.concat(response.data);

                this.setState({entries: mergedEntries,
                               loading: false,
                               offset: this.state.offset + PAGE_SIZE,
                               complete: complete});
            }, this.props.onHttpError);
    };

    handleEntryAdded = (entry: EntryData) => {
        let entries = this.state.entries;
        entries = [ entry ].concat(entries);
        this.setState({entries: entries});
    };

    handleScroll = () => {
        if (!this.state.complete && !this.state.loading) {
            // Find length of remaining content. "scrollheight" is total document length, "pageYOffset"
            // is current position, and "innerHeight" is viewport size.
            let remaining = document.documentElement.scrollHeight - window.pageYOffset - window.innerHeight;
            if (remaining < 1000) {
                console.log("Triggering new load on scroll");
                this.load();
            }
        }
    };

    render() {
        const entries = this.state.entries.map((entry) =>
           <Entry key={entry.id} site={this.props.site} entry={entry} userData={this.props.userData} />
        );

        return (
            <div onScroll={this.handleScroll}>
                <h2>Skriv en melding</h2>

                <EntryEditor site={this.props.site} userData={this.props.userData} onHttpError={this.props.onHttpError} onEntryAdded={this.handleEntryAdded} />

                <div id="entryContainer">{entries}</div>

                {this.state.loading && <div className="loading" style={{marginTop: 2 + 'em'}}>Henter logg</div>}
            </div>
        );
    }
}

export default Entries;
