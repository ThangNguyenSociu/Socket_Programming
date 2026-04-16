package TCP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;

public class JPanelHome extends JPanel {

	private static final long serialVersionUID = 1L;

	public JPanelHome() {
		System.out.println("Trang chủ");
		Home();
	}

	private void Home() {
	    this.setBackground(new Color(250, 250, 249));
	    this.setLayout(new BorderLayout());

	    JLabel labelTitle = new JLabel("GIỚI THIỆU TCP/IP", SwingConstants.CENTER);
	    labelTitle.setFont(new Font("Times New Roman", Font.BOLD, 30));
	    labelTitle.setForeground(new Color(52, 73, 94));
	    labelTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 15, 0));
	    add(labelTitle, BorderLayout.NORTH);

	    JPanel contentPanel = new JPanel();
	    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
	    contentPanel.setBackground(new Color(250, 250, 249));
	    contentPanel.setBorder(new EmptyBorder(0, 10, 20, 10));

	    contentPanel.add(createModernCard("Kiến trúc phân tầng (4 Layers)",
	    	    "Gồm 4 tầng độc lập (Application, Transport, Internet, Network Access), "
	    	    + "giúp tiêu chuẩn hóa giao tiếp và tối ưu quá trình khắc phục sự cố"));
    	contentPanel.add(Box.createVerticalStrut(10));

    	contentPanel.add(createModernCard("Định tuyến và Định danh (IP)",
    	    "Sử dụng địa chỉ IP làm định danh logic duy nhất, hỗ trợ bộ định tuyến (router) "
    	    + "tìm đường đi tối ưu cho các gói tin"));
    	contentPanel.add(Box.createVerticalStrut(10));

    	contentPanel.add(createModernCard("Chuyển mạch gói (Packet Switching)",
    	    "Phân mảnh dữ liệu thành các gói độc lập để truyền tải, giúp tối ưu băng thông "
    	    + "và tăng khả năng chịu lỗi của mạng"));
    	contentPanel.add(Box.createVerticalStrut(10));

    	contentPanel.add(createModernCard("Kết nối tin cậy (TCP Handshake)",
    	    "Thiết lập kết nối qua quá trình bắt tay 3 bước (SYN, SYN-ACK, ACK), "
    	    + "đảm bảo kiểm soát luồng và tính toàn vẹn dữ liệu"));
	    contentPanel.add(Box.createVerticalGlue());
	    add(contentPanel, BorderLayout.CENTER);
	}

	private JPanel createModernCard(String title, String description) {
	    JTextPane textPane = new JTextPane();
	    textPane.setContentType("text/html");

	    String text = "<html><b>" + title + "</b>: " + description + "</html>";
	    textPane.setText(text);

	    textPane.setEditable(false);
	    textPane.setFocusable(false);
	    textPane.setOpaque(false);
	    textPane.setFont(new Font("Segoe UI", Font.PLAIN, 15));
	    textPane.setForeground(new Color(44, 62, 80));

	    int cardWidth = 600;
	    int innerPaddingH = 20;
	    int textWidth = cardWidth - (innerPaddingH * 2);

	    textPane.setSize(new Dimension(textWidth, Integer.MAX_VALUE));
	    Dimension prefSize = textPane.getPreferredSize();
	    textPane.setPreferredSize(new Dimension(textWidth, prefSize.height));
	    textPane.setMinimumSize(new Dimension(textWidth, prefSize.height));
	    textPane.setMaximumSize(new Dimension(textWidth, prefSize.height));

	    JPanel card = new JPanel(new BorderLayout());

	    card.setBorder(BorderFactory.createEmptyBorder(15, innerPaddingH, 15, innerPaddingH));
	    card.setAlignmentX(Component.CENTER_ALIGNMENT);

	    card.putClientProperty(FlatClientProperties.STYLE,
	        "background: #ffffff; " +
	        "arc: 25; "
	    );
	    card.setOpaque(true);
	    card.add(textPane, BorderLayout.CENTER);

	    int cardHeight = prefSize.height + (15 * 2);
	    Dimension cardPrefSize = new Dimension(cardWidth, cardHeight);
	    card.setPreferredSize(cardPrefSize);
	    card.setMinimumSize(cardPrefSize);
	    card.setMaximumSize(cardPrefSize);
	    return card;
	}
}
