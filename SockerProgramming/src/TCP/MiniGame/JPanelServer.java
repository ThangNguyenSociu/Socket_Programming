package TCP.MiniGame;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import javax.swing.*;
import com.formdev.flatlaf.FlatClientProperties;

public class JPanelServer extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTextField txtPort, txtClientChoice, txtResult;
    private JButton btnStart, btnRoll;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public JPanelServer() {
    	Server();
    }

    private void Server() {
        this.setLayout(new BorderLayout(0, 20));
        this.setBackground(new Color(245, 247, 250)); 
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        topPanel.setOpaque(false);
        
        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        txtPort = new JTextField("1234", 10);
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPort.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 4,10,4,10"); 
        
        btnStart = new JButton("Mở Sòng (Start Server)");
        btnStart.setBackground(new Color(46, 204, 113)); 
        btnStart.setForeground(Color.WHITE);
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStart.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 4,20,4,20");
        
        topPanel.add(lblPort);
        topPanel.add(txtPort);
        topPanel.add(btnStart);
        this.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(25, 30, 30, 30) 
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblTitle = new JLabel("BÀN LẮC TÀI XỈU", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(44, 62, 80)); 
        centerPanel.add(lblTitle, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.4;
        JLabel lblChoice = new JLabel("Client Đặt Cược:", SwingConstants.RIGHT);
        lblChoice.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblChoice.setForeground(new Color(44, 62, 80));
        centerPanel.add(lblChoice, gbc);

        gbc.gridx = 1; gbc.weightx = 0.6;
        txtClientChoice = new JTextField(15);
        txtClientChoice.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtClientChoice.setForeground(new Color(231, 76, 60)); 
        txtClientChoice.setHorizontalAlignment(JTextField.CENTER);
        txtClientChoice.setEditable(false);
        txtClientChoice.putClientProperty(FlatClientProperties.STYLE, "arc: 15; margin: 6,10,6,10; background: #fdf2e9"); 
        centerPanel.add(txtClientChoice, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnRoll = new JButton("LẮC XÚC XẮC");
        btnRoll.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btnRoll.setBackground(new Color(243, 156, 18)); 
        btnRoll.setForeground(Color.WHITE);
        btnRoll.setPreferredSize(new Dimension(300, 75));
        btnRoll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRoll.setEnabled(false);
        btnRoll.putClientProperty(FlatClientProperties.STYLE, "arc: 25"); 
        centerPanel.add(btnRoll, gbc);

        gbc.gridy = 3;
        txtResult = new JTextField("Đang chờ người chơi vào bàn...", 30);
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

        btnStart.addActionListener(e -> startServer());
        btnRoll.addActionListener(e -> rollDice());
    }

    private void startServer() {
        int port = Integer.parseInt(txtPort.getText().trim());
        btnStart.setEnabled(false);
        btnStart.setText("Đang chờ người chơi...");

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                clientSocket = serverSocket.accept();
                SwingUtilities.invokeLater(() -> btnStart.setText("Người chơi đã vào bàn!"));
                
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());

                while (true) {
                    String type = dis.readUTF();
                    String choice = dis.readUTF();
                    
                    if ("TAIXIU".equals(type)) {
                        SwingUtilities.invokeLater(() -> {
                            txtClientChoice.setText(choice);
                            txtResult.setText("Đang chờ nhà cái lắc...");
                            btnRoll.setEnabled(true);
                        });
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void rollDice() {
        if (dos == null) return;
        new Thread(() -> {
            try {
                String clientChoice = txtClientChoice.getText();
                Random rand = new Random();
                int d1 = rand.nextInt(6) + 1;
                int d2 = rand.nextInt(6) + 1;
                int d3 = rand.nextInt(6) + 1;
                int sum = d1 + d2 + d3;
                
                String realResult = (sum >= 11) ? "TÀI" : "XỈU";
                String winOrLose = clientChoice.equals(realResult) ? "CLIENT THẮNG!" : "CLIENT THUA!";
                String finalMsg = String.format("[%d-%d-%d] Tổng %d: %s -> %s", d1, d2, d3, sum, realResult, winOrLose);
                
                SwingUtilities.invokeLater(() -> {
                    txtResult.setText(finalMsg);
                    btnRoll.setEnabled(false); 
                });
                
                dos.writeUTF("TAIXIU");
                dos.writeUTF(finalMsg);
                dos.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}