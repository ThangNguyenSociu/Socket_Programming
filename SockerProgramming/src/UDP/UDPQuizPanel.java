package UDP;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * Demo UDP: Hệ thống trắc nghiệm trực tuyến (UDP Quiz Game)
 * - Một máy làm Giám khảo (Host) quản lý câu hỏi.
 * - Các máy khác làm Thí sinh (Player) tham gia trả lời.
 * - Demo tính năng gửi nhận gói tin nhanh, đa luồng.
 */
public class UDPQuizPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_PORT = 9997;

    private static final Color primaryColor = new Color(0, 104, 255);
    private static final Color successColor = new Color(40, 167, 69);
    private static final Color dangerColor = new Color(220, 53, 69);
    private static final Color warningColor = new Color(255, 193, 7);
    private static final Color bgSecondary = new Color(245, 247, 250);

    private DatagramSocket socket;
    private CardLayout mainCardLayout;
    private JPanel mainContainer;

    // Data
    private String myName = "Thí sinh " + (int) (Math.random() * 100);
    private List<Question> questions = new ArrayList<>();
    private int currentIdx = -1;
    private boolean isHost = false;
    private List<String> participants = new ArrayList<>();
    private Map<String, Integer> participantPorts = new HashMap<>();

    // UI Host
    private DefaultTableModel hostTableModel;
    private JLabel lblHostStatus;

    // UI Player
    private JPanel quizCard;
    private JLabel lblPlayerQuestion;
    private JButton[] btnOptions = new JButton[4];
    private JLabel lblPlayerStatus;

    static class Question {
        String content;
        String[] options;
        int correctIdx;

        Question(String c, String[] o, int correct) {
            this.content = c;
            this.options = o;
            this.correctIdx = correct;
        }
    }

    public UDPQuizPanel() {
        setLayout(new BorderLayout());
        setBackground(bgSecondary);

        initQuestions();
        initUI();
    }

    private void initQuestions() {
        questions.add(new Question("Giao thức UDP thuộc tầng nào trong mô hình OSI?",
                new String[] { "A. Tầng Vật lý", "B. Tầng Mạng", "C. Tầng Giao vận", "D. Tầng Ứng dụng" }, 2));
        questions.add(new Question("UDP là giao thức có đặc điểm gì?",
                new String[] { "A. Hướng kết nối", "B. Tin cậy tuyệt đối", "C. Không hướng kết nối", "D. Có bắt tay 3 bước" }, 2));
        questions.add(new Question("Cổng mặc định của dịch vụ DNS là bao nhiêu?",
                new String[] { "A. 80", "B. 53", "C. 443", "D. 21" }, 1));
        questions.add(new Question("Đơn vị dữ liệu ở tầng Giao vận (Transport) gọi là gì?",
                new String[] { "A. Frame", "B. Packet", "C. Segment/Datagram", "D. Bit" }, 2));
    }

    private void initUI() {
        mainCardLayout = new CardLayout();
        mainContainer = new JPanel(mainCardLayout);
        mainContainer.setOpaque(false);

        mainContainer.add(createRoleSelection(), "ROLES");
        mainContainer.add(createHostUI(), "HOST");
        mainContainer.add(createPlayerUI(), "PLAYER");

        add(mainContainer, BorderLayout.CENTER);
    }

    private JPanel createRoleSelection() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new EmptyBorder(40, 40, 40, 40));
        box.putClientProperty(FlatClientProperties.STYLE, "arc: 30");

        JLabel lbl = new JLabel("Chào mừng tới UDP Quiz");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(lbl);
        box.add(Box.createVerticalStrut(30));

        JButton btnHost = new JButton("Làm Giám Khảo (Host)");
        btnHost.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnHost.setPreferredSize(new Dimension(250, 50));
        btnHost.setBackground(primaryColor);
        btnHost.setForeground(Color.WHITE);
        btnHost.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHost.addActionListener(e -> startAsHost());
        box.add(btnHost);

        box.add(Box.createVerticalStrut(15));

        JButton btnPlayer = new JButton("Làm Thí Sinh (Join)");
        btnPlayer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlayer.setPreferredSize(new Dimension(250, 50));
        btnPlayer.setBackground(successColor);
        btnPlayer.setForeground(Color.WHITE);
        btnPlayer.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPlayer.addActionListener(e -> startAsPlayer());
        box.add(btnPlayer);

        p.add(box);
        return p;
    }

    private JPanel createHostUI() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        lblHostStatus = new JLabel("Bảng Điều Khiển Giám Khảo");
        lblHostStatus.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.add(lblHostStatus, BorderLayout.WEST);

        JButton btnReset = new JButton("Thoát/Reset");
        btnReset.addActionListener(e -> resetPanel());
        head.add(btnReset, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        // Control buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        controls.setOpaque(false);
        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            JButton b = new JButton("Gửi Câu " + (i + 1));
            b.addActionListener(e -> sendQuestion(idx));
            controls.add(b);
        }
        
        JButton btnShowAns = new JButton("Công Bố Đáp Án");
        btnShowAns.setBackground(warningColor);
        btnShowAns.addActionListener(e -> broadcastResult());
        controls.add(btnShowAns);
        
        JPanel topGroup = new JPanel(new BorderLayout());
        topGroup.setOpaque(false);
        topGroup.add(head, BorderLayout.NORTH);
        topGroup.add(controls, BorderLayout.CENTER);
        p.add(topGroup, BorderLayout.NORTH);

        // Result Table
        String[] cols = { "IP", "Thí Sinh", "Đáp Án", "Thời Gian", "Kết Quả" };
        hostTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(hostTableModel);
        table.setRowHeight(30);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        return p;
    }

    private JPanel createPlayerUI() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Connect bar
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setOpaque(false);
        JTextField txtIP = new JTextField("127.0.0.1", 12);
        JTextField txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 5);
        JButton btnJoin = new JButton("Tham Gia");
        btnJoin.addActionListener(e -> joinQuiz(txtIP.getText(), txtPort.getText()));
        bar.add(new JLabel("IP Giám Khảo:"));
        bar.add(txtIP);
        bar.add(new JLabel("Port:"));
        bar.add(txtPort);
        bar.add(btnJoin);
        p.add(bar, BorderLayout.NORTH);

        // Quiz Area
        quizCard = new JPanel();
        quizCard.setLayout(new BoxLayout(quizCard, BoxLayout.Y_AXIS));
        quizCard.setOpaque(false);
        quizCard.setVisible(false);

        lblPlayerQuestion = new JLabel("Đang đợi câu hỏi...");
        lblPlayerQuestion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblPlayerQuestion.setAlignmentX(Component.CENTER_ALIGNMENT);
        quizCard.add(Box.createVerticalStrut(30));
        quizCard.add(lblPlayerQuestion);
        quizCard.add(Box.createVerticalStrut(30));

        JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        optionsGrid.setOpaque(false);
        optionsGrid.setMaximumSize(new Dimension(800, 200));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btnOptions[i] = new JButton("Option " + i);
            btnOptions[i].setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnOptions[i].setPreferredSize(new Dimension(0, 60));
            btnOptions[i].addActionListener(e -> submitAnswer(idx));
            optionsGrid.add(btnOptions[i]);
        }
        quizCard.add(optionsGrid);
        
        lblPlayerStatus = new JLabel("Hãy chọn đáp án đúng nhất");
        lblPlayerStatus.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblPlayerStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        quizCard.add(Box.createVerticalStrut(20));
        quizCard.add(lblPlayerStatus);

        p.add(quizCard, BorderLayout.CENTER);

        return p;
    }

    private void startAsHost() {
        try {
            isHost = true;
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DEFAULT_PORT));
            new Thread(this::receiveLoop).start();
            mainCardLayout.show(mainContainer, "HOST");
            lblHostStatus.setText("Giám khảo đang lắng nghe tại cổng " + DEFAULT_PORT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private void startAsPlayer() {
        try {
            isHost = false;
            socket = new DatagramSocket(0); // Port ngẫu nhiên
            new Thread(this::receiveLoop).start();
            mainCardLayout.show(mainContainer, "PLAYER");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private void resetPanel() {
        if (socket != null) socket.close();
        mainCardLayout.show(mainContainer, "ROLES");
    }

    private void joinQuiz(String ip, String port) {
        try {
            String msg = "JOIN:" + myName;
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), Integer.parseInt(port)));
            JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu tham gia tới Giám khảo.");
        } catch (Exception e) {
        }
    }

    private void sendQuestion(int idx) {
        currentIdx = idx;
        // Thêm dòng ngăn cách để dễ theo dõi thay vì xoá sạch bảng
        SwingUtilities.invokeLater(() -> {
            hostTableModel.addRow(new Object[]{"ID: " + (idx + 1), "=== BẮT ĐẦU CÂU HỎI MỚI ===", "===", "===", "==="});
        });

        Question q = questions.get(idx);
        StringBuilder sb = new StringBuilder("QUIZ:");
        sb.append(idx).append("|").append(q.content);
        for (String opt : q.options) sb.append("|").append(opt);
        
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < participants.size(); i++) {
            try {
                String ip = participants.get(i);
                int port = participantPorts.get(ip);
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), port));
            } catch (Exception e) {}
        }
        lblHostStatus.setText("Đã gửi câu hỏi số " + (idx + 1));
    }

    private void broadcastResult() {
        if (currentIdx == -1) return;
        char correctChar = (char) ('A' + questions.get(currentIdx).correctIdx);
        
        // 1. Cập nhật bảng và gửi kết quả cá nhân hóa
        for (int i = 0; i < hostTableModel.getRowCount(); i++) {
            String ip = (String) hostTableModel.getValueAt(i, 0);
            String ans = (String) hostTableModel.getValueAt(i, 2);
            String status = (String) hostTableModel.getValueAt(i, 4);

            if (status != null && status.equals("Chờ...")) {
                boolean isCorrect = ans != null && ans.equals(String.valueOf(correctChar));
                String resStr = isCorrect ? "ĐÚNG" : "SAI";
                hostTableModel.setValueAt(resStr, i, 4);

                // Gửi phản hồi riêng cho từng thí sinh (Dùng port đã lưu)
                if (participantPorts.containsKey(ip)) {
                    try {
                        String msg = "RESULT:" + currentIdx + ":" + questions.get(currentIdx).correctIdx + ":" + resStr;
                        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), participantPorts.get(ip)));
                    } catch (Exception e) {}
                }
            }
        }
        
        // 2. Gửi một bản tin broadcast chung (Dự phòng cho máy chưa trả lời)
        String generalMsg = "RESULT:" + currentIdx + ":" + questions.get(currentIdx).correctIdx + ":WAIT";
        byte[] genData = generalMsg.getBytes(StandardCharsets.UTF_8);
        for (String ip : participants) {
            try {
                socket.send(new DatagramPacket(genData, genData.length, InetAddress.getByName(ip), participantPorts.get(ip)));
            } catch (Exception e) {}
        }
    }


    private long questionStartTime = 0;
    private InetAddress lastHostAddr;
    private int lastHostPort;

    private void receiveLoop() {
        byte[] buf = new byte[4096];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String senderIP = packet.getAddress().getHostAddress();
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (isHost) {
                    if (msg.startsWith("JOIN:")) {
                        String name = msg.substring(5);
                        if (!participants.contains(senderIP)) {
                            participants.add(senderIP);
                            participantPorts.put(senderIP, packet.getPort());
                        }
                    } else if (msg.startsWith("ANSW:")) {
                        String[] p = msg.split(":");
                        // ANSW:ID:Name:OptIdx:Time
                        SwingUtilities.invokeLater(() -> {
                            String res = "Chờ...";
                            hostTableModel.addRow(new Object[]{senderIP, p[2], p[3], p[4] + "ms", res});
                        });
                    }
                } else {
                    if (msg.startsWith("QUIZ:")) {
                        lastHostAddr = packet.getAddress();
                        lastHostPort = packet.getPort();
                        String[] p = msg.substring(5).split("\\|");
                        currentIdx = Integer.parseInt(p[0]);
                        questionStartTime = System.currentTimeMillis();
                        SwingUtilities.invokeLater(() -> {
                            quizCard.setVisible(true);
                            lblPlayerQuestion.setText("<html><center>" + p[1] + "</center></html>");
                            for (int i = 0; i < 4; i++) {
                                btnOptions[i].setText(p[i + 2]);
                                btnOptions[i].setEnabled(true);
                                btnOptions[i].setBackground(null);
                                btnOptions[i].setForeground(Color.BLACK); // Reset màu chữ về đen
                            }
                            lblPlayerStatus.setText("Hãy chọn đáp án cho câu " + (currentIdx + 1) + "!");
                            lblPlayerStatus.setForeground(Color.BLACK);
                        });
                    } else if (msg.startsWith("RESULT:")) {
                        String[] p = msg.split(":");
                        int correct = Integer.parseInt(p[2]);
                        String myStatus = p.length > 3 ? p[3] : "WAIT";
                        
                        SwingUtilities.invokeLater(() -> {
                            for(int i=0; i<4; i++) {
                                if (i == correct) {
                                    btnOptions[i].setBackground(successColor);
                                    btnOptions[i].setForeground(Color.WHITE);
                                } else {
                                    btnOptions[i].setEnabled(false);
                                }
                            }
                            if (myStatus.equals("ĐÚNG")) {
                                lblPlayerStatus.setText("Chúc mừng! Bạn đã trả lời ĐÚNG.");
                                lblPlayerStatus.setForeground(successColor);
                            } else if (myStatus.equals("SAI")) {
                                lblPlayerStatus.setText("Tiếc quá! Bạn đã trả lời SAI.");
                                lblPlayerStatus.setForeground(dangerColor);
                            } else {
                                lblPlayerStatus.setText("Hết giờ! Đáp án đúng là " + (char)('A' + correct));
                            }
                        });
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    private void submitAnswerToHost(int optIdx) {
        long time = System.currentTimeMillis() - questionStartTime;
        String msg = "ANSW:" + currentIdx + ":" + myName + ":" + (char)('A' + optIdx) + ":" + time;
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, lastHostAddr, lastHostPort));
            lblPlayerStatus.setText("Đã gửi đáp án! (" + time + "ms)");
        } catch (Exception e) {}
    }

    // Override submitAnswer to call the network one
    private void submitAnswer(int idx) {
        for (JButton b : btnOptions) b.setEnabled(false);
        btnOptions[idx].setBackground(primaryColor);
        btnOptions[idx].setForeground(Color.WHITE);
        submitAnswerToHost(idx);
    }
    
    public void cleanup() {
        if (socket != null) socket.close();
    }
}
