package MainPackage;
import TCP.MainTCP;
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

public class Main extends JFrame {
	
	private static final Color primaryColor = new Color(93, 193, 242);
	private static final Color textColor = new Color(44, 62, 80);
	private static final Font textInformationFont = new Font("Times New Roman", Font.BOLD, 20);
	private static final Font textBtnFont = new Font("Times New Roman", Font.BOLD, 25);
	private static final Font textPage = new Font("Times New Roman", Font.ITALIC, 18);
	private static final long serialVersionUID = 1L;
	
	public Main() {
		TrangChu();
	}
	
	private void TrangChu(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Trang chủ");
		setBounds(150, 150, 650, 250);
		setLocationRelativeTo(null);
		
		loadPageIcon("image/logo_page.png");
		
		JPanel contentPane = new JPanel(new BorderLayout(15, 0));
		contentPane.setBorder(new EmptyBorder(40, 10, 20, 10));
		contentPane.setBackground(new Color(236, 240, 241));
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
		buttonPanel.setOpaque(false);
		JButton btnTCP = createButtonProtocol("TCP");
		JButton btnUDP = createButtonProtocol("UDP");
		buttonPanel.add(btnTCP);
		buttonPanel.add(btnUDP);
		
		JPanel informationPage = new JPanel();
		informationPage.setLayout(new BoxLayout(informationPage, BoxLayout.Y_AXIS));
		informationPage.setOpaque(false);
		informationPage.setBorder(new EmptyBorder(5, 0, 0, 0));
		
		JLabel labelPage = new JLabel("Chào mừng bạn đến với ứng dụng Socket Programming");
		labelPage.setFont(textPage);
		labelPage.setAlignmentX(Component.CENTER_ALIGNMENT);
		labelPage.setForeground(new Color(52, 73, 94));
		
		JPanel studentInfoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
		studentInfoPanel.setOpaque(false);
		studentInfoPanel.add(createLabelStudent("Nguyễn Thành Thắng - 52300256"));
		studentInfoPanel.add(createLabelStudent("Phạm Nhựt Huy - ....."));
		studentInfoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		informationPage.add(labelPage);
		informationPage.add(Box.createVerticalStrut(15));
		informationPage.add(studentInfoPanel);
		
		JPanel centerWrapper = new JPanel();
		centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
		centerWrapper.setOpaque(false);
		centerWrapper.add(buttonPanel);
		centerWrapper.add(Box.createVerticalStrut(5));
		centerWrapper.add(informationPage);
		
		contentPane.add(centerWrapper, BorderLayout.CENTER);		
		this.setContentPane(contentPane);
		this.setResizable(false);
		
		actionGoToPage(btnTCP);
	}
	
	private void loadPageIcon(String path) {
		try {
			File imageFile = new File(path);
			if ( imageFile.exists()) {
				this.setIconImage(ImageIO.read(imageFile));
			}
		}catch(Exception e) {
			System.err.println("Lỗi tải icon hoặc không thấy file icon" + e.getMessage());
		}
	}
	
	private JLabel createLabelStudent(String text) {
		JLabel label = new JLabel(text);
		label.setFont(textInformationFont);
		label.setForeground(textColor);
		return label;
	}
	
	private JButton createButtonProtocol(String text) {
		JButton btn = new JButton(text);
		btn.setFont(textBtnFont);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setBackground(primaryColor);
		btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
        return btn;
	}
	
	private void actionGoToPage(JButton button) {
		button.addActionListener(e -> {
			System.out.println("Đang chuyển sang nội dung TCP");
			new MainTCP(this).setVisible(true);
			this.setVisible(false);
		});
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
