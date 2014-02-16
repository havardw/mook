import ceylon.net.http.server { startsWith, Endpoint, Response, Request }
import ceylon.dbc { Sql }
import ceylon.net.http { post, get, Header }
import java.util { Date }
import ceylon.json { Array, JsonObject=Object }
import java.text { SimpleDateFormat, DateFormat }


class EntryController(String contextPath, Sql sql) 
		extends Endpoint(startsWith("``contextPath``/entry"), 
                         (Request request, Response response) => handleEntry(sql, request, response), 
                         {get, post})  {	
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
