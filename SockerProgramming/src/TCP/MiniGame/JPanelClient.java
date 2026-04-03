package TCP.MiniGame;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import com.formdev.flatlaf.FlatClientProperties;

public class JPanelClient extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTextField txtIP, txtPort, txtResult;
    private JButton btnConnect, btnBetTai, btnBetXiu;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String myChoice = "";

    public JPanelClient() {
    	Client();
    }

    private void Client() {
        this.setLayout(new BorderLayout(0, 20));
        this.setBackground(new Color(245, 247, 250)); 
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); 

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        topPanel.setOpaque(false);
        
        JLabel lblIp = new JLabel("IP:");
        lblIp.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        txtIP = new JTextField("localhost", 10);
        txtIP.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtIP.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 4,10,4,10"); 
        
        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        txtPort = new JTextField("1234", 6);
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPort.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 4,10,4,10");

        btnConnect = new JButton("Vào Bàn");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnect.setBackground(new Color(52, 152, 219)); 
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 4,20,4,20"); 
        
        topPanel.add(lblIp); topPanel.add(txtIP);
        topPanel.add(lblPort); topPanel.add(txtPort);
        topPanel.add(btnConnect);
        this.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE); 
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(25, 30, 30, 30) 
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 15, 15); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblTitle = new JLabel("CHỌN KÈO CỦA BẠN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80)); 
        centerPanel.add(lblTitle, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.5;
        btnBetTai = new JButton("TÀI (11-18)");
        btnBetTai.setFont(new Font("Segoe UI", Font.BOLD, 26));
        btnBetTai.setBackground(new Color(231, 76, 60)); 
        btnBetTai.setForeground(Color.WHITE);
        btnBetTai.setPreferredSize(new Dimension(160, 90));
        btnBetTai.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBetTai.setEnabled(false);
        btnBetTai.putClientProperty(FlatClientProperties.STYLE, "arc: 25"); 
        centerPanel.add(btnBetTai, gbc);

        gbc.gridx = 1;
        btnBetXiu = new JButton("XỈU (3-10)");
        btnBetXiu.setFont(new Font("Segoe UI", Font.BOLD, 26));
        btnBetXiu.setBackground(new Color(44, 62, 80)); 
        btnBetXiu.setForeground(Color.WHITE);
        btnBetXiu.setPreferredSize(new Dimension(160, 90));
        btnBetXiu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBetXiu.setEnabled(false);
        btnBetXiu.putClientProperty(FlatClientProperties.STYLE, "arc: 25"); 
        centerPanel.add(btnBetXiu, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        txtResult = new JTextField("Vui lòng kết nối để chơi...", 30);
        txtResult.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtResult.setEditable(false);
        txtResult.setHorizontalAlignment(JTextField.CENTER);
        txtResult.setForeground(new Color(127, 140, 141));
        
        txtResult.putClientProperty(FlatClientProperties.STYLE, 
        	    "arc: 20; borderWidth: 0; background: #ecf0f1; margin: 10,10,10,10");
        txtResult.setPreferredSize(new Dimension(350, 55));
        centerPanel.add(txtResult, gbc);

        JPanel wrapperCenter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapperCenter.setOpaque(false);
        wrapperCenter.add(centerPanel);
        
        this.add(wrapperCenter, BorderLayout.CENTER);

        btnConnect.addActionListener(e -> connectToServer());
        btnBetTai.addActionListener(e -> sendBet("TÀI"));
        btnBetXiu.addActionListener(e -> sendBet("XỈU"));
    }

    private void connectToServer() {
        String ip = txtIP.getText().trim();
        int port = Integer.parseInt(txtPort.getText().trim());

        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                SwingUtilities.invokeLater(() -> {
                    btnConnect.setEnabled(false);
                    btnConnect.setText("Đã kết nối");
                    btnBetTai.setEnabled(true);
                    btnBetXiu.setEnabled(true);
                    txtResult.setText("Chọn TÀI hoặc XỈU để cược!");
                });

                while (true) {
                    String type = dis.readUTF();
                    String result = dis.readUTF();
                    
                    if ("TAIXIU".equals(type)) {
                        SwingUtilities.invokeLater(() -> {
                            txtResult.setText(result);
                            btnBetTai.setEnabled(true);
                            btnBetXiu.setEnabled(true);
                        });
                    }
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> txtResult.setText("Lỗi kết nối: " + ex.getMessage()));
            }
        }).start();
    }

    private void sendBet(String choice) {
        if (dos == null) return;
        myChoice = choice;
        new Thread(() -> {
            try {
                dos.writeUTF("TAIXIU");
                dos.writeUTF(myChoice);
                dos.flush();
                
                SwingUtilities.invokeLater(() -> {
                    txtResult.setText("Đã cược " + myChoice + ". Đang chờ nhà cái lắc...");
                    btnBetTai.setEnabled(false);
                    btnBetXiu.setEnabled(false);
                });
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}