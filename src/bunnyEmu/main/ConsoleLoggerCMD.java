package bunnyEmu.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import misc.Logger;

/* handle console commands here */
public class ConsoleLoggerCMD implements Runnable {
	public void run() {
		System.out.println("\nType commands below after >>> indicators.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				Logger.writeLog(">>> ", Logger.LOG_TYPE_VERBOSE);

				// not ready to read anything yet
				while (!br.ready()) {
					Thread.sleep(200);
				}

				String command = br.readLine();

				if (command.equals("commands") || command.equals("help")) {
					System.out.println("Available commands are: {'shutdown', 'help', 'commands'}.");
				} else if(command.equals("shutdown")) {
					System.out.println("\n!!!Console shutdown imminent!!!");
					System.exit(0);
				} else if (command.isEmpty()) {
					continue;
				} else {
					Logger.writeLog("Unrecognized command. Try typing 'help'.", Logger.LOG_TYPE_VERBOSE);
				}
			}
		}
		catch (Exception e) {};
	}
}