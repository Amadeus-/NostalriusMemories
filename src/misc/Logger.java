package misc;

public class Logger {
	public static int LOG_TYPE_VERBOSE = 0;
	public static int LOG_TYPE_WARNING = 1;
	public static int LOG_TYPE_ERROR = 3;
	
	public static boolean printToConsole = false;
	
	public static void writeLog(String s, int logType)
	{
		if (printToConsole)
			System.out.println(s);
	}
}
