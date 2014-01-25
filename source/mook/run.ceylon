import ceylon.dbc { Sql }
import ceylon.json {Array, Object }
import ceylon.net.http.server { AsynchronousEndpoint, Endpoint, Request, Response, startsWith, newServer }
import ceylon.net.http { post, get, Header }
import com.mysql.jdbc.jdbc2.optional { MysqlDataSource }
import ceylon.net.http.server.endpoints { serveStaticFile }

import java.text { SimpleDateFormat, DateFormat }
import java.util { Date, UUID }



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

"Run the module `test.http`."
shared void run() {
	
	
	// Set up database
	MysqlDataSource ds = MysqlDataSource();
	ds.url = "jdbc:mysql://wigtil.net:3306/mook_dev";
	ds.user = "mook";
	ds.setPassword("2QcV6e6z");
	
	value sql = Sql(ds);	
	
    //create a HTTP server
    value server = newServer {
        //an endpoint, on the path /hello
        Endpoint {
            path = startsWith("/entry");
            //handle requests to this path
            void service(Request request, Response response) {
                // Check for valid user
                if (exists user=request.session.get("user"), is String user) {
                    if (request.method.equals(get)) {
                        handleGetEntries(sql, response);
                    } else if (request.method.equals(post)) {
                        value xsrf = request.header("X-XSRF-TOKEN");
                        if (exists xsrf, exists uuid=request.session.get("uuid")) {
                            if (xsrf == uuid) {
                                handlePostEntry(user, sql, request, response);
                            } else {
                                response.responseStatus = httpUnauthorized;
                            }
                        } else {
                            response.responseStatus = httpUnauthorized;
                        }
                    }
                } else {
                    response.responseStatus = httpUnauthorized;
                } 
            }
            acceptMethod = { get, post };
        },
        Endpoint {
            path = startsWith("/postlogin");
            void service(Request request, Response response) {
                handleLogin(sql, request, response);
            }
            acceptMethod = { post };
        },
        AsynchronousEndpoint {
            path = startsWith("");
            serveStaticFile("/home/havardw/prosjekter/mook/resources");
        }
    };
 
    //start the server on port 8080
    log("Starting server");
    server.start();
}


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
				request.session.put("user", user);
				String uuid = UUID.randomUUID().string;
				request.session.put("uuid", uuid);
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

String getUrl(Request request, String page) {
	value protocol = request.scheme;
	String? host = request.header("Host");
	variable String url = protocol + "://";

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
	Integer? lastSlash = path.lastOccurrence("/");
	if (exists lastSlash) {
		url += path[0..lastSlash] + "/" + page;
	} else {
		url += "/" + page;
	}
	
	return url;
}

void handleGetEntries(Sql sql, Response response) {
	value rows = sql.rows("SELECT * FROM entry")({});
	
	value entries = Array {};
	DateFormat formatter = SimpleDateFormat("yyyy-MM-dd");
	for (row in rows) {
		if (is String a=row["author"], is String text=row["entrytext"], is Date d=row["entrydate"]) {
			value o = Object {
				"author" -> a,
				"date" -> formatter.format(d),
				"text" -> text
			};
			entries.add(o);
		}
	}
	log("Returned ``entries.size`` entries");

	response.addHeader(Header("Content-Type", "application/json; encoding=utf-8"));
	response.writeString(entries.string);
}

void handlePostEntry(String user, Sql sql, Request request, Response response) {

	String? date = request.parameter("date");
	String? text = request.parameter("text");
	
	if (exists date, exists text) {
		DateFormat parser = SimpleDateFormat("yyyy-MM-dd");
		Date parsedDate = parser.parse(date);
		try {
			sql.insert("insert into entry (entryDate, entryText, author) values(?, ?, ?)", parsedDate, text, user);
			log("Inserted new entry fom ``user``");
		} catch (Exception e) {
			log("Inserting entry failed", e);
			response.writeString(e.string);
			response.responseStatus = httpServerError;
		}
		response.writeString("OK");
	} else {
		response.writeString("Missing parameter");
		response.responseStatus = httpBadRequest;
	}	
}