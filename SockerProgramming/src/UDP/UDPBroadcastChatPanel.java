package UDP;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class UDPBroadcastChatPanel extends JPanel {
    private static final int PORT = 8888;
    private static final int AVATAR_DATA_SIZE = 64; 
    private DatagramSocket socket;
    private InetAddress broadcastAddress;
    private String nickname;
    private String localIP = "127.0.0.1";
    private String targetIP = "";
    private String targetNick = "Tất cả mọi người";
    private ImageIcon myAvatarIcon;
    private String myAvatarBase64 = "";

    private CardLayout rightCardLayout;
    private JPanel rightCards;
    private DefaultListModel<UserInfo> userListModel;
    private JList<Message> chatList;
    private JTextField msgInput, txtProfileNick;
    private JList<UserInfo> userList;
    private JLabel lblChatTarget, lblMyAvatarBtn, lblPreviewAvatar;

    private boolean isRunning = true;
    private Map<String, UserInfo> onlineUsers = new HashMap<>(); // key: IP
    private Map<String, DefaultListModel<Message>> chatHistories = new HashMap<>(); // key: IP ("" for Global)
    private Map<String, Boolean> hasNewMessage = new HashMap<>(); // Mark unread

    private static final Color zaloBlue = new Color(0, 104, 255);
    private static final Color zaloMyBubble = new Color(215, 235, 255);
    private static final Color zaloOtherBubble = Color.WHITE;
    private static final Color zaloBg = new Color(231, 235, 239);
    private static final Color searchGray = new Color(241, 242, 245);
    private static final Color sidebarColor = Color.WHITE;

    private TargetDataLine targetLine;
    private SourceDataLine activeVoiceLine = null;
    private boolean isRecording = false;
    private JPopupMenu emojiMenu;

    private Message replyTarget = null;
    private JPanel replyPanel;
    private JLabel lblReplyTxt;

    class UserInfo {
        String nick, ip, avatarBase64;
        ImageIcon avatarIcon;

        UserInfo(String n, String a, String i) {
            nick = n; ip = i; avatarBase64 = a;
            this.avatarIcon = decodeBase64ToIcon(a);
        }
    }

    class Message {
        String sender, content, time, replySender = "", replyContent = "";
        ImageIcon avatar;
        boolean isMe;
        byte[] voiceData;

        Message(String s, String c, String t, ImageIcon a, boolean m) {
            sender = s; content = c; time = t; avatar = a; isMe = m;
        }
    }

    public UDPBroadcastChatPanel() {
        nickname = "User_" + (new Random().nextInt(900) + 100);
        myAvatarIcon = createDefaultAvatar(nickname);
        myAvatarBase64 = encodeIconToBase64(myAvatarIcon);
        
        chatHistories.put("", new DefaultListModel<Message>());
        
        setLayout(new BorderLayout(0, 0));
        setBackground(zaloBg);
        initNetwork();
        initUI();
        
        chatList.setModel(chatHistories.get(""));
        
        startReceiver();
        onlineUsers.put(localIP, new UserInfo(nickname, myAvatarBase64, localIP));
        updateUserList();
        sendBroadcast("JOIN:" + nickname + ":" + myAvatarBase64);
        sendBroadcast("JOIN:" + nickname + ":" + myAvatarBase64);
    }

    private void initUI() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(280, 0));
        leftPanel.setBackground(sidebarColor);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 225, 225)));
        
        // TIÊU ĐỀ DANH SÁCH
        JLabel lblListTitle = new JLabel("  DANH SÁCH ONLINE");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblListTitle.setForeground(Color.GRAY);
        lblListTitle.setBorder(new EmptyBorder(15, 10, 10, 10));
        leftPanel.add(lblListTitle, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserListRenderer());
        userList.setFixedCellHeight(70);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    UserInfo s = userList.getSelectedValue();
                    if (s != null) {
                        targetIP = s.ip;
                        targetNick = s.nick;
                        lblChatTarget.setText("<html>💬 Chat Riêng với: <b style='color:#0068FF;'>" + targetNick + "</b></html>");
                        
                        if (!chatHistories.containsKey(targetIP)) chatHistories.put(targetIP, new DefaultListModel<Message>());
                        chatList.setModel(chatHistories.get(targetIP));
                        hasNewMessage.put(targetIP, false);
                        updateUserList();
                        
                        rightCardLayout.show(rightCards, "CHAT");
                    }
                }
            }
        });
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JButton btnReset = new JButton("Vào Sảnh Chung") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(zaloBlue);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
                super.paintComponent(g);
            }
        };
        btnReset.setPreferredSize(new Dimension(0, 48));
        btnReset.setBackground(zaloBlue);
        btnReset.setForeground(Color.WHITE);
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnReset.setBorderPainted(false);
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(e -> {
            targetIP = "";
            lblChatTarget.setText("🌍 Sảnh Chung (Tất cả mọi người)");
            chatList.setModel(chatHistories.get(""));
            rightCardLayout.show(rightCards, "CHAT");
        });
        leftPanel.add(btnReset, BorderLayout.SOUTH);

        rightCardLayout = new CardLayout();
        rightCards = new JPanel(rightCardLayout);
        rightCards.setBackground(zaloBg);
        
        JPanel chatView = new JPanel(new BorderLayout());
        chatView.setBackground(zaloBg);
        
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(Color.WHITE);
        chatHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 225, 225)));
        chatHeader.setPreferredSize(new Dimension(0, 60));
        
        lblChatTarget = new JLabel("🌍 Sảnh Chung (Tất cả mọi người)");
        lblChatTarget.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblChatTarget.setBorder(new EmptyBorder(0, 20, 0, 0));
        chatHeader.add(lblChatTarget, BorderLayout.WEST);
        
        lblMyAvatarBtn = new JLabel(nickname, getHighQualityIcon(myAvatarIcon, 35), SwingConstants.RIGHT);
        lblMyAvatarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMyAvatarBtn.setBorder(new EmptyBorder(0, 0, 0, 20));
        lblMyAvatarBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rightCardLayout.show(rightCards, "PROFILE");
            }
        });
        chatHeader.add(lblMyAvatarBtn, BorderLayout.EAST);
        chatView.add(chatHeader, BorderLayout.NORTH);

        chatList = new JList<>();
        chatList.setCellRenderer(new MessageBubbleRenderer());
        chatList.setBackground(zaloBg);
        chatList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = chatList.locationToIndex(e.getPoint());
                if (index < 0) return;
                Message m = ((DefaultListModel<Message>)chatList.getModel()).getElementAt(index);
                if (SwingUtilities.isRightMouseButton(e) || e.getClickCount() == 2) {
                    showMessageOptions(e, m);
                } else if (e.getClickCount() == 1 && m.voiceData != null) {
                    playVoice(m.voiceData);
                }
            }
        });
        chatList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = chatList.locationToIndex(e.getPoint());
                if (index >= 0) chatList.setCursor(new Cursor(Cursor.HAND_CURSOR));
                else chatList.setCursor(Cursor.getDefaultCursor());
            }
        });
        chatView.add(new JScrollPane(chatList), BorderLayout.CENTER);

        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBackground(Color.WHITE);
        inputContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 225, 225)));
        
        replyPanel = new JPanel(new BorderLayout(10, 0));
        replyPanel.setBackground(new Color(245, 245, 245));
        replyPanel.setBorder(new EmptyBorder(8, 20, 8, 20));
        replyPanel.setVisible(false);
        lblReplyTxt = new JLabel("Đang trả lời...");
        lblReplyTxt.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        JLabel btnC = new JLabel("✕");
        btnC.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnC.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cancelReply();
            }
        });
        replyPanel.add(lblReplyTxt, BorderLayout.CENTER);
        replyPanel.add(btnC, BorderLayout.EAST);
        inputContainer.add(replyPanel, BorderLayout.NORTH);

        JPanel middleBox = new JPanel(new BorderLayout());
        middleBox.setOpaque(false);
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        toolBar.setOpaque(false);
        JLabel lblE = new JLabel("😊");
        lblE.setFont(new Font("Dialog", Font.PLAIN, 22)); 
        lblE.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblE.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (emojiMenu != null && emojiMenu.isVisible()) emojiMenu.setVisible(false);
                else showEmojiMenu(lblE);
            }
        });
        JLabel lblM = new JLabel("🎤");
        lblM.setFont(new Font("Dialog", Font.PLAIN, 22)); 
        lblM.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblM.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startRecording();
                lblM.setForeground(Color.RED);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                stopRecording();
                lblM.setForeground(Color.BLACK);
            }
        });
        toolBar.add(lblE); toolBar.add(lblM);
        middleBox.add(toolBar, BorderLayout.NORTH);

        JPanel textRow = new JPanel(new BorderLayout(10, 0));
        textRow.setOpaque(false); textRow.setBorder(new EmptyBorder(0, 15, 15, 15));
        JPanel iW = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(searchGray);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        iW.setOpaque(false); iW.setBorder(new EmptyBorder(10, 18, 10, 18));
        msgInput = new JTextField();
        msgInput.setFont(new Font("Dialog", Font.PLAIN, 16)); 
        msgInput.setBackground(searchGray); msgInput.setBorder(null); msgInput.setOpaque(false);
        msgInput.addActionListener(e -> sendMessage());
        iW.add(msgInput, BorderLayout.CENTER);
        textRow.add(iW, BorderLayout.CENTER);
        
        JLabel btnS = new JLabel("GỬI");
        btnS.setFont(new Font("Arial", Font.BOLD, 15));
        btnS.setForeground(zaloBlue);
        btnS.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnS.setBorder(new EmptyBorder(0, 15, 0, 15));
        btnS.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendMessage();
            }
        });
        textRow.add(btnS, BorderLayout.EAST);
        middleBox.add(textRow, BorderLayout.SOUTH);
        inputContainer.add(middleBox, BorderLayout.CENTER);
        chatView.add(inputContainer, BorderLayout.SOUTH);

        JPanel profileView = new JPanel(new GridBagLayout());
        profileView.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        lblPreviewAvatar = new JLabel(getHighQualityIcon(myAvatarIcon, 120));
        lblPreviewAvatar.setBorder(BorderFactory.createLineBorder(zaloBlue, 2));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; profileView.add(lblPreviewAvatar, gbc);
        JButton btnU = new JButton("Đổi ảnh đại diện");
        btnU.addActionListener(e -> startCropper());
        gbc.gridy = 1; profileView.add(btnU, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; profileView.add(new JLabel("Tên:"), gbc);
        txtProfileNick = new JTextField(nickname, 15);
        gbc.gridx = 1; profileView.add(txtProfileNick, gbc);
        JButton btnSave = new JButton("LƯU THÔNG TIN");
        btnSave.setBackground(zaloBlue); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            nickname = txtProfileNick.getText().trim();
            lblMyAvatarBtn.setText(nickname);
            lblMyAvatarBtn.setIcon(getHighQualityIcon(myAvatarIcon, 35));
            onlineUsers.put(localIP, new UserInfo(nickname, myAvatarBase64, localIP));
            updateUserList();
            sendBroadcast("JOIN:" + nickname + ":" + myAvatarBase64);
            sendBroadcast("JOIN:" + nickname + ":" + myAvatarBase64);
            rightCardLayout.show(rightCards, "CHAT");
        });
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; profileView.add(btnSave, gbc);

        rightCards.add(chatView, "CHAT");
        rightCards.add(profileView, "PROFILE");
        add(leftPanel, BorderLayout.WEST);
        add(rightCards, BorderLayout.CENTER);
    }

    private void showMessageOptions(MouseEvent e, Message m) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem it = new JMenuItem("Trả lời (Reply)");
        it.addActionListener(al -> {
            replyTarget = m;
            lblReplyTxt.setText("<html>Đang trả lời <b>" + m.sender + "</b>: " + (m.voiceData!=null?"[Tin nhắn thoại]":m.content) + "</html>");
            replyPanel.setVisible(true);
            msgInput.requestFocus();
        });
        menu.add(it);
        menu.show(chatList, e.getX(), e.getY());
    }

    private synchronized void playVoice(byte[] d) {
        if (activeVoiceLine != null) { activeVoiceLine.stop(); activeVoiceLine.close(); }
        new Thread(() -> {
            try {
                AudioFormat f = new AudioFormat(8000.0f, 8, 1, true, false);
                activeVoiceLine = AudioSystem.getSourceDataLine(f);
                activeVoiceLine.open(f);
                activeVoiceLine.start();
                activeVoiceLine.write(d, 0, d.length);
                activeVoiceLine.drain();
                activeVoiceLine.close();
            } catch (Exception e) {}
        }).start();
    }

    private void showEmojiMenu(Component p) {
        emojiMenu = new JPopupMenu();
        String[] ems = {"😊", "😂", "❤️", "👍", "🔥", "🙏", "🎉", "💩", "😭", "😮"};
        JPanel pan = new JPanel(new GridLayout(2, 5, 5, 5));
        pan.setBackground(Color.WHITE);
        pan.setBorder(new EmptyBorder(5, 5, 5, 5));
        for (String s : ems) {
            JButton b = new JButton(s);
            b.setFont(new Font("Dialog", Font.PLAIN, 20));
            b.setBorderPainted(false); b.setContentAreaFilled(false);
            b.setFocusable(false);
            b.addActionListener(al -> {
                msgInput.setText(msgInput.getText() + s);
                msgInput.requestFocusInWindow();
            });
            pan.add(b);
        }
        emojiMenu.add(pan);
        emojiMenu.show(p, 0, -85);
    }

    private void sendMessage() {
        String content = msgInput.getText().trim(); if (content.isEmpty()) return;
        String time = new java.text.SimpleDateFormat("HH:mm").format(new Date());
        Message m = new Message(nickname, content, time, myAvatarIcon, true);
        if (replyTarget != null) {
            m.replySender = replyTarget.sender;
            m.replyContent = replyTarget.voiceData != null ? "🎤 [Tin nhắn thoại]" : replyTarget.content;
            content = "[RPLY|" + m.replySender + "|" + m.replyContent + "]" + content;
            cancelReply();
        }
        if (targetIP.isEmpty()) {
            sendBroadcast("MSG:" + m.sender + ":" + content); 
            chatHistories.get("").addElement(m); 
        } else {
            try { 
                sendTo(InetAddress.getByName(targetIP), "PVMSG:" + m.sender + ":" + content); 
                if (!chatHistories.containsKey(targetIP)) chatHistories.put(targetIP, new DefaultListModel<Message>());
                chatHistories.get(targetIP).addElement(m); 
            } catch (Exception ex) {} 
        }
        msgInput.setText("");
        chatList.ensureIndexIsVisible(((DefaultListModel)chatList.getModel()).size() - 1);
    }

    private void cancelReply() { replyTarget = null; replyPanel.setVisible(false); }
    
    private void startRecording() {
        new Thread(() -> {
            try {
                AudioFormat f = new AudioFormat(8000.0f, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, f);
                targetLine = (TargetDataLine) AudioSystem.getLine(info);
                targetLine.open(f); targetLine.start();
                isRecording = true;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                while (isRecording) {
                    int n = targetLine.read(data, 0, data.length);
                    out.write(data, 0, n);
                }
                byte[] audio = out.toByteArray();
                if (audio.length > 500) {
                    String b64 = Base64.getEncoder().encodeToString(audio);
                    if (targetIP.isEmpty()) {
                        sendBroadcast("VOICE:" + nickname + ":" + b64);
                        Message m = new Message(nickname, null, new java.text.SimpleDateFormat("HH:mm").format(new Date()), myAvatarIcon, true);
                        m.voiceData = audio; 
                        chatHistories.get("").addElement(m);
                    } else {
                        try {
                            sendTo(InetAddress.getByName(targetIP), "PVVOICE:" + nickname + ":" + b64);
                            Message m = new Message(nickname, null, new java.text.SimpleDateFormat("HH:mm").format(new Date()), myAvatarIcon, true);
                            m.voiceData = audio; 
                            if (!chatHistories.containsKey(targetIP)) chatHistories.put(targetIP, new DefaultListModel<Message>());
                            chatHistories.get(targetIP).addElement(m);
                        } catch(Exception ex) {}
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    private void stopRecording() { isRecording = false; if (targetLine!=null) { targetLine.stop(); targetLine.close(); } }

    class MessageBubbleRenderer implements ListCellRenderer<Message> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Message> list, Message m, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setBorder(new EmptyBorder(10, 15, 10, 15));
            JLabel avt = new JLabel(getHighQualityIcon(m.avatar, 38));
            JPanel bubble = new JPanel(new BorderLayout(5, 5)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(m.isMe ? zaloMyBubble : zaloOtherBubble);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
            };
            bubble.setBorder(new EmptyBorder(10, 12, 10, 12));
            JPanel contentP = new JPanel(new BorderLayout(0, 5)); contentP.setOpaque(false);
            if (!m.replySender.isEmpty()) {
                String quote = "<html><div style='background: #EAEBF1; border-left: 3px solid #0068ff; padding: 6px 10px; margin-bottom: 5px; font-family: Segoe UI;'>" +
                               "<b style='color: #444; font-size: 10px;'>" + m.replySender + "</b><br>" +
                               "<span style='color: #666; font-size: 10px;'>" + m.replyContent + "</span></div></html>";
                JLabel lblQ = new JLabel(quote); contentP.add(lblQ, BorderLayout.NORTH);
            }
            String t = m.voiceData != null ? "🎤 Tin nhắn thoại (Bấm nghe)" : m.content;
            JLabel lblM = new JLabel();
            if (m.voiceData != null) {
                lblM.setText("<html><b style='color: " + (m.isMe?"#0068ff":"gray") + "; font-size: 10px; font-family: Segoe UI;'>" + (targetIP.isEmpty() && !m.isMe ? m.sender : "") + "</b><br>🎤 Tin nhắn thoại (Bấm nghe)</html>");
            } else {
                String senderTag = (!targetIP.isEmpty() || m.isMe) ? "" : "<b style='color: gray; font-size: 10px; font-family: Segoe UI;'>" + m.sender + "</b><br>";
                lblM.setText("<html>" + senderTag + "<p style='width: 250px; font-family: Dialog, Arial, sans-serif;'>" + t + "</p></html>");
            }
            contentP.add(lblM, BorderLayout.CENTER); bubble.add(contentP, BorderLayout.CENTER);
            JLabel lblT = new JLabel(m.time); lblT.setFont(new Font("Segoe UI", Font.PLAIN, 9)); lblT.setForeground(Color.GRAY);
            JPanel bB = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); bB.setOpaque(false); bB.add(lblT); bubble.add(bB, BorderLayout.SOUTH);
            JPanel fC = new JPanel(new FlowLayout(m.isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 0)); fC.setOpaque(false);
            if (m.isMe) { fC.add(bubble); fC.add(avt); } else { fC.add(avt); fC.add(bubble); }
            p.add(fC, BorderLayout.CENTER); return p;
        }
    }

    private ImageIcon getHighQualityIcon(ImageIcon s, int z) {
        if (s == null) return null;
        BufferedImage bi = new BufferedImage(z, z, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(s.getImage(), 0, 0, z, z, null); g.dispose();
        return new ImageIcon(bi);
    }

    private void startCropper() {
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try { BufferedImage o = ImageIO.read(c.getSelectedFile()); showCropperDialog(o); } catch (Exception e) {}
        }
    }

    private void showCropperDialog(BufferedImage img) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Cắt ảnh đại diện", true);
        d.setLayout(new BorderLayout()); d.getContentPane().setBackground(new Color(30,30,30));
        CropperPanel cp = new CropperPanel(img); d.add(cp, BorderLayout.CENTER);
        JPanel ctrl = new JPanel(new BorderLayout(10, 10)); ctrl.setBackground(new Color(45, 45, 45));
        ctrl.setBorder(new EmptyBorder(15, 20, 15, 20));
        JSlider s = new JSlider(40, 500, 150); s.setBackground(new Color(45, 45, 45));
        s.addChangeListener(e -> cp.setCircleRadius(s.getValue()));
        JButton b = new JButton("XONG");
        b.addActionListener(e -> {
            BufferedImage c = cp.getCroppedImage();
            if (c != null) {
                myAvatarIcon = processToFinalAvatar(c);
                myAvatarBase64 = encodeIconToBase64(myAvatarIcon);
                lblPreviewAvatar.setIcon(getHighQualityIcon(myAvatarIcon, 120));
                d.dispose();
            }
        });
        ctrl.add(s, BorderLayout.CENTER); ctrl.add(b, BorderLayout.EAST);
        d.add(ctrl, BorderLayout.SOUTH); d.setSize(900, 750); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private ImageIcon processToFinalAvatar(BufferedImage cr) {
        BufferedImage f = new BufferedImage(AVATAR_DATA_SIZE, AVATAR_DATA_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = f.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, AVATAR_DATA_SIZE, AVATAR_DATA_SIZE));
        g.drawImage(cr, 0, 0, AVATAR_DATA_SIZE, AVATAR_DATA_SIZE, null); g.dispose();
        return new ImageIcon(f);
    }

    class CropperPanel extends JPanel {
        private BufferedImage image; private Point center = new Point(400, 300); private int radius = 75;
        private boolean drag = false; private Point lastPoint;
        public CropperPanel(BufferedImage img) {
            this.image = img;
            MouseAdapter a = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { if (e.getPoint().distance(center) <= radius) { drag = true; lastPoint = e.getPoint(); } }
                @Override public void mouseReleased(MouseEvent e) { drag = false; }
                @Override public void mouseDragged(MouseEvent e) { if (drag) { center.translate(e.getX()-lastPoint.x, e.getY()-lastPoint.y); lastPoint = e.getPoint(); repaint(); } }
            };
            addMouseListener(a); addMouseMotionListener(a);
        }
        public void setCircleRadius(int r) { this.radius = r/2; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
            double asp = (double) image.getWidth()/image.getHeight();
            int w = getWidth(), h = (int)(getWidth()/asp);
            if (h>getHeight()) { h=getHeight(); w=(int)(getHeight()*asp); }
            int ox=(getWidth()-w)/2, oy=(getHeight()-h)/2;
            g2.drawImage(image, ox, oy, w, h, null);
            java.awt.geom.Area over = new java.awt.geom.Area(new Rectangle(0,0,getWidth(),getHeight()));
            java.awt.geom.Ellipse2D.Double hole = new java.awt.geom.Ellipse2D.Double(center.x-radius, center.y-radius, radius*2, radius*2);
            over.subtract(new java.awt.geom.Area(hole));
            g2.setColor(new Color(0,0,0,180)); g2.fill(over); g2.setColor(Color.WHITE); g2.draw(hole);
        }
        public BufferedImage getCroppedImage() {
            try {
                double asp = (double) image.getWidth()/image.getHeight();
                int w = getWidth(), h = (int)(getWidth()/asp);
                if (h>getHeight()) { h=getHeight(); w=(int)(getHeight()*asp); }
                double sc = (double) image.getWidth()/w;
                int rx = (int)((center.x-radius-(getWidth()-w)/2)*sc), ry = (int)((center.y-radius-(getHeight()-h)/2)*sc), rs = (int)(radius*2*sc);
                return image.getSubimage(Math.max(0, rx), Math.max(0, ry), Math.min(rs, image.getWidth()-rx), Math.min(rs, image.getHeight()-ry));
            } catch (Exception e) { return null; }
        }
    }

    private String encodeIconToBase64(ImageIcon i) {
        try {
            BufferedImage bi = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics(); i.paintIcon(null, g, 0, 0); g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); ImageIO.write(bi, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) { return ""; }
    }

    private ImageIcon decodeBase64ToIcon(String b) {
        try { if (b==null || b.isEmpty()) return createDefaultAvatar("U"); return new ImageIcon(Base64.getDecoder().decode(b)); }
        catch (Exception e) { return createDefaultAvatar("U"); }
    }

    private void initNetwork() {
        try {
            socket = new DatagramSocket(null); socket.setReuseAddress(true); socket.bind(new InetSocketAddress(PORT)); socket.setBroadcast(true);
            
            NetworkInterface fallbackNI = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String dN = ni.getDisplayName().toLowerCase();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                
                if (fallbackNI == null) {
                    fallbackNI = ni;
                }
                
                if (!dN.contains("vmware") && !dN.contains("virtual") && !dN.contains("vbox") && !dN.contains("pseudo")) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        if (ia.getBroadcast() != null) { 
                            broadcastAddress = ia.getBroadcast(); 
                            localIP = ia.getAddress().getHostAddress(); 
                            break; 
                        }
                    }
                }
                if (broadcastAddress != null) {
                    break;
                }
            }
            
            if (broadcastAddress == null && fallbackNI != null) {
                for (InterfaceAddress ia : fallbackNI.getInterfaceAddresses()) {
                    if (ia.getBroadcast() != null) {
                        broadcastAddress = ia.getBroadcast();
                        localIP = ia.getAddress().getHostAddress();
                        break;
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void startReceiver() {
        new Thread(() -> {
            byte[] buf = new byte[65507];
            while (isRunning) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length); socket.receive(p);
                    processIncoming(new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8), p.getAddress());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void processIncoming(String mStr, InetAddress senderIP) {
        String ip = senderIP.getHostAddress(); if (ip.equals(localIP)) return;
        SwingUtilities.invokeLater(() -> {
            String time = new java.text.SimpleDateFormat("HH:mm").format(new Date());
            if (mStr.startsWith("JOIN:")) {
                String[] p = mStr.split(":");
                onlineUsers.put(ip, new UserInfo(p[1], p.length > 2 ? p[2] : "", ip));
                updateUserList();
                sendTo(senderIP, "ALIVE:" + nickname + ":" + myAvatarBase64);
            } else if (mStr.startsWith("ALIVE:")) {
                String[] p = mStr.split(":");
                onlineUsers.put(ip, new UserInfo(p[1], p.length > 2 ? p[2] : "", ip));
                updateUserList();
            } else if (mStr.startsWith("MSG:")) {
                String[] p = mStr.split(":", 3); UserInfo u = onlineUsers.get(ip); String content = p[2]; String rS = "", rC = "";
                if (content.startsWith("[RPLY|")) {
                    int i1 = content.indexOf("|", 6), i2 = content.indexOf("]");
                    rS = content.substring(6, i1); rC = content.substring(i1 + 1, i2); content = content.substring(i2 + 1);
                }
                Message m = new Message(p[1], content, time, (u!=null?u.avatarIcon:createDefaultAvatar(p[1])), false);
                m.replySender = rS; m.replyContent = rC;
                chatHistories.get("").addElement(m); 
                chatList.ensureIndexIsVisible(chatHistories.get("").size()-1);
            } else if (mStr.startsWith("PVMSG:")) {
                String[] p = mStr.split(":", 3); UserInfo u = onlineUsers.get(ip); String content = p[2];
                Message m = new Message(p[1], content, time, (u!=null?u.avatarIcon:createDefaultAvatar(p[1])), false);
                if (!chatHistories.containsKey(ip)) chatHistories.put(ip, new DefaultListModel<Message>());
                chatHistories.get(ip).addElement(m);
                if (!targetIP.equals(ip)) hasNewMessage.put(ip, true);
                updateUserList();
            } else if (mStr.startsWith("VOICE:")) {
                String[] p = mStr.split(":"); byte[] aud = Base64.getDecoder().decode(p[2]); UserInfo u = onlineUsers.get(ip);
                Message m = new Message(p[1], null, time, (u!=null?u.avatarIcon:createDefaultAvatar(p[1])), false);
                m.voiceData = aud; 
                chatHistories.get("").addElement(m); 
                chatList.ensureIndexIsVisible(chatHistories.get("").size()-1);
            } else if (mStr.startsWith("PVVOICE:")) {
                String[] p = mStr.split(":"); byte[] aud = Base64.getDecoder().decode(p[2]); UserInfo u = onlineUsers.get(ip);
                Message m = new Message(p[1], null, time, (u!=null?u.avatarIcon:createDefaultAvatar(p[1])), false);
                m.voiceData = aud;
                if (!chatHistories.containsKey(ip)) chatHistories.put(ip, new DefaultListModel<Message>());
                chatHistories.get(ip).addElement(m);
                if (!targetIP.equals(ip)) hasNewMessage.put(ip, true);
                updateUserList();
            } else if (mStr.startsWith("QUIT:")) { 
                onlineUsers.remove(ip); 
                chatHistories.remove(ip);
                updateUserList(); 
            }
        });
    }

    private void sendTo(InetAddress a, String m) { try { byte[] d = m.getBytes(StandardCharsets.UTF_8); socket.send(new DatagramPacket(d, d.length, a, PORT)); } catch (Exception e) {} }
    private void sendBroadcast(String m) { if (broadcastAddress == null) return; try { byte[] d = m.getBytes(StandardCharsets.UTF_8); socket.send(new DatagramPacket(d, d.length, broadcastAddress, PORT)); } catch (Exception e) {} }
    private void updateUserList() { userListModel.clear(); for (UserInfo u : onlineUsers.values()) userListModel.addElement(u); }
    public void cleanup() { isRunning = false; sendBroadcast("QUIT:" + nickname); if (socket!=null) socket.close(); }

    private static final Color[] VIBRANT_COLORS = { new Color(231, 76, 60), new Color(155, 89, 182), new Color(52, 152, 219), new Color(46, 204, 113), new Color(241, 196, 15), new Color(230, 126, 34) };
    private ImageIcon createDefaultAvatar(String n) {
        BufferedImage bi = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bgColor = VIBRANT_COLORS[Math.abs(n.hashCode()) % VIBRANT_COLORS.length];
        g2.setColor(bgColor); g2.fillOval(0, 0, 160, 160); String i = n.length() > 0 ? n.substring(0, 1).toUpperCase() : "U";
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 80));
        FontMetrics fm = g2.getFontMetrics(); int tx = (160 - fm.stringWidth(i)) / 2; int ty = ((160 - fm.getHeight()) / 2) + fm.getAscent();
        g2.setColor(new Color(0, 0, 0, 50)); g2.drawString(i, tx + 2, ty + 2); g2.setColor(Color.WHITE); g2.drawString(i, tx, ty); g2.dispose();
        return new ImageIcon(bi);
    }
    class UserListRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
            JPanel p = new JPanel(new BorderLayout(15, 0)); p.setOpaque(true); p.setBackground(s ? new Color(237, 247, 255) : Color.WHITE);
            p.setBorder(new EmptyBorder(10, 15, 10, 15));
            if (v instanceof UserInfo) {
                UserInfo u = (UserInfo)v; JLabel lblAvatar = new JLabel(getHighQualityIcon(u.avatarIcon, 48));
                JPanel txt = new JPanel(new GridLayout(2, 1)); txt.setOpaque(false);
                JLabel lblNick = new JLabel(u.nick + (hasNewMessage.getOrDefault(u.ip, false) ? " 🔴 [New]" : "")); 
                lblNick.setFont(new Font("Segoe UI", Font.BOLD, 14));
                if (hasNewMessage.getOrDefault(u.ip, false)) lblNick.setForeground(Color.RED);
                JLabel lblIP = new JLabel(u.ip); lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lblIP.setForeground(Color.GRAY);
                txt.add(lblNick); txt.add(lblIP); p.add(lblAvatar, BorderLayout.WEST); p.add(txt, BorderLayout.CENTER);
            }
            return p;
        }
    }
}
