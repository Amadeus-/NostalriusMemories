package bunnyEmu.main;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextArea;

/**
 * Command part of the GUI
 *
 * @author Wazy
 *
 */
/* handle console commands here */
public class ConsoleLoggerGUI implements Runnable {

	/* The ConsoleLogger output */
	private JTextArea textArea;
	private String command = "";

	public ConsoleLoggerGUI(JTextArea textArea){
		this.textArea = textArea;
	}

	public void run() {
		textArea.append("Type commands below after >>> indicators.\n");
		try {
			textArea.addKeyListener(new KeyAdapter() {
            	private StringBuilder commandBuilder = new StringBuilder();
	            @Override
	            public void keyTyped(KeyEvent e) {
	                if (((int) e.getKeyChar()) == 10) {
	                	command = commandBuilder.toString();
	                	commandBuilder.setLength(0);
	                }
	                else {
	                	commandBuilder.append(e.getKeyChar());
	                }
	                super.keyTyped(e);
	            }
	        });
			while (true) {
				textArea.append(">>> ");
				// not ready to read anything yet

				while (command.isEmpty()) {
					Thread.sleep(200);
				}

				if (command.equals("commands") || command.equals("help")) {
					textArea.append("Available commands are: {'shutdown', 'help', 'commands'}.\n");
				}
				else if (command.equals("shutdown")) {
					textArea.append("\n!!!Console shutdown imminent!!!\n");
					System.exit(0);
				}
				else {
					textArea.append("Unrecognized command. Try typing 'help'.\n");
				}
				command = "";
			}
		}
		catch (Exception e) {};
	}
}
