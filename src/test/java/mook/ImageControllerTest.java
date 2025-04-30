package mook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;


import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
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


    @BeforeEach
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

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString("Location")).isEqualTo("/unittest/api/image/42.jpg");
        assertThat(((Image)response.getEntity()).id()).isEqualTo(42);
    }
}