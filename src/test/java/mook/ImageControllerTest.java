package mook;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;


import java.net.URI;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ImageController.
 */
public class ImageControllerTest {

    private ImageController controller;

    @Mock
    private ImageService mockImageService;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private UriInfo mockUriInfo;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        controller = new ImageController(mockImageService);
    }

    @Test
    public void postImage() throws Exception {
        when(mockUriInfo.getRequestUri()).thenReturn(new URI("https://www.example.org/unittest/api/image"));
        when(mockSecurityContext.getUserPrincipal()).thenReturn(new MookPrincipal(1, "test@example.org", "Unni Test"));
        when(mockImageService.saveImage(any(), eq(1))).thenReturn(new Image(42, "42.jpg", null));

        Response response = controller.postImage(new byte[28], mockSecurityContext, mockUriInfo);

        assertEquals(201, response.getStatus());
        assertEquals("/unittest/api/image/42.jpg", response.getHeaderString("Location"));
        assertEquals(42, ((Image)response.getEntity()).getId());
    }
}