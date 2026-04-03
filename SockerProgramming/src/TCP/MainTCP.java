package TCP;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;

import javax.swing.border.EmptyBorder;
import javax.swing.*;

import MainPackage.Main;

public class MainTCP extends JFrame {
	private static final long serialVersionUID = 1L;
	private JFrame mainPage;
	private JPanel contentPane;
	private JPanel sidebar;
	private CardLayout cardLayout;
	private JPanel contentArea;
	private static final Color contentAreaColor = new Color(250, 250, 249);
	private static final Color backColor = new Color(158, 158, 158);
	private static final Color primaryColor = new Color(93, 193, 242);
	private static final Color sidebarColor = new Color(236, 240, 241);
	private static final Font textBtnFont = new Font("Times New Roman", Font.BOLD, 20);
	
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
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(800, 500);
		this.setLocationRelativeTo(null);
		this.setTitle("TCP");
		loadPageIcon("image/logo_page.png");
		
		contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		
		sidebar = new JPanel();
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.setBackground(sidebarColor);
		sidebar.setPreferredSize(new Dimension(200, 0));
		sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));
		
		JButton btnTrangChu = createButtonTCP("Trang Chủ");
	    JButton btnChat = createButtonTCP("Chat TCP/IP");
	    JButton btnTransportFile = createButtonTCP("Truyền File");
	    JButton btnCalculate = createButtonTCP("Giải Toán");
	    JButton btnMiniGame = createButtonTCP("Mini Game");
	    JButton btnBack = createButtonBackTCP("Quay lại");
	    
	    cardLayout = new CardLayout();
	    contentArea = new JPanel();
	    contentArea.setLayout(cardLayout);
	    contentArea.setBackground(contentAreaColor);
	    
	    JPanelHome panelHome = new JPanelHome();
	    JPanelChat panelChat = new JPanelChat();
	    JPanelFile panelFile = new JPanelFile();
	    JPanelCalculate panelCalculate = new JPanelCalculate();
	    JPanelMiniGame panelMiniGame = new JPanelMiniGame();
	    
	    contentArea.add(panelHome, "TrangChu");
	    contentArea.add(panelChat, "Chat");
	    contentArea.add(panelFile, "File");
	    contentArea.add(panelCalculate, "Calculate");
	    contentArea.add(panelMiniGame, "MiniGame");
	    
	    btnTrangChu.addActionListener(e -> cardLayout.show(contentArea, "TrangChu"));
	    btnChat.addActionListener(e -> cardLayout.show(contentArea, "Chat"));
	    btnTransportFile.addActionListener(e -> cardLayout.show(contentArea, "File"));
	    btnCalculate.addActionListener(e -> cardLayout.show(contentArea, "Calculate"));
	    btnMiniGame.addActionListener(e -> cardLayout.show(contentArea, "MiniGame"));
	    
	    contentPane.add(sidebar, BorderLayout.WEST);
	    contentPane.add(contentArea, BorderLayout.CENTER);
	    this.setContentPane(contentPane);
		this.setResizable(false);
		
		actionGoToPage(btnBack);
	}
	
	private JButton createButtonTCP(String text) {
		JButton btn = new JButton(text);
		btn.setFont(textBtnFont);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setBackground(primaryColor);
		btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(160, 40));
        sidebar.add(btn);
	    sidebar.add(Box.createVerticalStrut(18));
        return btn;
	}
	
	private JButton createButtonBackTCP(String text) {
		JButton btn = new JButton(text);
		btn.setFont(textBtnFont);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setBackground(backColor);
		btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(160, 40));
        sidebar.add(Box.createVerticalStrut(18));
	    sidebar.add(btn);
        return btn;
	}
	
	private void actionGoToPage(JButton button) {
		button.addActionListener(e -> {
			if (this.mainPage != null) {
				System.out.println("Quay trở lại Trang chủ");
				this.mainPage.setVisible(true);
			}
			this.dispose();
		});
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
}
