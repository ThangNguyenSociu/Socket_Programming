package UDP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class UDPBroadcastChatPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PORT = 8888;
    private static final Color primaryColor = new Color(0, 104, 255);
    private static final Color bgSecondary = new Color(240, 242, 245);

    private DatagramSocket socket;
    private java.util.List<String> myLocalIPs = new java.util.ArrayList<>();
    private String lanPrefix = "";
    private String myNick = "User_" + (int) (Math.random() * 1000);
    private String myAvatarBase64 = "";

    private DefaultListModel<String> onlineModel = new DefaultListModel<>();
    private JList<String> onlineList;
    private Map<String, UserInfo> userMap = new HashMap<>();

    private JPanel chatLabelPanel;
    private JLabel lblCurrentChat;
    private String currentChatIP = "";
    private java.util.List<String> broadcastIPs = new java.util.ArrayList<>();

    private Map<String, DefaultListModel<Message>> chatHistories = new HashMap<>();
    private JTextField txtMsg;
    private JButton btnSend, btnVoice;

    private boolean isRecording = false;
    private TargetDataLine targetLine;
    private java.util.Set<String> processedMsgIds = new java.util.HashSet<>();

    static class UserInfo {
        String nick;
        String ip;
        ImageIcon avatar;
        boolean hasNewMsg = false;

        UserInfo(String n, String i, ImageIcon a) {
            this.nick = n;
            this.ip = i;
            this.avatar = a;
        }
    }

    static class Message {
        String sender;
        String content;
        long msgId;
        boolean isMe;
        boolean isVoice;
        byte[] audioData;

        Message(String s, String c, boolean m, boolean v, byte[] a) {
            this.sender = s;
            this.content = c;
            this.isMe = m;
            this.isVoice = v;
            this.audioData = a;
            this.msgId = System.currentTimeMillis() + (long)(Math.random() * 1000000);
        }
    }

    public UDPBroadcastChatPanel() {
        setLayout(new BorderLayout());
        myAvatarBase64 = MessageBubbleRenderer
                .imageToBase64(MessageBubbleRenderer.createDefaultAvatar(myNick.substring(0, 1)));
        initUI();
        initNetwork();
    }

    private void initUI() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(260, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        JPanel userHeader = new JPanel(new BorderLayout());
        userHeader.setBackground(Color.WHITE);
        userHeader.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel lblTitle = new JLabel("Đang Online");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        userHeader.add(lblTitle, BorderLayout.WEST);
        leftPanel.add(userHeader, BorderLayout.NORTH);

        onlineList = new JList<>(onlineModel);
        onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineList.setCellRenderer(new UserListRenderer(userMap));
        onlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = onlineList.getSelectedValue();
                if (selected != null) {
                    if (selected.equals("Sảnh Chung (Broadcast)")) {
                        switchChat("");
                    } else {
                        switchChat(selected);
                    }
                }
            }
        });

        onlineModel.addElement("Sảnh Chung (Broadcast)");
        leftPanel.add(new JScrollPane(onlineList), BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        chatLabelPanel = new JPanel(new BorderLayout());
        chatLabelPanel.setBackground(Color.WHITE);
        chatLabelPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        lblCurrentChat = new JLabel("Sảnh Chung (Broadcast)");
        lblCurrentChat.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatLabelPanel.add(lblCurrentChat, BorderLayout.WEST);
        rightPanel.add(chatLabelPanel, BorderLayout.NORTH);

        chatArea = new JList<>();
        chatArea.setCellRenderer(new MessageBubbleRenderer());
        chatArea.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatArea.setBackground(bgSecondary);

        chatArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int index = chatArea.locationToIndex(evt.getPoint());
                if (index >= 0) {
                    java.awt.Rectangle bounds = chatArea.getCellBounds(index, index);
                    if (bounds != null && bounds.contains(evt.getPoint())) {
                        Message msg = chatArea.getModel().getElementAt(index);
                        if (evt.getClickCount() == 2) {
                            if (!msg.isMe) {
                                txtMsg.setText("Trả lời " + msg.sender + ": ");
                                txtMsg.requestFocus();
                            }
                        } else if (evt.getClickCount() == 1) {
                            if (msg.isVoice && msg.audioData != null) {
                                playVoice(msg);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(null);
        rightPanel.add(scroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        txtMsg = new JTextField();
        txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSend = new JButton("Gửi");
        btnSend.setBackground(primaryColor);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);

        btnVoice = new JButton("🎤");
        btnVoice.setFont(new Font("Dialog", Font.PLAIN, 18));
        btnVoice.setFocusPainted(false);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(btnVoice);
        btnGroup.add(btnSend);

        inputPanel.add(txtMsg, BorderLayout.CENTER);
        inputPanel.add(btnGroup, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);

        btnSend.addActionListener(e -> sendMessage());
        txtMsg.addActionListener(e -> sendMessage());
        btnVoice.addActionListener(e -> toggleVoice());

        chatHistories.put("", new DefaultListModel<>());
        chatArea.setModel(chatHistories.get(""));
    }

    private void switchChat(String ip) {
        currentChatIP = ip;
        UserInfo ui = userMap.get(ip);
        if (ip.isEmpty()) {
            lblCurrentChat.setText("Sảnh Chung (Broadcast)");
        } else if (ui != null) {
            lblCurrentChat.setText("Đang chat với: " + ui.nick + " (" + ip + ")");
            ui.hasNewMsg = false;
            onlineList.repaint();
        }

        if (!chatHistories.containsKey(ip)) {
            chatHistories.put(ip, new DefaultListModel<>());
        }
        chatArea.setModel(chatHistories.get(ip));
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            int lastIndex = chatArea.getModel().getSize() - 1;
            if (lastIndex >= 0)
                chatArea.ensureIndexIsVisible(lastIndex);
        });
    }

    private void startReceiver() {
        new Thread(this::receiveLoop).start();
    }

    private String getLocalPhysicalIP() {
        String finalIP = "127.0.0.1";
        try {
            broadcastIPs.clear();
            myLocalIPs.clear();
            broadcastIPs.add("255.255.255.255");
            
            NetworkInterface selectedNI = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String dN = ni.getDisplayName().toLowerCase();
                String n = ni.getName().toLowerCase();
                
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                // LOẠI BỎ VPN / VIRTUAL CARDS
                if (dN.contains("vmware") || n.contains("vmware") || dN.contains("virtual") || n.contains("virtual") || dN.contains("vbox") || n.contains("vbox") 
                    || dN.contains("radmin") || n.contains("radmin") || dN.contains("hamachi") || n.contains("hamachi") || dN.contains("pseudo")) {
                    continue;
                }

                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address) {
                        String local = ia.getAddress().getHostAddress();
                        myLocalIPs.add(local);
                        System.out.println("  -> Bắt được IP nội bộ: " + local);
                        
                        if (ia.getBroadcast() != null) {
                            String bip = ia.getBroadcast().getHostAddress();
                            if (!broadcastIPs.contains(bip)) broadcastIPs.add(bip);
                        }
                    }
                }

                // ƯU TIÊN WIFI
                if (dN.contains("wi-fi") || dN.contains("wlan") || dN.contains("wireless") || dN.contains("802.11")) {
                    selectedNI = ni;
                    break;
                }
                if (selectedNI == null) selectedNI = ni;
            }
            
            if (selectedNI != null) {
                for (InterfaceAddress ia : selectedNI.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address) {
                        finalIP = ia.getAddress().getHostAddress();
                        int lastDot = finalIP.lastIndexOf('.');
                        if (lastDot > 0) lanPrefix = finalIP.substring(0, lastDot);
                        break;
                    }
                }
            }
        } catch (Exception e) {}
        return finalIP;
    }

    private void initNetwork() {
        try {
            getLocalPhysicalIP();
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);

            startReceiver();

            String joinMsg = "JOIN:" + myNick + ":" + myAvatarBase64;
            sendPacket(joinMsg, "BROADCAST");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi mạng: " + e.getMessage());
        }
    }

    private void receiveLoop() {
        byte[] buf = new byte[65507];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String senderIP = packet.getAddress().getHostAddress();
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                if (myLocalIPs.contains(senderIP)) {
                    System.out.println("[DEBUG-RECV] BỎ QUA GÓI TIN CỦA CHÍNH MÌNH TỪ IP: " + senderIP);
                    continue;
                }
                System.out.println("[DEBUG-RECV] CHẤP NHẬN GÓI TIN TỪ MÁY KHÁC: " + senderIP + " | Nội dung: "
                        + msg.substring(0, Math.min(msg.length(), 20)));

                processIncoming(msg, senderIP);
            } catch (Exception e) {
            }
        }
    }

    private void processIncoming(String msg, String ip) {
        String[] p = msg.split(":", 4);
        if (p.length < 2) return;
        String type = p[0];

        if (type.equals("JOIN")) {
            String nick = p[1];
            String avatarB64 = (p.length > 2) ? p[2] : "";
            addUser(ip, nick, avatarB64);
            sendPacket("ALIVE:" + myNick + ":" + myAvatarBase64, ip);
        } else if (type.equals("ALIVE")) {
            String nick = p[1];
            String avatarB64 = (p.length > 2) ? p[2] : "";
            addUser(ip, nick, avatarB64);
        } else if (type.equals("MSG") || type.equals("PVMSG") || type.equals("BVOICE") || type.equals("PVVOICE")) {
            if (p.length < 4) return;
            String mIdStr = p[1];
            String senderName = p[2];
            String contentOrB64 = p[3];
            
            // KIỂM TRA CHỐNG LẶP
            String uniqueKey = ip + "_" + mIdStr;
            if (processedMsgIds.contains(uniqueKey)) return;
            processedMsgIds.add(uniqueKey);

            if (type.equals("MSG")) {
                addMessage("", senderName, contentOrB64, false, false, null);
            } else if (type.equals("PVMSG")) {
                addMessage(ip, senderName, contentOrB64, false, false, null);
            } else if (type.equals("BVOICE")) {
                try {
                    byte[] audio = Base64.getDecoder().decode(contentOrB64);
                    addMessage("", senderName, "[▶ Click để nghe âm thanh]", false, true, audio);
                } catch (Exception e) {}
            } else if (type.equals("PVVOICE")) {
                try {
                    byte[] audio = Base64.getDecoder().decode(contentOrB64);
                    addMessage(ip, senderName, "[🔒▶ Click để nghe âm thanh]", false, true, audio);
                } catch (Exception e) {}
            }
        } else if (type.equals("QUIT")) {
            removeUser(ip);
        }
    }

    private void addUser(String ip, String nick, String avatarB64) {
        if (!userMap.containsKey(ip)) {
            ImageIcon icon = MessageBubbleRenderer.base64ToImage(avatarB64);
            userMap.put(ip, new UserInfo(nick, ip, icon));
            SwingUtilities.invokeLater(() -> {
                onlineModel.addElement(ip);
                if (!chatHistories.containsKey(ip))
                    chatHistories.put(ip, new DefaultListModel<>());
            });
        }
    }

    private void removeUser(String ip) {
        userMap.remove(ip);
        SwingUtilities.invokeLater(() -> onlineModel.removeElement(ip));
    }

    private void addMessage(String historyKey, String sender, String content, boolean isMe, boolean isVoice,
            byte[] audioData) {
        SwingUtilities.invokeLater(() -> {
            if (!chatHistories.containsKey(historyKey)) {
                chatHistories.put(historyKey, new DefaultListModel<>());
            }
            chatHistories.get(historyKey).addElement(new Message(sender, content, isMe, isVoice, audioData));

            if (!isMe && !currentChatIP.equals(historyKey)) {
                UserInfo ui = userMap.get(historyKey);
                if (ui != null)
                    ui.hasNewMsg = true;
                onlineList.repaint();
            }
            scrollToBottom();
        });
    }

    private void sendMessage() {
        String text = txtMsg.getText().trim();
        if (text.isEmpty())
            return;

        Message m = new Message(myNick, text, true, false, null);
        if (currentChatIP.isEmpty()) {
            sendPacket("MSG:" + m.msgId + ":" + myNick + ":" + text, "BROADCAST");
            addMessage("", myNick, text, true, false, null);
        } else {
            sendPacket("PVMSG:" + m.msgId + ":" + myNick + ":" + text, currentChatIP);
            addMessage(currentChatIP, myNick, text, true, false, null);
        }
        txtMsg.setText("");
    }

    private void toggleVoice() {
        if (!isRecording) {
            startRecording();
            btnVoice.setText("🔴");
            btnVoice.setBackground(Color.PINK);
        } else {
            stopRecording();
            btnVoice.setText("🎤");
            btnVoice.setBackground(null);
        }
        isRecording = !isRecording;
    }

    private void startRecording() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                targetLine = (TargetDataLine) AudioSystem.getLine(info);
                targetLine.open(format);
                targetLine.start();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                long start = System.currentTimeMillis();
                while (isRecording && (System.currentTimeMillis() - start < 5000)) {
                    int numRead = targetLine.read(data, 0, data.length);
                    out.write(data, 0, numRead);
                }
                targetLine.stop();
                targetLine.close();

                byte[] audioBytes = out.toByteArray();
                if (audioBytes.length > 60000) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Tin nhắn thoại quá dài! Vui lòng thu âm dưới 5 giây."));
                    return;
                }
                
                String base64 = Base64.getEncoder().encodeToString(audioBytes);
                Message m = new Message(myNick, null, true, true, audioBytes);
                
                if (currentChatIP.isEmpty()) {
                    sendPacket("BVOICE:" + m.msgId + ":" + myNick + ":" + base64, "BROADCAST");
                    addMessage("", myNick, "[▶ Click để nghe âm thanh (Bạn đã gửi)]", true, true, audioBytes);
                } else {
                    sendPacket("PVVOICE:" + m.msgId + ":" + myNick + ":" + base64, currentChatIP);
                    addMessage(currentChatIP, myNick, "[🔒▶ Click để nghe âm thanh riêng (Bạn đã gửi)]", true, true,
                            audioBytes);
                }

                SwingUtilities.invokeLater(() -> {
                    btnVoice.setText("🎤");
                    btnVoice.setBackground(null);
                    isRecording = false;
                });
            } catch (Exception e) {
            }
        }).start();
    }

    private void stopRecording() {
        isRecording = false;
    }

    private void playVoice(Message msg) {
        if (msg.content.contains("||")) return; // Đang phát thì bỏ qua
        new Thread(() -> {
            try {
                String originalContent = msg.content;
                msg.content = originalContent.replace("▶", "||");
                SwingUtilities.invokeLater(() -> chatArea.repaint());

                AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(msg.audioData, 0, msg.audioData.length);
                line.drain();
                line.close();

                msg.content = originalContent;
                SwingUtilities.invokeLater(() -> chatArea.repaint());
            } catch (Exception e) {
                // Nếu lỗi thì vẫn trả về icon cũ
                msg.content = msg.content.replace("||", "▶");
                SwingUtilities.invokeLater(() -> chatArea.repaint());
            }
        }).start();
    }

    public void cleanup() {
        if (socket != null && !socket.isClosed()) {
            sendPacket("QUIT:" + myNick, "BROADCAST");
            socket.close();
        }
    }

    private void sendPacket(String msg, String ip) {
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            if (ip.equals("BROADCAST")) {
                // 1. Unicast Sweep theo ý tưởng của Host (Tuyệt chiêu xuyên thủng
                // Firewall/Router Wi-Fi)
                if (!lanPrefix.isEmpty()) {
                    for (int i = 1; i <= 254; i++) {
                        String targetIP = lanPrefix + "." + i;
                        if (myLocalIPs.contains(targetIP))
                            continue;
                        try {
                            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(targetIP), PORT));
                        } catch (Exception ex) {
                        }
                    }
                    System.out.println("[DEBUG-SEND] Đã hoàn tất Unicast Sweep 254 máy dải: " + lanPrefix + ".xxx");
                }

                // 2. Gửi Broadcast dự phòng
                for (String bip : broadcastIPs) {
                    try {
                        socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(bip), PORT));
                        System.out.println("[DEBUG-SEND] Đã gửi Broadcast tới: " + bip);
                    } catch (Exception ex) {
                        System.out.println("[DEBUG-SEND] Lỗi gửi Broadcast tới " + bip + ": " + ex.getMessage());
                    }
                }
            } else {
                DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), PORT);
                socket.send(p);
                System.out.println("[DEBUG-SEND] Đã gửi Direct tới: " + ip);
            }
        } catch (Exception e) {
        }
    }
}

// ======================== UI RENDERERS & DATA MODELS ========================

class UserListRenderer extends JPanel implements javax.swing.ListCellRenderer<String> {
    private Map<String, UDPBroadcastChatPanel.UserInfo> userMap;
    private JLabel lblNick = new JLabel();
    private JLabel lblAvatar = new JLabel();
    private JLabel lblStatus = new JLabel();

    public UserListRenderer(Map<String, UDPBroadcastChatPanel.UserInfo> map) {
        this.userMap = map;
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(8, 10, 8, 10));
        add(lblAvatar, BorderLayout.WEST);

        JPanel center = new JPanel(new GridLayout(2, 1));
        center.setOpaque(false);
        center.add(lblNick);
        center.add(lblStatus);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {
        if (value.equals("Sảnh Chung (Broadcast)")) {
            lblNick.setText("Sảnh Chung");
            lblNick.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblStatus.setText("Tất cả mọi người");
            lblAvatar.setIcon(null);
        } else {
            UDPBroadcastChatPanel.UserInfo ui = userMap.get(value);
            if (ui != null) {
                lblNick.setText(ui.nick + (ui.hasNewMsg ? " 🔴" : ""));
                lblNick.setFont(new Font("Segoe UI", ui.hasNewMsg ? Font.BOLD : Font.PLAIN, 14));
                lblStatus.setText(value);
                lblAvatar.setIcon(ui.avatar);
            }
        }
        setBackground(isSelected ? new Color(230, 240, 255) : Color.WHITE);
        return this;
    }
}

class MessageBubbleRenderer extends JPanel implements javax.swing.ListCellRenderer<UDPBroadcastChatPanel.Message> {
    private JLabel lblContent = new JLabel();
    private JLabel lblSender = new JLabel();

    public MessageBubbleRenderer() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));

        lblContent.setOpaque(true);
        lblContent.setBorder(new EmptyBorder(8, 12, 8, 12));
        lblSender.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSender.setForeground(Color.GRAY);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends UDPBroadcastChatPanel.Message> list,
            UDPBroadcastChatPanel.Message msg, int index, boolean isSelected, boolean cellHasFocus) {
        removeAll();
        lblContent.setText("<html><p style='width: 200px;'>" + msg.content + "</p></html>");
        lblSender.setText(msg.sender);

        if (msg.isMe) {
            lblContent.setBackground(new Color(0, 104, 255));
            lblContent.setForeground(Color.WHITE);
            add(lblContent, BorderLayout.EAST);
        } else {
            lblContent.setBackground(Color.WHITE);
            lblContent.setForeground(Color.BLACK);
            JPanel leftAlign = new JPanel(new BorderLayout());
            leftAlign.setOpaque(false);
            leftAlign.add(lblSender, BorderLayout.NORTH);
            leftAlign.add(lblContent, BorderLayout.CENTER);
            add(leftAlign, BorderLayout.WEST);
        }
        return this;
    }

    public static ImageIcon createDefaultAvatar(String text) {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 104, 255));
        g2.fillOval(0, 0, 40, 40);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString(text, 12, 28);
        g2.dispose();
        return new ImageIcon(img);
    }

    public static String imageToBase64(ImageIcon icon) {
        try {
            BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = bi.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ImageIcon base64ToImage(String b64) {
        try {
            if (b64 == null || b64.isEmpty())
                return createDefaultAvatar("U");
            byte[] bytes = Base64.getDecoder().decode(b64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            Image scaled = img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return createDefaultAvatar("U");
        }
    }
}
