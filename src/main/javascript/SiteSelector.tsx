import {Site, SitePermission} from "./domain";
import * as React from "react";

interface SiteSelectorProps {
    sites: SitePermission[];
    onSiteChange: (site: Site) => void;
}

export const SiteSelector: React.FC<SiteSelectorProps> = ({sites, onSiteChange}) => {

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
            {sites.map(site => (
                <button
                    key={site.path}
                    onClick={() => onSiteChange(site)}
                >
                    {site.name}
                </button>
            ))}
        </div>
    );
};
