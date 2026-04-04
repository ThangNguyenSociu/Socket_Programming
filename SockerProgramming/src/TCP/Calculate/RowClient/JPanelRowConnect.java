package TCP.Calculate.RowClient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.formdev.flatlaf.FlatClientProperties;

public class JPanelRowConnect extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTextField txtIP;
    private JTextField txtPort;
    private JButton btnConnect;

	public JPanelRowConnect() {
		Connect();
	}

	private void Connect() {
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

	    txtIP = new JTextField("127.0.0.1");
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

	    txtPort = new JTextField("1234");
	    txtPort.setPreferredSize(new Dimension(80, 35));
	    txtPort.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
	    txtPort.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
	    gbc.gridx = 3;
	    gbc.weightx = 0.3;
	    this.add(txtPort, gbc);

	    btnConnect = new JButton("Kết nối Server");
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
	}

	public String getIP() {
        return txtIP.getText().trim();
    }

    public String getPort() {
        return txtPort.getText().trim();
    }

    public JButton getBtnConnect() {
        return btnConnect;
    }

}
