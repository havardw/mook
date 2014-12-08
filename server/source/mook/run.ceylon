import ceylon.dbc { Sql, newConnectionFromDataSource }
import ceylon.file { parsePath, Path, Directory, Nil }
import ceylon.net.http.server { Endpoint, Request, Response, startsWith, newServer }
import ceylon.net.http { post }
import com.mysql.jdbc.jdbc2.optional { MysqlDataSource }

import java.io { FileReader }
import java.util { Properties }



"Run Mook server."
shared void run() {
	MysqlDataSource ds = MysqlDataSource();
	variable String contextPath = "";
	Directory imageDir;
	
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
		
		String? imageConfig = config.getProperty("image.dir");
		if (exists imageConfig) {
			Path imagePath = parsePath(imageConfig);
			if (is Nil imagePath) {
				imageDir = imagePath.createDirectory();
				log("Created image directory ``imagePath``");
			} else if (is Directory r = imagePath.resource) {
				imageDir = r;
			} else {
				log("Config property 'image'.dir' is not a directory, exiting");
				return;
			}
		} else {
			log("Config property 'image'.dir' is not a directory, exiting");
			return;
		}
		
	} else {
		process.writeErrorLine("No config file found, set with property 'mook.config'");
		return;
	}

	
	value sql = Sql(newConnectionFromDataSource(ds));
	
    //create a HTTP server
    value server = newServer {
        EntryController(contextPath, sql),
        ImageController(contextPath, imageDir),
        Endpoint {
            path = startsWith("``contextPath``/login");
            void service(Request request, Response response) {
                handleLogin(sql, request, response);
            }
            acceptMethod = { post };
        }
    };
 
    //start the server on port 8080
    log("Starting server");
    server.start();
}