package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collection;

@Path("/api/entry/{site}")
public class EntryController {

    private static final Logger log = LoggerFactory.getLogger(EntryController.class);

	private final EntryService entryService;
	private final PermissionsService permissionsService;

    @Inject
    public EntryController(EntryService entryService, PermissionsService permissionsService) {
        this.entryService = entryService;
        this.permissionsService = permissionsService;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Collection<Entry> getEntries(@PathParam("site") String siteSlug, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset, @Context SecurityContext securityContext) {
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());

        log.info("Request for {} entries from offset {} for site {}", limit, offset, siteId);

        int verifiedLimit;
    	if (limit == null || limit < 0) {
    	    verifiedLimit = 20;
        } else {
    	    verifiedLimit = limit;
        }

        int verifiedOffset;
    	if (offset == null || offset < 0) {
    	    verifiedOffset = 0;
        } else {
    	    verifiedOffset = offset;
        }

        Collection<Entry> entries = entryService.getEntries(siteId, verifiedOffset, verifiedLimit);
    
    	log.info("Returned {} entries for site {}", entries.size(), siteSlug);

    	return entries;
    }

    
     
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Entry postEntry(@PathParam("site") String siteSlug, Entry entry, @Context SecurityContext securityContext) {
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());

        log.info("POST for new entry for site {}", siteId);
    
        int id = entryService.saveEntry(entry.text(), entry.date(), entry.images(),
                               principal.getId(), siteId);

    	log.info("Inserted new entry from {} for site {}", securityContext.getUserPrincipal().getName(), siteSlug);

    	return new Entry(id, principal.getDisplayName(), entry.text(), entry.date(), entry.images());
   }
}