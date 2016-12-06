package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.List;

@Path("entry")
@Slf4j
public class EntryController {

	private final EntryService entryService;

    @Inject
    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Collection<Entry> getEntries() {
    	log.info("Request for entries");
    
        Collection<Entry> entries = entryService.getEntries();
    
    	log.info("Returned {} entries", entries.size());

    	return entries;
    }

    
     
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public void postEntry(Entry entry, @Context SecurityContext securityContext) {
    	log.info("POST for new entry");
    	
    	entryService.saveEntry(entry.getText(), entry.getDate(), entry.getImages(),
                               ((MookPrincipal)securityContext.getUserPrincipal()).getId());

    	log.info("Inserted new entry from {}", securityContext.getUserPrincipal().getName());
   }
}