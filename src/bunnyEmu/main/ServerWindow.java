package bunnyEmu.main;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import bunnyEmu.main.handlers.RealmHandler;

public class ServerWindow implements ActionListener {

	private JFrame frame;
	private static JTextArea textArea;
	private static JButton replayButton;
	private JPanel commandPanel;

	/**
	 * Launch the application.
	 */
	public static void create() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerWindow window = new ServerWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ServerWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Nostalrius Memories");
		frame.setBounds(100, 100, 596, 319);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLabel lblBunnyEmu = new JLabel("Nostalrius Memories - Based on BunnyEmu");
		lblBunnyEmu.setToolTipText("");
		lblBunnyEmu.setBounds(10, 10, 207, 14);
		frame.getContentPane().add(lblBunnyEmu);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(10, 35, 560, 235);
		frame.getContentPane().add(tabbedPane);

		/// Output
		JPanel outputPanel = new JPanel();
		outputPanel.setToolTipText("");
		tabbedPane.addTab("Memories", null, outputPanel, null);

		textArea = new JTextArea();
		textArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textArea.setColumns(65);
		textArea.setRows(14);
		outputPanel.add(textArea);

		replayButton = new JButton("Replay...");
		replayButton.addActionListener(this);
		outputPanel.add(replayButton);

		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane scroll = new JScrollPane (textArea);
		outputPanel.add(scroll);

		// Infos tab
		JPanel infoPanel = new JPanel();
		tabbedPane.addTab("Info", null, infoPanel, null);
		infoPanel.setLayout(null);

		final JLabel memoryLabel = new JLabel("Memory usage:");
		memoryLabel.setBounds(30, 11, 195, 22);
		infoPanel.add(memoryLabel);

		final JLabel clientsLabel = new JLabel("Clients logged in:");
		clientsLabel.setBounds(30, 66, 195, 22);
		infoPanel.add(clientsLabel);

		commandPanel = new JPanel();
		JTextArea commandArea = new JTextArea();
		commandArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		commandArea.setColumns(65);
		commandArea.setRows(14);

		DefaultCaret caret2 = (DefaultCaret)commandArea.getCaret();
		caret2.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane scroll3 = new JScrollPane (commandArea);
		commandPanel.add(scroll3);
		tabbedPane.addTab("Command", null, commandPanel, null);

		/* console commands are handled by this thread */
		Runnable loggerRunnable = new ConsoleLoggerGUI(commandArea);
		Thread loggerThread = new Thread(loggerRunnable);
		loggerThread.start();

		new Thread(){
			@Override
			public void run(){
				while(true){
					memoryLabel.setText("Kb memory usage: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024);
					clientsLabel.setText("Logged in clients: " + RealmHandler.getAllClientsAllRealms().size());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public static void appendOut(String text){
		if(textArea != null)
			textArea.append(text + "\n");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JFileChooser fileChooser = new JFileChooser();
		String path = ServerWindow.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			fileChooser.setCurrentDirectory(new File(decodedPath));
			int result = fileChooser.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
			    File selectedFile = fileChooser.getSelectedFile();
			    appendOut("Loading replay: " + selectedFile.getAbsolutePath());
			    Server.startReplay(selectedFile);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
}
