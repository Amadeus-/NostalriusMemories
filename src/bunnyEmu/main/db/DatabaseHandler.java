package bunnyEmu.main.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import bunnyEmu.main.Server;
import misc.Logger;

public class DatabaseHandler {
	
	private static String authDB = "USE " + Server.prop.getProperty("authDB");
	//private static String charactersDB = "USE " + Server.prop.getProperty("charactersDB");
	//private static String worldDB = "USE " + Server.prop.getProperty("worldDB");
}