package TCP.FilePackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private File selectedFile;
    private JTextArea txtLog;
    private String lastReceivedFileName = "";

	public JPanelClient() {
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

	    JButton btnDowload = new JButton("Dowload");
	    btnDowload.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    btnDowload.setBackground(new Color(52, 152, 219));
	    btnDowload.setForeground(Color.WHITE);
	    btnDowload.setPreferredSize(new Dimension(100, 40));
	    btnDowload.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btnDowload.putClientProperty(FlatClientProperties.STYLE, "arc: 15");

	    JButton btnSelectFILE = new JButton("Chọn FILE");
	    btnSelectFILE.setFont(new Font("Times New Roman", Font.BOLD, 14));
	    btnSelectFILE.setBackground(new Color(52, 152, 219));
	    btnSelectFILE.setForeground(Color.WHITE);
	    btnSelectFILE.setPreferredSize(new Dimension(100, 40));
	    btnSelectFILE.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    btnSelectFILE.putClientProperty(FlatClientProperties.STYLE, "arc: 15");

//	    txtMessage.setEnabled(false);
	    btnSend.setEnabled(false);

//	    chatPanel.add(txtMessage, BorderLayout.CENTER);
	    chatPanel.add(btnSend, BorderLayout.EAST);
	    chatPanel.add(btnDowload, BorderLayout.CENTER);
	    chatPanel.add(btnSelectFILE, BorderLayout.WEST);

	    btnConnect.addActionListener(e -> {
	    	new Thread(() -> {
	    		try {
	    			String ip = txtIP.getText().trim();
	    			String stringPort = txtPort.getText().trim();
	    			int port = Integer.parseInt(stringPort);
	    			socket = new Socket(ip, port);
	    			InputStream in = socket.getInputStream();
	    			OutputStream out = socket.getOutputStream();

	    			dis = new DataInputStream(in);
	    			dos = new DataOutputStream(out);
	    			System.out.println("Kết nối thành công đến server");
	    			txtLog.append("Kết nối thành công đến Server "+ "Port[" + port + "]" + "\n");
	    			SwingUtilities.invokeLater(() -> {
//                        txtLog.append("Đã kết nối thành công tới Server!\n");
                        btnSend.setEnabled(true);
                    });
	    			startReceivingFile();

	    		} catch (IOException ex) {
	    			ex.printStackTrace();
	    			JOptionPane.showMessageDialog(null, "Không thể kết nối tới Server!");
	    		}
	    	}).start();
	    });

	    btnSelectFILE.addActionListener(e -> {
	    	JFileChooser fileChooser = new JFileChooser();
	    	int result = fileChooser.showOpenDialog(this);
	    	if (result == JFileChooser.APPROVE_OPTION) {
	            selectedFile = fileChooser.getSelectedFile();

	            String fileName = selectedFile.getName();
	            long fileSize = selectedFile.length();

	            txtLog.append("Đã chọn: " + fileName + " (" + fileSize + " bytes)" + "\n");

	            System.out.println("Đường dẫn file: " + selectedFile.getAbsolutePath());
	        }
	    });

	    btnSend.addActionListener(e -> {
	        if (selectedFile == null) {
	            JOptionPane.showMessageDialog(this, "Vui lòng chọn file trước!");
	            return;
	        }
	        String fileName = selectedFile.getName();

	        new Thread(() -> {
	            try {
	                dos.writeUTF(selectedFile.getName());
	                dos.writeLong(selectedFile.length());
	                dos.flush();

	                SwingUtilities.invokeLater(() -> {
	                    txtLog.append("Đang gửi file: " + fileName + " tới Server...\n");
	                });

	                FileInputStream fis = new FileInputStream(selectedFile);
	                byte[] buffer = new byte[4096];
	                int bytesRead;

	                while ((bytesRead = fis.read(buffer)) != -1) {
	                    dos.write(buffer, 0, bytesRead);
	                }

	                dos.flush();
	                fis.close();
	                SwingUtilities.invokeLater(() -> {
	                    txtLog.append("Đã gửi thành công file: " + fileName + "\n");
	                });
	                JOptionPane.showMessageDialog(this, "Gửi file thành công!");
	                System.out.print("Gửi FIle thành công");
	            } catch (IOException ex) {
	                JOptionPane.showMessageDialog(this, "Lỗi gửi file: " + ex.getMessage());
	            }
	        }).start();
	    });

	    btnDowload.addActionListener(e -> {
	        if (lastReceivedFileName == null || lastReceivedFileName.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Chưa có file nào để tải xuống!");
	            return;
	        }

	        File tempFile = new File(lastReceivedFileName);
	        JFileChooser fileChooser = new JFileChooser();
	        fileChooser.setSelectedFile(new File(lastReceivedFileName.replace("client_received_", "")));

	        int result = fileChooser.showSaveDialog(this);
	        if (result == JFileChooser.APPROVE_OPTION) {
	            File saveFile = fileChooser.getSelectedFile();

	            try (FileInputStream in = new FileInputStream(tempFile);
	                 FileOutputStream out = new FileOutputStream(saveFile)) {

	                byte[] buffer = new byte[4096];
	                int length;
	                while ((length = in.read(buffer)) > 0) {
	                    out.write(buffer, 0, length);
	                }

	                JOptionPane.showMessageDialog(this, "Tải file thành công vào:\n" + saveFile.getAbsolutePath());
	                txtLog.append("Đã lưu file: " + saveFile.getName() + "\n");

	            } catch (IOException ex) {
	                JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage());
	            }
	        }
	    });

	}

	private void startReceivingFile() {
	    new Thread(() -> {
	        try {
	            while (true) {
	                String fileName = dis.readUTF();
	                long fileSize = dis.readLong();

	                System.out.println("Đang nhận file: " + fileName + " (" + fileSize + " bytes)");
	                SwingUtilities.invokeLater(() -> {
	                    txtLog.append("Bắt đầu nhận file: " + fileName + " (" + fileSize + " bytes) từ Server...\n");
	                });
	                File file = new File("received_" + fileName);
	                FileOutputStream fos = new FileOutputStream(file);

	                byte[] buffer = new byte[4096];
	                int bytesRead;
	                long totalRead = 0;

	                while (totalRead < fileSize &&
	                      (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {

	                    fos.write(buffer, 0, bytesRead);
	                    totalRead += bytesRead;
	                }

	                fos.close();
	                lastReceivedFileName = "client_received_" + fileName;
	                SwingUtilities.invokeLater(() -> {
	                    txtLog.append("Nhận thành công file: " + fileName + "\n");
	                });
	                System.out.println("Nhận thành công file: " + fileName);

	                JOptionPane.showMessageDialog(this, "Đã nhận xong: " + fileName);
	            }
	        } catch (IOException ex) {
	            System.err.println("Mất kết nối với Client: " + ex.getMessage());
	        }
	    }).start();
	}

}
