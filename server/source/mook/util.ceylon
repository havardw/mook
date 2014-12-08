import java.util { Date }
import java.text { SimpleDateFormat, DateFormat }
import ceylon.net.http.server { Request }
Integer httpFormRedirect = 303;
Integer httpBadRequest   = 400;
Integer httpUnauthorized = 401;
Integer httpServerError  = 500;


void log(String message, Exception? e = null) {
	DateFormat formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	String date = formatter.format(Date());
	print("``date`` ``message``");
	if (exists e) {
		e.printStackTrace();
	}
}

String getUrl(Request request, String page) {
	variable String protocol;
	if (exists header=request.header("X-Forwarded-Proto")) {
		protocol = header;
	} else {
		protocol = request.scheme;
	}
	variable String url = protocol + "://";
	
	String? host = request.header("Host");
	if (exists host) {
		url += host;
	} else {
		url += request.destinationAddress.address;
		value port = request.destinationAddress.port;
		if ((protocol == "http" && port != 80)
				|| (protocol == "https" && port != 443)) {
			url += ":" + port.string;
		}
	}
	
	value path = request.path;
	Integer? lastSlash = path.lastOccurrence('/');
	if (exists lastSlash) {
		url += path[0..lastSlash]  + page;
	} else {
		url += "/" + page;
	}
	
	return url;
}
