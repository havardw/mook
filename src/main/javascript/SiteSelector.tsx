import {Permission} from "./domain";
import * as React from "react";

interface SiteSelectorProps {
    sites: Record<string, Permission>;
    onSelect: (site: string) => void;
}

export const SiteSelector: React.FC<SiteSelectorProps> = ({sites, onSelect}) => {
    const SelectorWrapper = {
        display: 'flex',
        flexDirection: 'column' as const,
        alignItems: 'center',
        padding: '2rem',
        gap: '1rem'
    };


    return (
        <div style={SelectorWrapper}>
            <h2>Velg side</h2>
            {Object.keys(sites).map(site => (
                <button
                    key={site}
                    onClick={() => onSelect(site)}
                >
                    {site}
                </button>
            ))}
        </div>
    );
};
