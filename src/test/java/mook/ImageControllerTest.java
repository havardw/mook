package mook;

import jakarta.ws.rs.core.MediaType;
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
import org.junit.jupiter.api.AfterEach;

/**
 * Unit tests for ImageController.
 */
public class ImageControllerTest {

    private ImageController controller;

    @Mock
    private ImageService mockImageService;

    @Mock
    private PermissionsService mockPermissionsService;
    
    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private UriInfo mockUriInfo;

    private AutoCloseable mockCloser;

    @BeforeEach
    public void setUp() throws Exception {
        mockCloser = MockitoAnnotations.openMocks(this);
        
        controller = new ImageController(mockImageService, mockPermissionsService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mockCloser != null) {
            mockCloser.close();
        }
    }

    @Test
    public void postImage() throws Exception {
        String siteSlug = "standard";
        int siteId = 1;
        when(mockUriInfo.getRequestUri()).thenReturn(new URI("https://www.example.org/unittest/api/image/" + siteSlug));
        when(mockSecurityContext.getUserPrincipal()).thenReturn(new MookPrincipal(1, "test@example.org", "Unni Test"));
        when(mockPermissionsService.checkUserHasAccess(eq(siteSlug), eq(1))).thenReturn(siteId);
        when(mockImageService.saveImage(any(), eq(1), eq(siteId))).thenReturn(new Image(42, "42.jpg", null));
    
        Response response = controller.postImage(new byte[28], siteSlug, mockSecurityContext, mockUriInfo);
    
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString("Location")).isEqualTo("/unittest/api/image/" + siteSlug + "/42.jpg");
        assertThat(((Image)response.getEntity()).id()).isEqualTo(42);
    }
    
    @Test
    public void getOriginalImage() throws Exception {
        String siteSlug = "standard";
        int siteId = 1;
        String imageName = "42.jpg";
        byte[] imageData = new byte[100];
        
        when(mockSecurityContext.getUserPrincipal()).thenReturn(new MookPrincipal(1, "test@example.org", "Unni Test"));
        when(mockPermissionsService.checkUserHasAccess(eq(siteSlug), eq(1))).thenReturn(siteId);
        when(mockImageService.readImage(eq(imageName), eq(siteId))).thenReturn(imageData);
        when(mockImageService.getMimeTypeFromName(eq(imageName))).thenReturn(MediaType.valueOf("image/jpeg"));
        
        Response response = controller.getOriginalImage(siteSlug, imageName, mockSecurityContext);
        
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(imageData);
    }
    
    @Test
    public void deleteImage() throws Exception {
        String siteSlug = "standard";
        int siteId = 1;
        String imageName = "42.jpg";
        
        when(mockSecurityContext.getUserPrincipal()).thenReturn(new MookPrincipal(1, "test@example.org", "Unni Test"));
        when(mockPermissionsService.checkUserHasAccess(eq(siteSlug), eq(1))).thenReturn(siteId);
        
        controller.deleteImage(siteSlug, imageName, mockSecurityContext);
    }
}