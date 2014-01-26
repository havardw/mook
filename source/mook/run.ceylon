import ceylon.dbc { Sql }
import ceylon.json {Array, JsonObject=Object }
import ceylon.net.http.server { AsynchronousEndpoint, Endpoint, Request, Response, startsWith, newServer }
import ceylon.net.http { post, get, Header }
import com.mysql.jdbc.jdbc2.optional { MysqlDataSource }
import ceylon.net.http.server.endpoints { serveStaticFile }

import java.io { FileReader }
import java.text { SimpleDateFormat, DateFormat }
import java.util { Date, UUID, Properties }



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
	MysqlDataSource ds = MysqlDataSource();
	variable String contextPath = "";
	
	String? configFile = process.propertyValue("mook.config");
	if (exists configFile) {
		Properties config = Properties();
		config.load(FileReader(configFile));
		String? dbUrl  = config.getProperty("db.url");
		String? dbUser = config.getProperty("db.user");
		String? dbPass = config.getProperty("db.pass");
		if (exists dbUrl, exists dbUser, exists dbPass) {
			ds.url = dbUrl;
			ds.user = dbUser;
			ds.setPassword(dbPass);
			log("Read database settings");
		} else {
			process.writeErrorLine("Can't configure database, exiting");
			return;
		}
		
		String? configContext = config.getProperty("context");
		if (exists configContext) {
			if (configContext.startsWith("/")) {
				contextPath = configContext;
			} else {
				contextPath = "/``configContext``";
			}
			
		}
		log("Context path is '``contextPath``'");
	} else {
		process.writeErrorLine("No config file found, set with property 'mook.config'");
		return;
	}

	
	
	
	value sql = Sql(ds);
	
	
	String contextAwareFileMapper(Request request) {
		String path = request.path;
		String file;
		if (path.startsWith(contextPath), contextPath.size > 0) {
			file = path.segment(contextPath.size, path.size);
		} else {
			file = path;
		}
		if (file.equals("/")) {
			return "/index.html";
		} else {
			return file;
		}
	}
	
	value serveStatic = serveStaticFile("resources", contextAwareFileMapper);	
	
    //create a HTTP server
    value server = newServer {
        //an endpoint, on the path /hello
        Endpoint {
            path = startsWith("``contextPath``/entry");
            //handle requests to this path
            void service(Request request, Response response) {
                handleEntry(sql, request, response); 
            }
            acceptMethod = { get, post };
        },
        Endpoint {
            path = startsWith("``contextPath``/postlogin");
            void service(Request request, Response response) {
                handleLogin(sql, request, response);
            }
            acceptMethod = { post };
        },
        AsynchronousEndpoint {
            path = startsWith("");
            void service(Request request, Response response, Anything() complete) {
                log("Serving static file for ``request.path``");
                serveStatic(request, response, complete);
            }
        }
    };
 
    //start the server on port 8080
    log("Starting server");
    server.start();
}

void handleEntry(Sql sql, Request request, Response response) {
	value session = request.session;
	value user = session.get("user");

	// Check for valid user
	if (is String user) {
		if (request.method.equals(get)) {
			handleGetEntries(sql, response);
		} else if (request.method.equals(post)) {
			value xsrf = request.header("X-XSRF-TOKEN");
			if (exists xsrf, exists uuid=request.session.get("uuid")) {
				if (xsrf == uuid) {
					handlePostEntry(user, sql, request, response);
				} else {
					log("Invalid XSRF token");
					response.responseStatus = httpUnauthorized;
				}
			} else {
				log("XSRF token not set");
				response.responseStatus = httpUnauthorized;
			}
		}
	} else {
		log("No user in session");
		response.responseStatus = httpUnauthorized;
	}
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

void handleGetEntries(Sql sql, Response response) {
	log("Request for entries");
	Sequential<Map<String,Object>> rows;
	try {
		rows = sql.rows("SELECT * FROM entry")({});
	} catch (Exception e) {
		log("Exception getting entries", e);
		response.writeString(e.string);
		response.responseStatus = httpServerError;
		return;
	}
		
	value entries = Array {};
	DateFormat formatter = SimpleDateFormat("yyyy-MM-dd");
	for (row in rows) {
		if (is String a=row["author"], is String text=row["entrytext"], is Date d=row["entrydate"]) {
			value o = JsonObject {
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
	log("POST for new entry");
	String? date = request.parameter("date");
	String? text = request.parameter("text");
	
	if (exists date, exists text) {
		DateFormat parser = SimpleDateFormat("yyyy-MM-dd");		
		try {
			Date parsedDate = parser.parse(date);
			sql.insert("insert into entry (entryDate, entryText, author) values(?, ?, ?)", parsedDate, text, user);
			log("Inserted new entry fom ``user``");
			response.writeString("OK");
		} catch (Exception e) {
			log("Inserting entry failed", e);
			response.writeString(e.string);
			response.responseStatus = httpServerError;
		}		
	} else {
		response.writeString("Missing parameter");
		response.responseStatus = httpBadRequest;
	}	
}