package TCP;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.FlowLayout;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.*;

import MainPackage.Main;

public class MainTCP extends JFrame {
	private JFrame mainPage;
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	public MainTCP(JFrame jframe) {
		TCP();
		this.mainPage = jframe;
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				mainPage.setVisible(true);
			}
		});
	}
	
	private void TCP() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

	}
}
