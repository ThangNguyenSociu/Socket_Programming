package TCP.ChatPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import com.formdev.flatlaf.FlatClientProperties;

public class JPanelClient extends JPanel {

	private static final long serialVersionUID = 1L;

	public JPanelClient() {
		System.out.println("Client");
		Client();
	}
	
	private void Client() {
	    this.setBackground(new Color(250, 250, 249));
	    this.setLayout(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(10, 10, 10, 10); 
	    gbc.fill = GridBagConstraints.HORIZONTAL;

	    JLabel lblIP = new JLabel("IP Server:");
	    lblIP.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 0; 
	    this.add(lblIP, gbc);

	    JTextField txtIP = new JTextField("127.0.0.1");
	    txtIP.setPreferredSize(new Dimension(120, 35));
	    txtIP.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    txtIP.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    txtIP.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ví dụ: 192.168.1.5");
	    gbc.gridx = 1;
	    gbc.weightx = 0.5; 
	    this.add(txtIP, gbc);

	    JLabel lblPort = new JLabel("Port:");
	    lblPort.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    gbc.gridx = 2;
	    gbc.weightx = 0; 
	    this.add(lblPort, gbc);

	    JTextField txtPort = new JTextField("1234");
	    txtPort.setPreferredSize(new Dimension(80, 35));
	    txtPort.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    txtPort.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    gbc.gridx = 3;
	    gbc.weightx = 0.3; 
	    this.add(txtPort, gbc);

	    JButton btnConnect = new JButton("Kết nối Server");
	    btnConnect.setPreferredSize(new Dimension(140, 35));
	    btnConnect.setBackground(new Color(46, 204, 113)); 
	    btnConnect.setForeground(Color.WHITE);
	    btnConnect.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btnConnect.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    btnConnect.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    gbc.gridx = 4;
	    gbc.weightx = 0;
	    this.add(btnConnect, gbc);

	    JTextArea txtLog = new JTextArea();
	    txtLog.setEditable(false); 
	    txtLog.setFont(new Font("Times New Roman", Font.PLAIN, 15)); 
	    txtLog.setLineWrap(true); 
	    txtLog.setWrapStyleWord(true);
	    
	    JScrollPane scrollPane = new JScrollPane(txtLog);
	    scrollPane.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY), "Nhật ký hệ thống (Logs)"));
	    scrollPane.putClientProperty("FlatClientProperties.arc", 15);

	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.gridwidth = 5;
	    gbc.weighty = 1.0; 
	    gbc.fill = GridBagConstraints.BOTH; 
	    this.add(scrollPane, gbc);
	    
	    JPanel chatPanel = new JPanel(new BorderLayout(10, 0));
	    chatPanel.setBackground(new Color(250, 250, 249));

	    JTextField txtMessage = new JTextField();
	    txtMessage.setFont(new Font("Times New Roman", Font.PLAIN, 15));
	    txtMessage.setPreferredSize(new Dimension(400, 40));
	    txtMessage.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tin nhắn để gửi đến Server...");
	    txtMessage.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 0,10,0,10");

	    JButton btnSend = new JButton("Gửi");
	    btnSend.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    btnSend.setBackground(new Color(52, 152, 219)); 
	    btnSend.setForeground(Color.WHITE);
	    btnSend.setPreferredSize(new Dimension(100, 40));
	    btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btnSend.putClientProperty(FlatClientProperties.STYLE, "arc: 15");
	    
	    txtMessage.setEnabled(false);
	    btnSend.setEnabled(false);

	    chatPanel.add(txtMessage, BorderLayout.CENTER);
	    chatPanel.add(btnSend, BorderLayout.EAST);

	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.gridwidth = 5;
	    gbc.weighty = 0; 
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    this.add(chatPanel, gbc);
	    
	    
	    final PrintWriter[] clientOut = {null};
	    final Socket[] currentSocket = {null};

	    ActionListener sendAction = e -> {
	        String msg = txtMessage.getText().trim();
	        if (!msg.isEmpty() && clientOut[0] != null) {
	            txtLog.append("Tôi (Client): " + msg + "\n");
	            clientOut[0].println(msg);
	            txtMessage.setText("");
	        }
	    };
	    
	    btnSend.addActionListener(sendAction);
	    txtMessage.addActionListener(sendAction);

	    btnConnect.addActionListener(e -> {
	        String ip = txtIP.getText().trim();
	        String portStr = txtPort.getText().trim();

	        if (ip.isEmpty() || portStr.isEmpty()) {
	            txtLog.append("Vui lòng nhập đầy đủ IP và Port!\n");
	            return;
	        }

	        try {
	            int port = Integer.parseInt(portStr);
	            
	            txtLog.append("Đang kết nối tới " + ip + ":" + port + "...\n");
	            btnConnect.setEnabled(false);
	            txtIP.setEditable(false);
	            txtPort.setEditable(false);

	            Thread clientThread = new Thread(() -> {
	                try {
	                    currentSocket[0] = new Socket(ip, port);
	                    clientOut[0] = new PrintWriter(currentSocket[0].getOutputStream(), true);
	                    BufferedReader in = new BufferedReader(new InputStreamReader(currentSocket[0].getInputStream()));
	                    
	                    SwingUtilities.invokeLater(() -> {
	                        txtLog.append("Đã kết nối thành công tới Server!\n");
	                        txtMessage.setEnabled(true);
	                        btnSend.setEnabled(true);
	                    });

	                    String messageFromServer;
	                    while ((messageFromServer = in.readLine()) != null) {
	                        String finalMsg = messageFromServer;
	                        SwingUtilities.invokeLater(() -> 
	                            txtLog.append(finalMsg + "\n")
	                        );
	                    }
	                    
	                } catch (Exception ex) {
	                    SwingUtilities.invokeLater(() -> {
	                        txtLog.append("Mất kết nối hoặc không tìm thấy Server: " + ex.getMessage() + "\n");
	                        btnConnect.setEnabled(true);
	                        txtIP.setEditable(true);
	                        txtPort.setEditable(true);
	                        txtMessage.setEnabled(false);
	                        btnSend.setEnabled(false);
	                    });
	                }
	            });
	            
	            clientThread.start();
	            
	        } catch (NumberFormatException ex) {
	            txtLog.append("Vui lòng nhập Port là một số!\n");
	        }
	    });
	}

}
