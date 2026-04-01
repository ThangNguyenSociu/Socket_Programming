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
import java.net.ServerSocket;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.formdev.flatlaf.FlatClientProperties;

public class JPanelServer extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTextArea txtLog;
	private JTextField txtPort;
	private JButton btnStart;

	public JPanelServer() {
		System.out.println("Server");
		Server();
	}
	
	private void Server() {
	    this.setBackground(new Color(250, 250, 249));
	    this.setLayout(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(10, 10, 10, 10); 
	    gbc.fill = GridBagConstraints.HORIZONTAL;

	    JLabel lblPort = new JLabel("Port Server:");
	    lblPort.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 0; 
	    this.add(lblPort, gbc);

	    txtPort = new JTextField("1234");
	    txtPort.setPreferredSize(new Dimension(100, 35));
	    txtPort.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    txtPort.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    txtPort.putClientProperty("FlatClientProperties.placeholderText", "Ví dụ: 8080");
	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.weightx = 1.0; 
	    this.add(txtPort, gbc);

	    btnStart = new JButton("Mở kết nối Server");
	    btnStart.setPreferredSize(new Dimension(150, 35));
	    btnStart.setBackground(new Color(46, 204, 113)); 
	    btnStart.setForeground(Color.WHITE);
	    btnStart.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    btnStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btnStart.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    btnStart.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    
	    gbc.gridx = 2;
	    gbc.gridy = 0;
	    gbc.weightx = 0;
	    this.add(btnStart, gbc);

	    txtLog = new JTextArea();
	    txtLog.setEditable(false); 
	    txtLog.setFont(new Font("Times New Roman", Font.PLAIN, 15)); 
	    txtLog.setLineWrap(true); 
	    txtLog.setWrapStyleWord(true);
	    
	    JScrollPane scrollPane = new JScrollPane(txtLog);
	    scrollPane.setBorder(new TitledBorder(new LineBorder(Color.LIGHT_GRAY), "Nhật ký hệ thống (Logs)"));
	    scrollPane.putClientProperty("FlatClientProperties.arc", 15);

	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.gridwidth = 3; 
	    gbc.weighty = 1.0; 
	    gbc.fill = GridBagConstraints.BOTH; 
	    this.add(scrollPane, gbc);
	    
	    JPanel chatPanel = new JPanel(new BorderLayout(10, 0));
	    chatPanel.setBackground(new Color(250, 250, 249));

	    JTextField txtMessage = new JTextField();
	    txtMessage.setFont(new Font("Times New Roman", Font.PLAIN, 15));
	    txtMessage.setPreferredSize(new Dimension(400, 40));
	    txtMessage.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tin nhắn để gửi đến Client...");
	    txtMessage.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 0,10,0,10"); // Thêm lề trái/phải cho chữ

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
	    gbc.gridwidth = 3;
	    gbc.weighty = 0; 
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    this.add(chatPanel, gbc);
	    
	    List<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();
	    
	    ActionListener sendAction = e -> {
	        String msg = txtMessage.getText().trim();
	        if (!msg.isEmpty()) {
	            txtLog.append("Server: " + msg + "\n");
	            txtMessage.setText("");
	    
	            for (PrintWriter writer : clientWriters) {
	                writer.println("Server: " + msg);
	            }
	        }
	    };
	    btnSend.addActionListener(sendAction);
	    txtMessage.addActionListener(sendAction); 

	    btnStart.addActionListener(e -> {
	        try {
	            int port = Integer.parseInt(txtPort.getText().trim());
	            
	            txtLog.append("Đang khởi động Server tại Port " + port + "...\n");
	            btnStart.setEnabled(false);
	            txtPort.setEditable(false);

	            Thread serverThread = new Thread(() -> {
	                try {
	                    ServerSocket serverSocket = new ServerSocket(port);
	                    
	                    SwingUtilities.invokeLater(() -> {
	                        txtLog.append("Server đang lắng nghe. Chờ Client kết nối...\n");
	                        txtMessage.setEnabled(true);
	                        btnSend.setEnabled(true);
	                    });

	                    while (true) {
	                        Socket clientSocket = serverSocket.accept();
	                        String clientIP = clientSocket.getInetAddress().getHostAddress();
	                        
	                        SwingUtilities.invokeLater(() -> 
	                            txtLog.append("Client đã kết nối từ IP: " + clientIP + "\n")
	                        );

	                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	                        clientWriters.add(out);

	                        new Thread(() -> {
	                            try {
	                                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	                                String messageFromClient;
	                                while ((messageFromClient = in.readLine()) != null) {
	                                    String finalMsg = messageFromClient;
	                                    SwingUtilities.invokeLater(() -> 
	                                        txtLog.append("Client (" + clientIP + "): " + finalMsg + "\n")
	                                    );
	                                }
	                            } catch (Exception ex) {
	                                SwingUtilities.invokeLater(() -> 
	                                    txtLog.append("Client " + clientIP + " đã ngắt kết nối.\n")
	                                );
	                            } finally {
	                                clientWriters.remove(out); // Xóa khỏi danh sách khi Client thoát
	                            }
	                        }).start();
	                    }
	                    
	                } catch (Exception ex) {
	                    SwingUtilities.invokeLater(() -> {
	                        txtLog.append("Lỗi Server: " + ex.getMessage() + "\n");
	                        btnStart.setEnabled(true);
	                        txtPort.setEditable(true);
	                    });
	                }
	            });
	            
	            serverThread.start();
	            
	        } catch (NumberFormatException ex) {
	            txtLog.append("Vui lòng nhập Port là một số hợp lệ!\n");
	        }
	    });
	    
	}

}
