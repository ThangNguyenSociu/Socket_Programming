package UDP;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.formdev.flatlaf.FlatClientProperties;

public class UDPMenu extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final Color primaryColor = new Color(0, 104, 255); 
	private static final Color darkBg = new Color(248, 249, 250);
	private static final Color sidebarColor = Color.WHITE;
	private static final Color accentColor = new Color(230, 240, 255);
	private static final Font navFont = new Font("Segoe UI", Font.BOLD, 14);
	private static final Font brandFont = new Font("Segoe UI Semibold", Font.BOLD, 26);

	private JPanel contentArea;
	private CardLayout cardLayout;
	private JFrame parentFrame;

	private UDPWhiteboardPanel whiteboardPanel;
	private UDPReliableFilePanel reliableFilePanel;
	private UDPBroadcastChatPanel broadcastChatPanel;
	private UDPScreenMirrorPanel mirrorPanel;

	private String physicalIP = "127.0.0.1";

	public UDPMenu(JFrame mainPage) {
		this.parentFrame = mainPage;
		this.physicalIP = getLocalPhysicalIP();
		initUI();

		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanup();
                if (parentFrame != null) parentFrame.setVisible(true);
			}
		});
	}

	private String getLocalPhysicalIP() {
		try {
			NetworkInterface fallbackNI = null;
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if (ni.isLoopback() || !ni.isUp()) continue;
				String dN = ni.getDisplayName().toLowerCase();
				if (fallbackNI == null) fallbackNI = ni;
				if (!dN.contains("vmware") && !dN.contains("virtual") && !dN.contains("vbox")) {
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
						if (ia.getAddress() instanceof Inet4Address && ia.getBroadcast() != null) {
							return ia.getAddress().getHostAddress();
						}
					}
				}
			}
			if (fallbackNI != null) {
				for (InterfaceAddress ia : fallbackNI.getInterfaceAddresses()) {
					if (ia.getAddress() instanceof Inet4Address && ia.getBroadcast() != null)
						return ia.getAddress().getHostAddress();
				}
			}
		} catch (Exception e) {}
		return "127.0.0.1";
	}

	private void cleanup() {
		if (whiteboardPanel != null) whiteboardPanel.cleanup();
		if (reliableFilePanel != null) reliableFilePanel.cleanup();
		if (broadcastChatPanel != null) broadcastChatPanel.cleanup();
		if (mirrorPanel != null) mirrorPanel.cleanup();
	}

	private void initUI() {
		setTitle("UDP Socket Laboratory");
		setSize(1300, 850);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(darkBg);

		// 1. SIDEBAR DỌN DẸP & THOÁNG
		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setPreferredSize(new Dimension(320, 0));
		sidebar.setBackground(sidebarColor);
		sidebar.setBorder(new MatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

		// Top Area: Logo & IP
		JPanel topArea = new JPanel();
		topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
		topArea.setBackground(sidebarColor);
		topArea.setBorder(new EmptyBorder(40, 25, 30, 25));
		
		JLabel lblLogo = new JLabel("UDP Laboratory");
		lblLogo.setForeground(primaryColor);
		lblLogo.setFont(brandFont);
		lblLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
		topArea.add(lblLogo);
		
		topArea.add(Box.createVerticalStrut(10));
		JPanel ipBadge = new JPanel(new BorderLayout());
		ipBadge.setBackground(accentColor);
		ipBadge.setBorder(new EmptyBorder(6, 12, 6, 12));
		JLabel lblIP = new JLabel("IP: " + physicalIP);
		lblIP.setFont(new Font("Segoe UI", Font.BOLD, 12));
		lblIP.setForeground(primaryColor);
		ipBadge.add(lblIP);
		ipBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
		
		JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		badgeWrapper.setOpaque(false);
		badgeWrapper.add(ipBadge);
		badgeWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		topArea.add(badgeWrapper);
        sidebar.add(topArea, BorderLayout.NORTH);

		// Middle Area: Navigation
		JPanel navArea = new JPanel();
		navArea.setLayout(new BoxLayout(navArea, BoxLayout.Y_AXIS));
		navArea.setBackground(sidebarColor);
		navArea.setBorder(new EmptyBorder(0, 20, 0, 20));

		JLabel lblSection = new JLabel("CHỨC NĂNG HỆ THỐNG");
		lblSection.setFont(new Font("Segoe UI", Font.BOLD, 11));
		lblSection.setForeground(new Color(160, 160, 160));
		lblSection.setBorder(new EmptyBorder(0, 10, 15, 0));
		navArea.add(lblSection);

		JButton btnChat = createNavButton("Phòng Chat UDP", "Nhắn tin & Voice đa phương thức");
		JButton btnWhite = createNavButton("Bảng Trắng Sync", "Đồng bộ nét vẽ thời gian thực");
		JButton btnMirr = createNavButton("Trình Chiếu", "Chia sẻ màn hình tốc độ cao");
		JButton btnFile = createNavButton("Truyền File", "Giao thức truyền dữ liệu tin cậy");
		
		navArea.add(btnChat); navArea.add(Box.createVerticalStrut(15));
		navArea.add(btnWhite); navArea.add(Box.createVerticalStrut(15));
		navArea.add(btnMirr); navArea.add(Box.createVerticalStrut(15));
		navArea.add(btnFile);
		
		sidebar.add(navArea, BorderLayout.CENTER);

		// Footer Area
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBackground(sidebarColor);
		footer.setBorder(new EmptyBorder(20, 25, 40, 25));
		JButton btnBack = new JButton("Quay lại Menu Chính");
		btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
		btnBack.setBackground(new Color(245, 245, 245));
		btnBack.setForeground(new Color(120, 120, 120));
		btnBack.setPreferredSize(new Dimension(0, 48));
		btnBack.setFocusPainted(false);
		btnBack.putClientProperty(FlatClientProperties.STYLE, "arc: 15; borderWidth: 1; borderColor: #DDD");
		btnBack.addActionListener(e -> { cleanup(); this.dispose(); if (parentFrame != null) parentFrame.setVisible(true); });
		footer.add(btnBack);
		sidebar.add(footer, BorderLayout.SOUTH);

		// 2. CONTENT AREA
		cardLayout = new CardLayout();
		contentArea = new JPanel(cardLayout);
		contentArea.setBackground(darkBg);
		contentArea.add(createWelcomeScreen(), "HOME");

		whiteboardPanel = new UDPWhiteboardPanel();
		contentArea.add(whiteboardPanel, "WHITEBOARD");
		reliableFilePanel = new UDPReliableFilePanel();
		contentArea.add(reliableFilePanel, "FILE_TRANSFER");
		broadcastChatPanel = new UDPBroadcastChatPanel();
		contentArea.add(broadcastChatPanel, "BROADCAST");
		mirrorPanel = new UDPScreenMirrorPanel();
		contentArea.add(mirrorPanel, "MIRROR");

		btnWhite.addActionListener(e -> cardLayout.show(contentArea, "WHITEBOARD"));
		btnFile.addActionListener(e -> cardLayout.show(contentArea, "FILE_TRANSFER"));
		btnChat.addActionListener(e -> cardLayout.show(contentArea, "BROADCAST"));
		btnMirr.addActionListener(e -> cardLayout.show(contentArea, "MIRROR"));

		mainPanel.add(sidebar, BorderLayout.WEST);
		mainPanel.add(contentArea, BorderLayout.CENTER);
		setContentPane(mainPanel);
	}

	private JButton createNavButton(String title, String subtitle) {
		JButton btn = new JButton("<html><div style='padding-left: 10px; width: 220px;'><b style='font-size: 13px; color: #2c3e50;'>" + title + "</b><br><i style='font-size: 9px; color: #95a5a6;'>" + subtitle + "</i></div></html>");
		btn.setFont(navFont);
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
		btn.setPreferredSize(new Dimension(280, 65));
		btn.setBackground(Color.WHITE);
		btn.setForeground(new Color(50, 50, 50));
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setHorizontalAlignment(SwingConstants.LEFT);
		btn.setFocusPainted(false);
		btn.putClientProperty(FlatClientProperties.STYLE, "arc: 18; borderWidth: 1; borderColor: #EEE");
		
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(accentColor); btn.setBorder(BorderFactory.createLineBorder(primaryColor)); }
			public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(Color.WHITE); btn.setBorder(BorderFactory.createLineBorder(new Color(238, 238, 238))); }
		});
		return btn;
	}

	private JPanel createWelcomeScreen() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBackground(darkBg);
		JLabel l = new JLabel("<html><center><h1 style='color: #2c3e50; font-family: Segoe UI; font-size: 24px;'>Hệ thống Thực nghiệm Socket</h1>" +
				"<p style='color: #7f8c8d; font-size: 14px; font-family: Segoe UI;'>Hãy chọn một công cụ ở menu bên trái để bắt đầu buổi trình diễn.</p></center></html>");
		p.add(l);
		return p;
	}
}
