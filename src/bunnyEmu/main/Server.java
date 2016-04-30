/*
 * BunnyEmu - A Java WoW sandbox/emulator
 * https://github.com/marijnz/BunnyEmu
 */
package bunnyEmu.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.UIManager;

import bunnyEmu.main.handlers.ConfigHandler;
import bunnyEmu.main.net.Connection;
import bunnyEmu.main.net.LogonConnection;
import bunnyEmu.main.net.WorldSession;
import misc.Logger;

/**
 *
 * To login: Run and login with a WoW client with any username but make sure to
 * use the password: "password".
 *
 * @author Marijn
 */
public class Server extends Thread {

	public static String realmlist = null;

	// Replay
	private static BufferedReader replayReader = null;
	private static int currentReplayMs = 0;
	private static String nextLine;

	public static Properties prop = null;

	private ServerSocket serverSocket;
	private static ArrayList<Connection> connections = new ArrayList<Connection>(10);
	public static ArrayList<WorldSession> worldSessions = new ArrayList<WorldSession>(10);


	public static void main(String[] args) {
		try {
			Logger.printToConsole = true;

			prop = ConfigHandler.loadProperties();
			// Set default values
			prop.setProperty("enableGUI", prop.getProperty("enableGUI", "1"));

			realmlist = prop.getProperty("realmlistAddress", "127.0.0.1");
			if (realmlist.isEmpty()) {
				Logger.writeLog("Wrong realmlist set in server.conf, unable to start.", Logger.LOG_TYPE_ERROR);
				System.exit(0);
			}

			/* does user want a GUI */
			if (Integer.parseInt(prop.getProperty("enableGUI")) != 0) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				ServerWindow.create();
				Thread.sleep(200);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Server s = new Server();
		s.start();
		s.launch();
	}

	public void launch() {
		//RealmHandler.addRealm(new Realm(1, "Server test 1", "31.220.24.8", 3344, 1));
		listenSocket();
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	private void listenSocket() {
		try {
			Logger.writeLog("Launched NostalriusMemories - set your realmlist to 127.0.0.1.", Logger.LOG_TYPE_VERBOSE);

			InetAddress address = InetAddress.getByName(realmlist);
			serverSocket = new ServerSocket(3724, 0, address);

			Logger.writeLog("based on BunnyEmu: https://github.com/marijnz/BunnyEmu", Logger.LOG_TYPE_VERBOSE);

			/* console commands are handled by this thread if no GUI */
			if (Integer.parseInt(prop.getProperty("enableGUI")) == 0) {
				Runnable loggerRunnable = new ConsoleLoggerCMD();
				Thread loggerThread = new Thread(loggerRunnable);
				loggerThread.start();
			}

		} catch (IOException e) {
			Logger.writeLog("Network error: unable to bind ?", Logger.LOG_TYPE_WARNING);
		}

		try {
			while (true) {
				try {
					LogonConnection connection = new LogonConnection(serverSocket.accept());
					Logger.writeLog("Client connected to logon server.", Logger.LOG_TYPE_VERBOSE);
					synchronized (connections) {
						connections.add(connection);
					}
				} catch(NullPointerException e) {
					continue;
				}
			}
		} catch (IOException e) {
			Logger.writeLog("Accept failed: 3724", Logger.LOG_TYPE_WARNING);
		}
	}

    public void run(){
        long last_update = System.currentTimeMillis();
        while (true)
        {
            try {
                Thread.sleep(10);
                long current_update = System.currentTimeMillis();
                synchronized(connections) {
                	int diff = (int) (current_update - last_update);
                	update(diff);
                }
                last_update = current_update;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public void update(int msdiff){
    	if (nextLine == null || replayReader == null)
    		return;

    	currentReplayMs += msdiff;
    	while (nextLine != null && Integer.parseInt(nextLine.split(":", 2)[0]) < currentReplayMs)
    	{
        	String[] parts = nextLine.split(":", 2);
        	for (WorldSession c: worldSessions)
        		c.SendSerializedPacket(parts[1]);
        	try {
				nextLine = replayReader.readLine();
			} catch (IOException e) {
				nextLine = null;
			}
    	}
    	if (nextLine == null)
    	{
    		try {
				replayReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		replayReader = null;
    	}
    }
	public static boolean startReplay(File selectedFile) {
		// FileReader reads text files in the default encoding.
        FileReader fileReader;
		try {
			fileReader = new FileReader(selectedFile);
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
			return false;
		}

        // Always wrap FileReader in BufferedReader.
        replayReader = new BufferedReader(fileReader);

        /*
         * Read header:
         * BEGIN_TIME=130366
		 * RECORDER_LOWGUID=299439
         */
        try {
			String line = replayReader.readLine();
			currentReplayMs = Integer.parseInt(line.split("=")[1]);
			line = replayReader.readLine();
			if (!line.startsWith("RECORDER_LOWGUID"))
				nextLine = line;
			else
				nextLine = replayReader.readLine();
		} catch (IOException|NullPointerException|IndexOutOfBoundsException e) {
			replayReader = null;
			return false;
		}

        return true;
	}
}
