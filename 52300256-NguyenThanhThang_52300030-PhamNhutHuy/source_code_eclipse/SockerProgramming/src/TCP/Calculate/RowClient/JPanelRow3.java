package TCP.Calculate.RowClient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JPanelRow3 extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel labelString;
    private JTextField txtString;
    private JButton send;

    public JPanelRow3() {
        Row();
    }

    private void Row() {
        labelString = new JLabel("Đảo Chuỗi:");
        txtString = new JTextField(25);
        send = new JButton("Gửi");

        labelString.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtString.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        txtString.setPreferredSize(new Dimension(300, 40));
        txtString.setMaximumSize(new Dimension(400, 40));
        txtString.putClientProperty("JComponent.roundRect", true);

        send.setPreferredSize(new Dimension(120, 40));
        send.putClientProperty("JButton.buttonType", "roundRect");
        send.setBackground(new Color(0, 123, 255));
        send.setForeground(Color.WHITE);
        send.setCursor(new Cursor(Cursor.HAND_CURSOR));

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.add(labelString);
        this.add(Box.createRigidArea(new Dimension(15, 0)));
        this.add(txtString);
        this.add(Box.createRigidArea(new Dimension(15, 0)));
        this.add(send);
    }

    public JButton getBtnSend() { return send; }
    public String getStringText() { return txtString.getText().trim(); }
    public void setStringText(String text) { this.txtString.setText(text); }
    public void clearTextField() { txtString.setText(""); txtString.requestFocus(); }
}