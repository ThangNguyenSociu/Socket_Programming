package TCP.Calculate.RowServer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.Stack;
import com.formdev.flatlaf.FlatClientProperties;

public class JPanelRowConnect extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTextArea txtLog;
	private JTextField txtPort;
	private JButton btnStart;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private double res;
    private JPanelRow1 panelRow1;
    private JPanelRow2 panelRow2;
    private JPanelRow3 panelRow3;

	public JPanelRowConnect(JPanelRow1 panelRow1, JPanelRow2 panelRow2, JPanelRow3 panelRow3) {
		this.panelRow1 = panelRow1;
		this.panelRow2 = panelRow2;
		this.panelRow3 = panelRow3;
		Connect();
	}
	
	private void Connect() {
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
		    
		    setupEvents();
	}
	
	private void setupEvents() {
	    btnStart.addActionListener(e -> {
	        String portText = txtPort.getText().trim();
	        int port;
	        try { port = Integer.parseInt(portText); } 
	        catch (NumberFormatException ex) { return; }

	        btnStart.setEnabled(false);
	        btnStart.setText("Đang mở Server...");

	        new Thread(() -> {
	            try {
	                if (serverSocket == null || serverSocket.isClosed()) {
	                    serverSocket = new ServerSocket(port);
	                }
	                
	                SwingUtilities.invokeLater(() -> btnStart.setText("Server đang chạy (Port " + port + ")"));
	                System.out.println("Đợi Client trên cổng " + port + "...");
	                
	                clientSocket = serverSocket.accept();
	                System.out.println("Client đã kết nối!");
	                
	                dis = new DataInputStream(clientSocket.getInputStream());
	                dos = new DataOutputStream(clientSocket.getOutputStream());

	                while (true) {
	                    String type = dis.readUTF(); 
	                    String expression = dis.readUTF(); 
	                    
	                    SwingUtilities.invokeLater(() -> {
	                        if ("PREFIX".equals(type) && panelRow1 != null) {
	                            panelRow1.setPrefixText(expression);
	                            panelRow1.setPrefixTextResult(""); 
	                            
	                        } else if ("USD".equals(type) && panelRow2 != null) {
	                            panelRow2.setUsdText(expression);
	                            panelRow2.setUsdTextResult(""); 
	                        } else if ("REVERSE".equals(type) && panelRow3 != null) {
	                            panelRow3.setStringText(expression);
	                            panelRow3.setStringTextResult(""); 
	                        }
	                    });
	                }
	            } catch (IOException ex) {
	                System.err.println("Lỗi Server: " + ex.getMessage());
	                SwingUtilities.invokeLater(() -> {
	                    btnStart.setEnabled(true);
	                    btnStart.setText("Mở kết nối Server");
	                });
	            }
	        }).start();
	    });

	    if (panelRow1 != null) {
	        panelRow1.getBtnSend().addActionListener(e -> {
	            if (dos == null) {
	                System.out.println("Chưa có Client nào kết nối để gửi!");
	                return;
	            }
	            
	            new Thread(() -> {
	                try {
	                    String expression = panelRow1.getPrefixText();
	                    
	                    String result = calculateResult("PREFIX", expression);
	                    setResultText(result);    
	                    
	                    dos.writeUTF("PREFIX");
	                    dos.writeUTF(result);
	                    dos.flush();
	                    System.out.println("Đã gửi kết quả Prefix cho Client: " + result);
	                } catch (Exception ex) {
	                    System.err.println("Lỗi khi gửi kết quả: " + ex.getMessage());
	                }
	            }).start();
	        });
	    }
	    if (panelRow2 != null) {
	        panelRow2.getBtnSend().addActionListener(e -> {
	            if (dos == null) return;
	            new Thread(() -> {
	                try {
	                    String expression = panelRow2.getUsdText();
	                    String result = calculateResult("USD", expression);
	                    
	                    SwingUtilities.invokeLater(() -> panelRow2.setUsdTextResult(result));
	                    
	                    dos.writeUTF("USD"); 
	                    dos.writeUTF(result); 
	                    dos.flush();
	                    System.out.println("Đã gửi kết quả USD cho Client: " + result);
	                } catch (Exception ex) { ex.printStackTrace(); }
	            }).start();
	        });
	    }
	    if (panelRow3 != null) {
	        panelRow3.getBtnSend().addActionListener(e -> {
	            if (dos == null) return;
	            new Thread(() -> {
	                try {
	                    String expression = panelRow3.getStringText();
	                    String result = calculateResult("REVERSE", expression);
	                    
	                    SwingUtilities.invokeLater(() -> panelRow3.setStringTextResult(result));
	                    
	                    dos.writeUTF("REVERSE"); 
	                    dos.writeUTF(result); 
	                    dos.flush();
	                    System.out.println("Đã gửi kết quả Chuỗi cho Client: " + result);
	                } catch (Exception ex) { ex.printStackTrace(); }
	            }).start();
	        });
	    }
	}
	
	
	
	public void setResultText(String text) {
		SwingUtilities.invokeLater(() -> {
            if (panelRow1 != null) {
                panelRow1.setPrefixTextResult(text);
            }
        });
	}
    
	private String calculateResult(String type, String expression) {
        if ("PREFIX".equals(type)) {
            try {
                res = evaluatePrefix(expression); 
                return "" + (int) res; 
            } catch (Exception e) {
                e.printStackTrace(); 
                return "Lỗi biểu thức: " + e.getMessage();
            }
        } else if ("USD".equals(type)) {
            try {
                double usd = Double.parseDouble(expression.trim());
                double vnd = usd * 25400; 
                return String.format("%,.0f VND", vnd); 
            } catch (NumberFormatException e) {
                return "Lỗi: Vui lòng nhập số hợp lệ";
            }
        } else if ("REVERSE".equals(type)) {
            if (expression == null || expression.isEmpty()) return "";
            return new StringBuilder(expression).reverse().toString();
        }
        return "0";
    }
    
    private int evaluatePrefix(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new RuntimeException("Chuỗi trống!");
        }
        String[] tokens = expression.trim().split("\\s+");
        Stack<Integer> stack = new Stack<>();

        try {
            for (int i = tokens.length - 1; i >= 0; i--) {
                String t = tokens[i];

                if (isNumber(t)) {
                    stack.push(Integer.parseInt(t));
                } else {
                    int o1 = stack.pop();
                    int o2 = stack.pop();

                    switch (t) {
                        case "+": stack.push(o1 + o2); break;
                        case "-": stack.push(o1 - o2); break;
                        case "*": stack.push(o1 * o2); break;
                        case "/": 
                            if (o2 == 0) throw new RuntimeException("Chia cho 0");
                            stack.push(o1 / o2); 
                            break;
                        default:
                            throw new RuntimeException("Ký tự lạ: " + t);
                    }
                }
            }
            if (stack.size() != 1) {
                throw new RuntimeException("Dư toán hạng");
            }
            return stack.pop();
            
        } catch (java.util.EmptyStackException e) {
            throw new RuntimeException("Sai cú pháp (thiếu số)"); 
        }
    }
    
    private boolean isNumber(String t) {
        if (t == null || t.isEmpty()) return false;
        
        int start = 0;
        if (t.charAt(0) == '-' && t.length() > 1) {
            start = 1;
        }
        
        for (int i = start; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
