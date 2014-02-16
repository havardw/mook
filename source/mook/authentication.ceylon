import ceylon.net.http { Header }
import java.util { UUID }
import ceylon.net.http.server { Response, Request }
import ceylon.dbc { Sql }

void handleLogin(Sql sql, Request request, Response response) {
	String? email = request.parameter("email");
	String? password = request.parameter("password");
	
	if (exists email, exists password) {
		if (email.empty || password.empty) {
			response.responseStatus = httpUnauthorized;
		} else {
			log("Login attempt for user ``email``");
			String? user = sql.queryForString("select name from user where email=? and hash=SHA2(?, 512)", email, password);
			if (exists user) {
				// Get a cache for the session to prevent multiple session objects being created
				value session = request.session;
				session.put("user", user);
				String uuid = UUID.randomUUID().string;
				session.put("uuid", uuid);
				response.addHeader(Header("Set-Cookie", "XSRF-TOKEN=``uuid``"));
				response.addHeader(Header("Location", getUrl(request, "index.html")));
				response.responseStatus = httpFormRedirect;
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
