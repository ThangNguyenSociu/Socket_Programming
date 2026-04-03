package TCP.Calculate.RowClient;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JPanelRow1 extends JPanel {

	private static final long serialVersionUID = 1L;
	private JLabel labelPrefix;
	private JTextField txtPrefix;
	private JButton send;
	
	public JPanelRow1() {
		Row();
	}
	
	public static void main(String[] args) {
	    try {
	        com.formdev.flatlaf.FlatLightLaf.setup(); 
	        javax.swing.UIManager.put("Button.arc", 20);
	        javax.swing.UIManager.put("Component.arc", 20);
	        javax.swing.UIManager.put("TextComponent.arc", 20);
	        javax.swing.UIManager.put("JTextField.showClearButton", true);
	        
	    } catch (Exception ex) {
	        System.err.println("Không thể khởi động FlatLaf");
	    }
	}
	
	private void Row() {
		labelPrefix = new JLabel("Nhập Prefix:");
	    txtPrefix = new JTextField(25);
	    send = new JButton("Gửi");

	    labelPrefix.setFont(new Font("Segoe UI", Font.BOLD, 18));
	    txtPrefix.setFont(new Font("Segoe UI", Font.PLAIN, 16));
	    
	    txtPrefix.setPreferredSize(new Dimension(300, 40));
	    txtPrefix.setMaximumSize(new Dimension(400, 40));
	    txtPrefix.putClientProperty("JComponent.roundRect", true);
	    txtPrefix.putClientProperty("JTextField.showClearButton", true);
	    send.setPreferredSize(new Dimension(100, 40));
	    send.putClientProperty("JButton.buttonType", "roundRect");

	    send.setBackground(new Color(0, 123, 255));
	    send.setForeground(Color.WHITE);
	    send.setCursor(new Cursor(Cursor.HAND_CURSOR));

	    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	    this.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

	    this.add(labelPrefix);
	    this.add(Box.createRigidArea(new Dimension(15, 0)));
	    this.add(txtPrefix);
	    this.add(Box.createRigidArea(new Dimension(15, 0)));
	    this.add(send);
    }
    
    public JButton getBtnSend() {
        return send;
    }
    
    public String getPrefixText() {
        return txtPrefix.getText().trim();
    }
    
    public void setPrefixText(String text) {
        this.txtPrefix.setText(text);
    }
    
    public void clearTextField() {
        txtPrefix.setText("");
        txtPrefix.requestFocus();
    }

}
