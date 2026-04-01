package UDP;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import MainPackage.Main;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UDPMenu extends JFrame {

	private static final long serialVersionUID = 1L;

	// --- constants theo style Project ---
	private static final Color primaryColor = new Color(52, 152, 219); // Peter River
	private static final Color sidebarColor = new Color(44, 62, 80); // Midnight Blue
	private static final Color backgroundColor = new Color(236, 240, 241); // Clouds
	private static final Font menuFont = new Font("Times New Roman", Font.BOLD, 18);
	private static final Font titleFont = new Font("Times New Roman", Font.BOLD, 24);

	private JPanel contentArea;
	private CardLayout cardLayout;
	private JFrame parentFrame;

	// -- Demo Panels --
	private UDPWhiteboardPanel whiteboardPanel;
	private UDPReliableFilePanel reliableFilePanel;

	public UDPMenu(JFrame mainPage) {
		this.parentFrame = mainPage;
		initUI();

		// Luôn đóng tài nguyên khi tắt cửa sổ (Theo Hiến pháp)
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanup();
				if (parentFrame != null)
					parentFrame.setVisible(true);
			}
		});
	}

	private void cleanup() {
		if (whiteboardPanel != null) {
			whiteboardPanel.cleanup();
		}
		if (reliableFilePanel != null) {
			reliableFilePanel.cleanup();
		}
	}

	private void initUI() {
		setTitle("UDP Socket Laboratory");
		setSize(1000, 650);
		setLocationRelativeTo(null);

		JPanel mainPanel = new JPanel(new BorderLayout());

		// 1. SIDEBAR (Phần 3)
		JPanel sidebar = new JPanel();
		sidebar.setPreferredSize(new Dimension(300, 0));
		sidebar.setBackground(sidebarColor);
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.setBorder(new EmptyBorder(30, 20, 30, 20));

		JLabel lblTitle = new JLabel("UDP LAB");
		lblTitle.setForeground(Color.WHITE);
		lblTitle.setFont(titleFont);
		lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		sidebar.add(lblTitle);
		sidebar.add(Box.createVerticalStrut(50));

		// Các nút Menu
		JButton btnWhiteboard = createMenuButton("Whiteboard Sync");
		JButton btnReliableFile = createMenuButton("Reliable Transfer");
		JButton btnBroadcast = createMenuButton("Broadcast Chat");
		JButton btnBack = createMenuButton("Quay lại Menu");
		btnBack.setBackground(new Color(192, 57, 43)); // Màu đỏ Alizarin cho nút back

		sidebar.add(btnWhiteboard);
		sidebar.add(Box.createVerticalStrut(15));
		sidebar.add(btnReliableFile);
		sidebar.add(Box.createVerticalStrut(15));
		sidebar.add(btnBroadcast);
		sidebar.add(Box.createVerticalGlue());
		sidebar.add(btnBack);

		// 2. CONTENT AREA (Phần 7)
		cardLayout = new CardLayout();
		contentArea = new JPanel(cardLayout);
		contentArea.setBackground(backgroundColor);

		// Placeholder cho các Demo (Sẽ code chi tiết từng file sau)
		contentArea.add(createPlaceholder("Chào mừng bạn đến với UDP Lab. Chọn một chức năng bên trái!"), "HOME");

		whiteboardPanel = new UDPWhiteboardPanel();
		contentArea.add(whiteboardPanel, "WHITEBOARD");

		reliableFilePanel = new UDPReliableFilePanel();
		contentArea.add(reliableFilePanel, "FILE_TRANSFER");
		contentArea.add(createPlaceholder("Màn hình Broadcast Chat / Discovery"), "BROADCAST");

		// Action listeners
		btnWhiteboard.addActionListener(e -> cardLayout.show(contentArea, "WHITEBOARD"));
		btnReliableFile.addActionListener(e -> cardLayout.show(contentArea, "FILE_TRANSFER"));
		btnBroadcast.addActionListener(e -> cardLayout.show(contentArea, "BROADCAST"));
		btnBack.addActionListener(e -> {
			cleanup();
			this.dispose();
			if (parentFrame != null)
				parentFrame.setVisible(true);
		});

		mainPanel.add(sidebar, BorderLayout.WEST);
		mainPanel.add(contentArea, BorderLayout.CENTER);

		setContentPane(mainPanel);
	}

	private JButton createMenuButton(String text) {
		JButton btn = new JButton(text);
		btn.setFont(menuFont);
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		btn.setBackground(primaryColor);
		btn.setForeground(Color.WHITE);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setFocusPainted(false);
		btn.putClientProperty(FlatClientProperties.STYLE, "arc: 15"); // Bo góc nhẹ cho menu
		btn.setAlignmentX(Component.CENTER_ALIGNMENT);
		return btn;
	}

	private JPanel createPlaceholder(String text) {
		JPanel p = new JPanel(new GridBagLayout());
		p.setBackground(backgroundColor);
		JLabel l = new JLabel(text);
		l.setFont(new Font("Times New Roman", Font.ITALIC, 20));
		l.setForeground(sidebarColor);
		p.add(l);
		return p;
	}

	public static void main(String[] args) {
		FlatLightLaf.setup();
		EventQueue.invokeLater(() -> {
			new UDPMenu(null).setVisible(true);
		});
	}
}
