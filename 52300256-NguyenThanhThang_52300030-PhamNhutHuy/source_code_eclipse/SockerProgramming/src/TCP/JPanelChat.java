package TCP;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.formdev.flatlaf.FlatClientProperties;

import TCP.ChatPackage.JPanelClient;
import TCP.ChatPackage.JPanelServer;

public class JPanelChat extends JPanel {

	private CardLayout cardLayout;
	private JPanel contentArea;
	private static final Color contentAreaColor = new Color(250, 250, 249);
	private static final long serialVersionUID = 1L;
	private static final Color primaryColor = new Color(93, 193, 242);
	private static final Font textBtnFont = new Font("Times New Roman", Font.BOLD, 20);

	public JPanelChat() {
		System.out.println("Nội dung Chat");
		ChatHome();
	}

	private void ChatHome() {
		this.setBackground(new Color(250, 250, 249));
	    this.setLayout(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();

	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.gridx = 0;
	    gbc.weightx = 1.0;

	    JPanel topPanel = new JPanel(new GridBagLayout());
	    topPanel.setBackground(new Color(214, 235, 253));
	    gbc.gridy = 0;
	    gbc.weighty = 0.1;
	    this.add(topPanel, gbc);

	    GridBagConstraints btnGbc = new GridBagConstraints();
	    btnGbc.insets = new java.awt.Insets(0, 10, 0, 10);
	    btnGbc.gridy = 0;

	    JButton btnClient = createButtonTCP("Client");
	    btnGbc.gridx = 0;
	    topPanel.add(btnClient, btnGbc);

	    JButton btnServer = createButtonTCP("Server");
	    btnGbc.gridx = 1;
	    topPanel.add(btnServer, btnGbc);

	    JPanel bottomPanel = new JPanel(new BorderLayout());
	    bottomPanel.setBackground(new Color(250, 250, 249));
	    gbc.gridy = 1;
	    gbc.weighty = 0.9;
	    this.add(bottomPanel, gbc);

	    cardLayout = new CardLayout();
	    contentArea = new JPanel();
	    contentArea.setLayout(cardLayout);
	    contentArea.setBackground(contentAreaColor);

	    JPanelClient panelClient = new JPanelClient();
	    JPanelServer panelServer = new JPanelServer();

	    contentArea.add(panelClient, "Client");
	    contentArea.add(panelServer, "Server");

	    bottomPanel.add(contentArea, BorderLayout.CENTER);

	    btnClient.addActionListener(e -> cardLayout.show(contentArea, "Client"));
	    btnServer.addActionListener(e -> cardLayout.show(contentArea, "Server"));

	}

	private JButton createButtonTCP(String text) {
		JButton btn = new JButton(text);
		btn.setFont(textBtnFont);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setBackground(primaryColor);
		btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension btnSize = new Dimension(200, 40);
        btn.setPreferredSize(btnSize);
        btn.setMinimumSize(btnSize);
        btn.setMaximumSize(btnSize);
        return btn;
	}

}
