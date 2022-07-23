package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * REST endpoint for images.
 */
@Path("/api/image")
@Slf4j
public class ImageController {

    private final ImageService imageService;

    @Inject
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @POST
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    public Response postImage(byte[] data, @Context SecurityContext securityContext, @Context UriInfo uriInfo) throws IOException {
        log.info("POST for new image, {} bytes", data.length);
        Image image = imageService.saveImage(data, ((MookPrincipal) securityContext.getUserPrincipal()).getId());
        log.info("Saved image as {}", image.name());

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
    public Response getOriginalImage(@PathParam("name") String name) {
        return serveImageResponse(imageService.readImage(name), name);
    }

    @DELETE
    @Path("original/{name}")
    public void deleteImage(@PathParam("name") String name, @Context SecurityContext securityContext) {
        imageService.deleteImage(name, ((MookPrincipal)securityContext.getUserPrincipal()).getId());
    }

    @GET
    @Path("resized/{size}/{name}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getResizedImage(@PathParam("size") int size, @PathParam("name") String name) {
        if (size < 0 || size > 1500) {
            log.warn("Can't resize to {} px", size);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return serveImageResponse(imageService.getResizedImage(size, name), name);
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
