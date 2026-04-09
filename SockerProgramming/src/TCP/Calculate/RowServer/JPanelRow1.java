package TCP.Calculate.RowServer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
	private JTextField txtResult;
	private JButton send;

	public JPanelRow1() {
		Row();
	}

	private void Row() {
		labelPrefix = new JLabel("Nhập Prefix:");
	    txtPrefix = new JTextField(25);
	    txtResult = new JTextField(25);
	    send = new JButton("Kết quả");

	    labelPrefix.setFont(new Font("Segoe UI", Font.BOLD, 18));
	    txtPrefix.setFont(new Font("Segoe UI", Font.PLAIN, 16));
	    txtResult.setFont(new Font("Segoe UI", Font.PLAIN, 16));

	    txtPrefix.setPreferredSize(new Dimension(300, 40));
	    txtPrefix.setMaximumSize(new Dimension(400, 40));
	    txtPrefix.putClientProperty("JComponent.roundRect", true);
	    txtPrefix.putClientProperty("JTextField.showClearButton", true);


	    txtResult.setPreferredSize(new Dimension(300, 40));
	    txtResult.setMaximumSize(new Dimension(400, 40));
	    txtResult.putClientProperty("JComponent.roundRect", true);
	    txtResult.putClientProperty("JTextField.showClearButton", true);

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
	    this.add(txtResult);
	    this.add(Box.createRigidArea(new Dimension(15, 0)));
	    this.add(send);
    }

    public JButton getBtnSend() {
        return send;
    }

    public String getPrefixText() {
        return txtPrefix.getText().trim();
    }

    public void setPrefixTextResult(String text) {
    		this.txtResult.setText(text);
    }

    public void clearTextField() {
        txtPrefix.setText("");
        txtPrefix.requestFocus();
    }

    public void setPrefixText(String text) {
        this.txtPrefix.setText(text);
    }

}
