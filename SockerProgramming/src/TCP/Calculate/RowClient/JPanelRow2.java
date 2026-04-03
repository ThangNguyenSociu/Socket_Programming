package TCP.Calculate.RowClient;

import java.awt.*;
import javax.swing.*;

public class JPanelRow2 extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel labelUSD;
    private JTextField txtUSD;
    private JButton send;
    
    public JPanelRow2() {
        Row();
    }
    
    private void Row() {
        labelUSD = new JLabel("Nhập USD:");
        txtUSD = new JTextField(25);
        send = new JButton("Gửi");

        labelUSD.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtUSD.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        txtUSD.setPreferredSize(new Dimension(300, 40));
        txtUSD.setMaximumSize(new Dimension(400, 40));
        txtUSD.putClientProperty("JComponent.roundRect", true);
        
        send.setPreferredSize(new Dimension(120, 40));
        send.putClientProperty("JButton.buttonType", "roundRect");
        send.setBackground(new Color(0, 123, 255)); 
        send.setForeground(Color.WHITE);
        send.setCursor(new Cursor(Cursor.HAND_CURSOR));

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.add(labelUSD);
        this.add(Box.createRigidArea(new Dimension(15, 0)));
        this.add(txtUSD);
        this.add(Box.createRigidArea(new Dimension(15, 0)));
        this.add(send);
    }
    
    public JButton getBtnSend() { return send; }
    public String getUsdText() { return txtUSD.getText().trim(); }
    public void setUsdText(String text) { this.txtUSD.setText(text); }
    public void clearTextField() { txtUSD.setText(""); txtUSD.requestFocus(); }
}