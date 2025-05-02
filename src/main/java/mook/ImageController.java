package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * REST endpoint for images.
 */
@Path("/api/image/{site}")
public class ImageController {
    
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;
    private final PermissionsService permissionsService;

    @Inject
    public ImageController(ImageService imageService, PermissionsService permissionsService) {
        this.imageService = imageService;
        this.permissionsService = permissionsService;
    }

    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    public Response postImage(byte[] data, @PathParam("site") String siteSlug, @Context SecurityContext securityContext, @Context UriInfo uriInfo) {
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());

        log.info("POST for new image, {} bytes for site {}", data.length, siteId);
        
        Image image = imageService.saveImage(data, principal.getId(), siteId);
        log.info("Saved image as {} for site {}", image.name(), siteSlug);

        // We want an URI without host, so start from the path
        UriBuilder uriBuilder = UriBuilder.fromUri(uriInfo.getRequestUri().getPath());
        uriBuilder.path(image.name());
        Response.ResponseBuilder response = Response.created(uriBuilder.build());
        response.entity(image);
        return response.build();
    }

    @GET
    @Path("original/{name}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getOriginalImage(@PathParam("site") String siteSlug, @PathParam("name") String name, @Context SecurityContext securityContext) {
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());
        
        return serveImageResponse(imageService.readImage(name, siteId), name);
    }

    @DELETE
    @Path("original/{name}")
    public void deleteImage(@PathParam("site") String siteSlug, @PathParam("name") String name, @Context SecurityContext securityContext) {
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());
        
        imageService.deleteImage(name, principal.getId(), siteId);
    }

    @GET
    @Path("resized/{size}/{name}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getResizedImage(@PathParam("site") String siteSlug, @PathParam("size") int size, @PathParam("name") String name, @Context SecurityContext securityContext) {
        if (size < 0 || size > 1500) {
            log.warn("Can't resize to {} px", size);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    
        MookPrincipal principal = (MookPrincipal) securityContext.getUserPrincipal();
        int siteId = permissionsService.checkUserHasAccess(siteSlug, principal.getId());
        
        return serveImageResponse(imageService.getResizedImage(size, name, siteId), name);
    }

    private Response serveImageResponse(byte[] data, String name) {
        if (data == null) {
            log.warn("Image {} not found", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            Response.ResponseBuilder response = Response.ok();
            response.type(imageService.getMimeTypeFromName(name));
            // Images don't change, can cache for a long time
            response.expires(Date.from(LocalDateTime.now().plusYears(2).toInstant(ZoneOffset.UTC)));
            response.entity(data);
            return response.build();
        }
    }
}
