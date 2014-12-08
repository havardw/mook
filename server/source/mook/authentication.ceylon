import java.util { UUID }
import ceylon.net.http.server { Response, Request }
import ceylon.dbc { Sql, Row }
import ceylon.json { JsonObject=Object }

void handleLogin(Sql sql, Request request, Response response) {
	String? email = request.parameter("email");
	String? password = request.parameter("password");
	
	if (exists email, exists password) {
		if (email.empty || password.empty) {
			response.responseStatus = httpUnauthorized;
		} else {
			log("Login attempt for user ``email``");
			// Can't use singleValue on select because it fails when password does not match hash (i.e. no object returned)
			Row[] rows = sql.Select("select name from user where email=? and hash=SHA2(?, 512)").execute(email, password);
			if (exists row = rows[0], exists user = row.get("name")) {
				// Get a cache for the session to prevent multiple session objects being created
				value session = request.session;
				session.put("user", user);
				String uuid = UUID.randomUUID().string;
				session.put("uuid", uuid);
				
				value auth = JsonObject {
					"auth" -> uuid
				};
				
				if (is String user) {
					auth.put("name", user);
				}

				response.writeString(auth.string);
				log("Login successful for ``email``");
			} else {
				response.responseStatus = httpUnauthorized;
				log("Login failed for ``email``");
			}
		}
	} else {
		response.responseStatus = httpBadRequest;
	}	
}
