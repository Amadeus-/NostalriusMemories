package bunnyEmu.main;

import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;

public class ServerWindow implements ActionListener {

	public static ServerWindow sWindow;
	private JFrame frame;
	private static JTextArea textArea;
	private static JButton replayButton;

	/**
	 * Launch the application.
	 */
	public static void create() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					sWindow = new ServerWindow();
					sWindow.frame.setVisible(true);
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
		Container panel = frame.getContentPane();

		textArea = new JTextArea();
		textArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textArea.setColumns(65);
		textArea.setRows(14);

		replayButton = new JButton("Load Replay");
		replayButton.addActionListener(this);
		replayButton.setEnabled(false);
		replayButton.setText("Load Replay - Login a character first");

		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JEditorPane welcomeMsg = new JEditorPane();
		welcomeMsg.setContentType("text/html");//set content as html
		welcomeMsg.setText("<p style='text-align:center'><a href='https://github.com/NostalriusVanilla/NostalriusMemories'>Nostalrius Memories</a> -"
				+ " Based on <a href='https://github.com/marijnz/BunnyEmu'>BunnyEmu</a>"
				+ "<br />"
				+ "<table>"
				+ "<tr>"
				+ "<td>Instructions:"
				+ "<ol>"
				+ "<li>set realmlist 127.0.0.1</li>"
				+ "<li>Login: <strong>Nostalrius</strong></li>"
				+ "<li>Password: <strong>Memories</strong></li>"
				+ "<li>Load your character and press 'Load Replay'</li>"
				+ "</ol>"
				+ "</td><td>"
				+ "<a href='https://nostalrius.org/'>http://nostalrius.org/</a><br />"
				+ "@NostalBegins "
				+ "<a href='https://twitter.com/NostalBegins'>Twitter</a> / <a href='https://www.facebook.com/NostalBegins'>Facebook</a><br />"
				+ "<a href='https://www.change.org/p/michael-morhaime-legacy-server-among-world-of-warcraft-community'>Open letter for legacy servers</a>"
				+ "</td></tr></table>"
				+ "</p>");

		welcomeMsg.setEditable(false);//so its not editable
		welcomeMsg.setOpaque(false);//so we dont see whit background
		welcomeMsg.setSize(welcomeMsg.getPreferredSize());
		welcomeMsg.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    System.out.println(hle.getURL());
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(hle.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

		panel.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
		panel.add(welcomeMsg);
		panel.add(replayButton);
		panel.add(Box.createRigidArea(new Dimension(0,10)));
		panel.add(textArea);
	}

	public static void appendOut(String text){
		if(textArea != null)
			textArea.append(text + "\n");
	}

	public void enableReplay() {
		replayButton.setEnabled(true);
		replayButton.setText("Load Replay");
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
