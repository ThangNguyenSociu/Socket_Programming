import com.formdev.flatlaf.FlatLightLaf;
//import java.awt.EventQueue;
import java.awt.FlowLayout;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.*;




public class Main extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JButton btnTCP;
	private JButton btnUDP;
	private JLabel label_SV1;
	private JLabel label_SV2;
	private JLabel label_Page;
	private JPanel Panel_SV;
	
	public Main() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Trang chủ");
		setBounds(100, 100, 600, 300);
		setLocationRelativeTo(null);
		try {
			File imageFile = new File("image/logo_page.png");
			if ( imageFile.exists()) {
				Image icon = ImageIO.read(imageFile);
				this.setIconImage(icon);
			}else {
				System.out.print("Không tìm thấy File");
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(30, 30, 30, 30));
		contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));
		
//		------

		btnTCP = new JButton("TCP");
		btnTCP.setFont(new Font("Segoe UI", Font.BOLD, 25));
//		btnTCP.setFont(new Font("Times New Roman", Font.BOLD, 25));
		btnTCP.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		btnTCP.putClientProperty("JButton.buttonType", "roundRect");
		btnTCP.putClientProperty("JButton.arc", 999);
		
		btnTCP.setBackground(new Color(52, 152, 219));
		btnTCP.setForeground(Color.WHITE);
		btnTCP.setFocusPainted(false);
//		------

		
//		------
		btnUDP = new JButton("UDP");
		btnUDP.setFont(new Font("Segoe UI", Font.BOLD, 25));
//		btnTCP.setFont(new Font("Times New Roman", Font.BOLD, 25));
		btnUDP.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		btnUDP.putClientProperty("JButton.buttonType", "roundRect");
		btnUDP.putClientProperty("JButton.arc", 999);
		
		btnUDP.setBackground(new Color(52, 152, 219));
		btnUDP.setForeground(Color.WHITE);
		btnUDP.setFocusPainted(false);
//		------
		
		label_Page = new JLabel("Chào mừng bạn đến với ứng dụng Socket Programming");
		label_Page.setFont(new Font("Times New Roman", Font.ITALIC, 16));
		label_Page.setHorizontalAlignment(SwingConstants.CENTER);
		label_Page.setForeground(new Color(52, 73, 94));
		label_Page.setBorder(new EmptyBorder(20, 0, 0, 0));
		
//		-----------
		JPanel Panel_SV = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
		Panel_SV.setOpaque(false); 
		
		label_SV1 = new JLabel("Nguyễn Thành Thắng - 52300256");
		label_SV1.setFont(new Font("Times New Roman", Font.BOLD, 20));
		label_SV1.setForeground(new Color(44, 62, 80));

		label_SV2 = new JLabel("Phạm Nhựt Huy - .....");
		label_SV2.setFont(new Font("Times New Roman", Font.BOLD, 20));
		label_SV2.setForeground(new Color(44, 62, 80));

		Panel_SV.add(label_SV1);
		Panel_SV.add(label_SV2);
		Panel_SV.setBorder(new EmptyBorder(30, 0, 0, 0));
				
		contentPane.setBackground(new Color(236, 240, 241));
		contentPane.add(btnUDP);
		contentPane.add(btnTCP);
		contentPane.add(label_Page, BorderLayout.SOUTH);
		contentPane.add(Panel_SV, BorderLayout.SOUTH);
		
		setContentPane(contentPane);
	}
	
	public static void main(String[] args) {
		try {
			FlatLightLaf.setup();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
				
		SwingUtilities.invokeLater(() -> {
			new Main().setVisible(true);
		});
	}

}
