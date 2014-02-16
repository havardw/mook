import ceylon.file { Directory, File, Path, Nil }
import ceylon.net.http.server { AsynchronousEndpoint, startsWith, Response, Request, UploadedFile }
import ceylon.net.http { post, get }
import ceylon.net.http.server.endpoints { serveStaticFile }
import java.util { UUID }
import ceylon.json { JsonObject=Object }

class ImageController(String contextPath, Directory imageDir) 
		extends AsynchronousEndpoint(startsWith("``contextPath``/image"),
                                     (Request request, Response response, Anything() complete) => handleImage(imageDir, request, response, complete),
                                     { get, post }) {
}

void handleImage(Directory imageDir, Request request, Response response, Anything() complete) {
	if (request.method.equals(get)) {
		serveStaticFile(imageDir.string, (Request request) => request.relativePath)(request, response, complete);
	} else {
		UploadedFile? img = request.file("img");
		if (exists img) {
			log("Got upload, path ``img.file``, file name ``img.fileName``");
			if (is File imgFile = img.file.resource) {
				String uuid = UUID.randomUUID().string;
				String fileName = "``uuid``.jpg";
				Path filePath = imageDir.path.childPath(fileName);
				if (is Nil newResource = filePath.resource) {
					imgFile.copy(newResource);
					log("File saved as ``filePath``");
					
					JsonObject result = JsonObject();
					result.put("filename", fileName);
				} else {
					log("Target file for copy already exists: ``filePath``");
					response.responseStatus = httpServerError;
				}				
			} else {
				log("Upload ``img.file.resource`` is not a file");
				response.responseStatus = httpBadRequest;
			}
		} else {
			log("No image file in request");
			response.responseStatus = httpBadRequest;
		}
		
		complete();
	}	
}