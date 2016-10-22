package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST endpoint for images.
 */
@Path("image")
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
        String fileName = imageService.saveImage(data, ((MookPrincipal)securityContext.getUserPrincipal()).getId());
        log.info("Saved image as {}", fileName);

        // We want an URI without host, so start from the path
        UriBuilder uriBuilder = UriBuilder.fromUri(uriInfo.getRequestUri().getPath());
        uriBuilder.path(fileName);
        return Response.created(uriBuilder.build()).build();
    }

}
