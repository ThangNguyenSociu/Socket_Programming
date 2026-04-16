package TCP.Calculate;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import TCP.Calculate.RowServer.JPanelRow1;
import TCP.Calculate.RowServer.JPanelRow2;
import TCP.Calculate.RowServer.JPanelRow3;
import TCP.Calculate.RowServer.JPanelRowConnect;

public class JPanelServer extends JPanel {
//    private ServerSocket serverSocket;
//    private Socket clientSocket;
//    private DataInputStream dis;
//    private DataOutputStream dos;
    private JPanelRow1 row1;
	private JPanelRow2 row2;
	private JPanelRow3 row3;
	private JPanelRowConnect rowConnect;
    private JTextArea txtLog;
    private JButton btnStart;
    private static final Color bgColor = new Color(236, 236, 233);
	private static final Color whiteColor = new Color(250, 250, 249);

    public JPanelServer() {
        initComponents();
    }

    private void initComponents() {
    	this.setLayout(new GridLayout(4, 1));

    	row1 = new JPanelRow1();
		row1.setBackground(bgColor);

		row2 = new JPanelRow2();
        row2.setBackground(whiteColor);


        row3 = new JPanelRow3();
		row3.setBackground(bgColor);

		rowConnect = new JPanelRowConnect(row1, row2, row3);
        this.add(rowConnect);
        this.add(row1);
        this.add(row2);
        this.add(row3);

    }
}
