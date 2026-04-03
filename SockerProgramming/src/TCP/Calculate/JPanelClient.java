package TCP.Calculate;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import TCP.Calculate.RowClient.*;


public class JPanelClient extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final Color bgColor = new Color(236, 236, 233);
	private static final Color whiteColor = new Color(250, 250, 249);
	private JPanelRow1 row1;
	private JPanelRow2 row2;
	private JPanelRow3 row3;
	private DataOutputStream dos;
	private DataInputStream dis;
	private JPanelRowConnect rowConnect;
	
	public JPanelClient() {
		Client();
	}
	
	private void Client() {
		this.setLayout(new GridLayout(4, 1));
        
		rowConnect = new JPanelRowConnect();
        this.add(rowConnect); 
		
		row1 = new JPanelRow1();
		row1.setBackground(bgColor);
        this.add(row1); 
        
        row2 = new JPanelRow2();
        row2.setBackground(whiteColor);
        this.add(row2);
        
        row3 = new JPanelRow3();
		row3.setBackground(bgColor);
        this.add(row3);
        
        setupEvents();
	}
	
	private void setupEvents() {
		
		rowConnect.getBtnConnect().addActionListener(e -> {
	        connectToServer(); 
	    });
	    
	    row1.getBtnSend().addActionListener(e -> {
	        
	        String dataToSend = row1.getPrefixText();
	        
	        if (dataToSend.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Vui lòng nhập biểu thức Prefix!");
	            return;
	        }
	        
	        sendDataToServer("PREFIX", dataToSend);
	    });
	    
	    row2.getBtnSend().addActionListener(e -> {
            String dataToSend = row2.getUsdText();
            if (dataToSend.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số USD!");
                return;
            }
            sendDataToServer("USD", dataToSend); 
        });
	    
	    row3.getBtnSend().addActionListener(e -> {
	        String dataToSend = row3.getStringText();
	        if (dataToSend.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Vui lòng nhập chuỗi cần đảo!");
	            return;
	        }
	        sendDataToServer("REVERSE", dataToSend);
	    });
	}
	
	private void connectToServer() {
	    String ip = rowConnect.getIP();
	    String portStr = rowConnect.getPort();

	    if (ip.isEmpty() || portStr.isEmpty()) {
	        JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ IP và Port!");
	        return;
	    }

	    rowConnect.getBtnConnect().setEnabled(false);
	    rowConnect.getBtnConnect().setText("Đang kết nối...");

	    new Thread(() -> {
	        try {
	            int port = Integer.parseInt(portStr);
	            Socket socket = new Socket(ip, port);
	            
	            dos = new DataOutputStream(socket.getOutputStream());
	            dis = new DataInputStream(socket.getInputStream());

	            SwingUtilities.invokeLater(() -> {
	                JOptionPane.showMessageDialog(this, "Kết nối Server thành công!");
	                rowConnect.getBtnConnect().setText("Đã kết nối");
	            });

	            while (true) {
	            	String type = dis.readUTF();
	                String result = dis.readUTF(); 
	                SwingUtilities.invokeLater(() -> {
	                    if ("PREFIX".equals(type)) {
	                        row1.setPrefixText(result); 
	                    } else if ("USD".equals(type)) {
	                        row2.setUsdText(result);
	                    } else if ("REVERSE".equals(type)) {
	                        row3.setStringText(result);
	                    }
	                });
	            }

	        } catch (Exception ex) {
	            SwingUtilities.invokeLater(() -> {
	                JOptionPane.showMessageDialog(this, "Không thể kết nối: " + ex.getMessage());
	                rowConnect.getBtnConnect().setEnabled(true);
	                rowConnect.getBtnConnect().setText("Kết nối Server");
	            });
	        }
	    }).start();
	}
	
	private void sendDataToServer(String type, String data) {
	    if (dos == null) {
	        JOptionPane.showMessageDialog(this, "Bạn chưa kết nối tới Server!");
	        return;
	    }

	    new Thread(() -> {
	        try {
	        	dos.writeUTF(type); 
	            dos.writeUTF(data);
	            dos.flush();
	            
	            SwingUtilities.invokeLater(() -> {
	                row1.clearTextField();
	            });
	            
	        } catch (IOException ex) {
	            SwingUtilities.invokeLater(() -> {
	                JOptionPane.showMessageDialog(this, "Lỗi mất kết nối: " + ex.getMessage());
	            });
	        }
	    }).start();
	}
}


//row2.getBtnSend().addActionListener(e -> {
//
//String dataToSend = row2.getPostfixText();
//
//if (dataToSend.isEmpty()) {
//    JOptionPane.showMessageDialog(this, "Vui lòng nhập biểu thức Prefix!");
//    return;
//}
//
//sendDataToServer("POSTFIX", dataToSend);
//});
