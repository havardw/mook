package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;

@Path("/api/entry")
public class EntryController {

    private static final Logger log = LoggerFactory.getLogger(EntryController.class);

	private final EntryService entryService;

    @Inject
    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Collection<Entry> getEntries(@QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset) {
    	log.info("Request for {} entries from offset {}", limit, offset);

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
    
        Collection<Entry> entries = entryService.getEntries(verifiedOffset, verifiedLimit);
    
    	log.info("Returned {} entries", entries.size());

    	return entries;
    }

    
     
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Entry postEntry(Entry entry, @Context SecurityContext securityContext) {
    	log.info("POST for new entry");
    	
    	int id = entryService.saveEntry(entry.text(), entry.date(), entry.images(),
                               ((MookPrincipal)securityContext.getUserPrincipal()).getId());

    	log.info("Inserted new entry from {}", securityContext.getUserPrincipal().getName());

    	return new Entry(id, ((MookPrincipal)securityContext.getUserPrincipal()).getDisplayName(), entry.text(), entry.date(), entry.images());
   }
}