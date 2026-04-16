package UDP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class UDPReliableFilePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int PORT = 6666;
    private static final int CHUNK_SIZE = 8192;
    private static final Color primaryColor = new Color(0, 104, 255);
    private static final Color backColor = new Color(52, 73, 94);

    private DatagramSocket socket;
    private String physicalIP = "127.0.0.1";

    private JTextField txtSrcPort, txtDestIP, txtDestPort;
    private JProgressBar progress;
    private JTextArea logArea;
    private JButton btnSend, btnConnect, btnApplyConfig;
    private JLabel lblStatus, lblLocalIP;

    // Dispatcher Queues
    private final BlockingQueue<String> ackQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DatagramPacket> dataQueue = new LinkedBlockingQueue<>();

    public UDPReliableFilePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        physicalIP = getLocalPhysicalIP();
        initUI();
        startReceiver();
    }

    private String getLocalPhysicalIP() {
        try {
            NetworkInterface selectedNI = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String dN = ni.getDisplayName().toLowerCase();
                String n = ni.getName().toLowerCase();
                
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual())
                    continue;

                // LOẠI BỎ CÁC CARD MẠNG ẢO / VPN PHỔ BIẾN
                if (dN.contains("vmware") || n.contains("vmware") || dN.contains("virtual") || n.contains("virtual") || dN.contains("vbox") || n.contains("vbox")
                    || dN.contains("radmin") || n.contains("radmin") || dN.contains("hamachi") || n.contains("hamachi") || dN.contains("zerotier") || n.contains("zerotier")
                    || dN.contains("tunnel") || n.contains("tunnel") || dN.contains("teredo") || n.contains("teredo") || dN.contains("pseudo") || n.contains("pseudo")) {
                    continue;
                }

                // ƯU TIÊN WIFI / WLAN
                if (dN.contains("wi-fi") || dN.contains("wlan") || dN.contains("wireless") || dN.contains("802.11")) {
                    selectedNI = ni;
                    break;
                }

                if (selectedNI == null) {
                    selectedNI = ni;
                }
            }

            if (selectedNI != null) {
                for (InterfaceAddress ia : selectedNI.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address && ia.getBroadcast() != null) {
                        return ia.getAddress().getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
        }
        return "127.0.0.1";
    }

    private void initUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, javax.swing.BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBackground(Color.WHITE);

        // HÀNG THÔNG TIN IP CỤC BỘ
        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row0.setOpaque(false);
        lblLocalIP = new JLabel("● IP WiFi/LAN của bạn: " + physicalIP);
        lblLocalIP.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLocalIP.setForeground(primaryColor);
        row0.add(lblLocalIP);
        topPanel.add(row0);

        // HÀNG CẤU HÌNH MẠNG (UDP CONFIG)
        JPanel rowCfg = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowCfg.setOpaque(false);
        rowCfg.setBorder(new TitledBorder(null, "CẤU HÌNH MẠNG UDP", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(230, 126, 34)));

        rowCfg.add(new JLabel("Port nguồn (Lắng nghe):"));
        txtSrcPort = new JTextField(String.valueOf(PORT), 5);
        txtSrcPort.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rowCfg.add(txtSrcPort);

        rowCfg.add(new JLabel("   IP Đích:"));
        txtDestIP = new JTextField(physicalIP, 11);
        txtDestIP.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rowCfg.add(txtDestIP);

        rowCfg.add(new JLabel("   Port Đích:"));
        txtDestPort = new JTextField(String.valueOf(PORT), 5);
        txtDestPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rowCfg.add(txtDestPort);

        btnApplyConfig = new JButton("CẬP NHẬT CẤU HÌNH");
        btnApplyConfig.setBackground(new Color(230, 126, 34)); // Orange
        btnApplyConfig.setForeground(Color.WHITE);
        btnApplyConfig.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApplyConfig.addActionListener(e -> applyConfig());
        rowCfg.add(btnApplyConfig);
        topPanel.add(rowCfg);

        // Hàng điều khiển
        JPanel rowControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        rowControl.setOpaque(false);

        btnConnect = new JButton("THIẾT LẬP KẾT NỐI");
        styleButton(btnConnect, new Color(46, 204, 113)); // Emerald Green

        btnSend = new JButton("CHỌN & GỬI FILE");
        styleButton(btnSend, primaryColor);
        btnSend.setEnabled(false); // Chỉ bật sau khi connect thành công

        rowControl.add(btnConnect);
        rowControl.add(btnSend);

        lblStatus = new JLabel("Trạng thái: Chưa kết nối");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        rowControl.add(lblStatus);

        topPanel.add(rowControl);

        add(topPanel, BorderLayout.NORTH);

        // Khu vực Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(248, 249, 250));
        logArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(new TitledBorder("NHẬT KÝ HOẠT ĐỘNG"));
        add(scrollLog, BorderLayout.CENTER);

        // Progress bar
        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(0, 30));
        progress.setForeground(new Color(46, 204, 113));
        progress.setFont(new Font("Segoe UI", Font.BOLD, 12));
        add(progress, BorderLayout.SOUTH);

        // Listeners
        btnConnect.addActionListener(e -> new Thread(this::connectToPeer).start());
        btnSend.addActionListener(e -> selectAndSendFile());
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(180, 40));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void applyConfig() {
        try {
            int newSrcPort = Integer.parseInt(txtSrcPort.getText());
            physicalIP = getLocalPhysicalIP();
            lblLocalIP.setText("● IP WiFi/LAN của bạn: " + physicalIP);
            
            // Restart Socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(newSrcPort));
            
            log("--- CẬP NHẬT CẤU HÌNH ---");
            log("Lắng nghe tại port: " + newSrcPort);
            log("IP Đích: " + txtDestIP.getText() + ":" + txtDestPort.getText());
            
            btnSend.setEnabled(false);
            lblStatus.setText("Trạng thái: Đã cập nhật cấu hình");
            lblStatus.setForeground(Color.BLACK);
            
            JOptionPane.showMessageDialog(this, "Đã cập nhật cấu hình mạng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            log("Lỗi cập nhật cấu hình: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi: Port không hợp lệ hoặc đang bị sử dụng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            // Re-open on default if possible
            startReceiver();
        }
    }

    private void startReceiver() {
        new Thread(() -> {
            try {
                int currentPort = Integer.parseInt(txtSrcPort.getText());
                if (socket == null || socket.isClosed()) {
                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(currentPort));
                }
                log("Hệ thống UDP sẵn sàng tại cổng " + currentPort);

                while (socket != null && !socket.isClosed()) {
                    byte[] buf = new byte[CHUNK_SIZE + 100];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(0); 
                    try {
                        socket.receive(p);
                    } catch (java.net.SocketException se) {
                        break; // Socket closed externally
                    }

                    String msg = new String(p.getData(), 0, p.getLength());
                    
                    if (msg.equals("CONN_REQ")) {
                        log("Nhận yêu cầu kết nối từ " + p.getAddress().getHostAddress());
                        new Thread(() -> {
                            int choice = JOptionPane.showConfirmDialog(this, 
                                "Máy [" + p.getAddress().getHostAddress() + "] muốn thiết lập kết nối để gửi file.\nBạn có đồng ý không?", 
                                "Xác nhận kết nối", 
                                JOptionPane.YES_NO_OPTION, 
                                JOptionPane.QUESTION_MESSAGE);
                            
                            try {
                                if (choice == JOptionPane.YES_OPTION) {
                                    sendACK("CONN_ACK", p.getAddress(), p.getPort());
                                    log("Đã chấp nhận kết nối từ " + p.getAddress().getHostAddress());
                                } else {
                                    sendACK("CONN_REJ", p.getAddress(), p.getPort());
                                    log("Đã từ chối kết nối từ " + p.getAddress().getHostAddress());
                                }
                            } catch (Exception ex) {
                                log("Lỗi phản hồi handshake: " + ex.getMessage());
                            }
                        }).start();
                    } 
                    else if (msg.equals("CONN_ACK") || msg.equals("CONN_REJ") || msg.startsWith("ACK_")) {
                        ackQueue.offer(msg);
                    } 
                    else if (msg.startsWith("FILE_HEADER:")) {
                        // Chạy receiveFile trên thread riêng để không block dispatcher
                        new Thread(() -> receiveFile(msg, p.getAddress(), p.getPort())).start();
                    } 
                    else {
                        // Đây là chunk dữ liệu
                        byte[] data = new byte[p.getLength()];
                        System.arraycopy(p.getData(), 0, data, 0, p.getLength());
                        dataQueue.offer(new DatagramPacket(data, data.length, p.getAddress(), p.getPort()));
                    }
                }
            } catch (Exception e) {
                log("Receiver Loop dừng: " + e.getMessage());
            }
        }).start();
    }

    private void connectToPeer() {
        try {
            btnConnect.setEnabled(false);
            lblStatus.setText("Trạng thái: Đang thử kết nối...");
            InetAddress destAddr = InetAddress.getByName(txtDestIP.getText());
            int destPort = Integer.parseInt(txtDestPort.getText());

            log("Gửi yêu cầu kết nối tới " + destAddr.getHostAddress() + "...");
            ackQueue.clear(); // Xóa ACKs cũ
            sendPacket("CONN_REQ", destAddr, destPort);

            // Chờ phản hồi trong 10 giây (vì bên kia cần click đồng ý)
            String res = waitForACKExtended(10000); 
            if ("CONN_ACK".equals(res)) {
                log("KẾT NỐI THÀNH CÔNG!");
                lblStatus.setText("Trạng thái: Đã kết nối");
                lblStatus.setForeground(new Color(46, 204, 113));
                btnSend.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Kết nối với máy đối tác thành công! Bạn có thể gửi file ngay bây giờ.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else if ("CONN_REJ".equals(res)) {
                log("KẾT NỐI BỊ TỪ CHỐI.");
                lblStatus.setText("Trạng thái: Bị từ chối");
                lblStatus.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Máy đối tác đã từ chối yêu cầu kết nối.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            } else {
                log("Kết nối thất bại. Không nhận được phản hồi (Timeout).");
                lblStatus.setText("Trạng thái: Lỗi kết nối");
                lblStatus.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Không thể kết nối với máy đối tác. Vui lòng kiểm tra IP/Port và đảm bảo bên đối tác đã nhấn 'Đồng ý'.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log("Lỗi handshake: " + e.getMessage());
        } finally {
            btnConnect.setEnabled(true);
        }
    }

    private void receiveFile(String header, InetAddress senderAddr, int senderPort) {
        try {
            String[] parts = header.split(":");
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            int totalChunks = Integer.parseInt(parts[3]);

            log("Nhận yêu cầu: " + fileName + " (" + fileSize + " bytes)");
            sendACK("ACK_START", senderAddr, senderPort);

            String home = System.getProperty("user.home");
            File downloadDir = new File(home, "Downloads");
            if (!downloadDir.exists()) downloadDir.mkdirs();
            
            File file = new File(downloadDir, "received_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                int expectedChunk = 0;
                dataQueue.clear(); // Xóa dữ liệu cũ nếu có
                while (expectedChunk < totalChunks) {
                    // Lấy chunk từ queue với timeout
                    DatagramPacket p = dataQueue.poll(10, TimeUnit.SECONDS);
                    if (p == null) {
                        log("Lỗi: Quá thời gian chờ gói tin (Timeout).");
                        break;
                    }

                    // Đọc ID từ 4 byte đầu
                    int receivedChunkId = ((p.getData()[0] & 0xFF) << 24) |
                                          ((p.getData()[1] & 0xFF) << 16) |
                                          ((p.getData()[2] & 0xFF) << 8)  |
                                          ((p.getData()[3] & 0xFF));

                    if (receivedChunkId == expectedChunk) {
                        // Gói tin đúng thứ tự
                        byte[] chunkContent = new byte[p.getLength() - 4];
                        System.arraycopy(p.getData(), 4, chunkContent, 0, p.getLength() - 4);
                        fos.write(chunkContent);
                        
                        sendACK("ACK_" + expectedChunk, senderAddr, senderPort);
                        expectedChunk++;
                        updateProgress(expectedChunk, totalChunks);
                    } else if (receivedChunkId < expectedChunk) {
                        // Gói tin cũ (do ACK trước đó bị mất), gửi lại ACK để sender biết
                        log("Nhận lại Chunk cũ " + receivedChunkId + ", gửi lại ACK.");
                        sendACK("ACK_" + receivedChunkId, senderAddr, senderPort);
                    }
                }
            }
            log("Đã nhận file xong: " + file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Đã nhận file hoàn tất!");
        } catch (Exception e) {
            log("Lỗi nhận file: " + e.getMessage());
        }
    }

    private void selectAndSendFile() {
        JFileChooser jfc = new JFileChooser();
        if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            new Thread(() -> doSendFile(f)).start();
        }
    }

    private void doSendFile(File file) {
        try {
            btnSend.setEnabled(false);
            InetAddress destAddr = InetAddress.getByName(txtDestIP.getText());
            int destPort = Integer.parseInt(txtDestPort.getText());

            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

            log("Bắt đầu gửi " + file.getName() + " tới " + destAddr.getHostAddress());
            sendPacket("FILE_HEADER:" + file.getName() + ":" + fileSize + ":" + totalChunks, destAddr, destPort);

            // Chờ ACK của Header
            if (!waitForACK("ACK_START")) {
                log("Bên nhận không phản hồi Header.");
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] chunk = new byte[CHUNK_SIZE];
                for (int i = 0; i < totalChunks; i++) {
                    int read = fis.read(chunk);
                    boolean delivered = false;
                    int retry = 0;
                    while (!delivered && retry < 10) {
                        sendChunkWithID(i, chunk, read, destAddr, destPort);

                        if (waitForACK("ACK_" + i)) {
                            delivered = true;
                        } else {
                            retry++;
                            log("Gửi lại Chunk " + i + " (Lần " + retry + ")...");
                        }
                    }
                    if (!delivered) {
                        log("Lỗi: Không thể gửi Chunk " + i + " sau 10 lần thử.");
                        return;
                    }
                    updateProgress(i + 1, totalChunks);
                }
            }
            log("Đã gửi file thành công!");
        } catch (Exception e) {
            log("Lỗi gửi file: " + e.getMessage());
        } finally {
            btnSend.setEnabled(true);
        }
    }

    private void sendPacket(String msg, InetAddress addr, int port) throws Exception {
        byte[] d = msg.getBytes();
        socket.send(new DatagramPacket(d, d.length, addr, port));
    }

    private void sendChunkWithID(int id, byte[] data, int len, InetAddress addr, int port) throws Exception {
        byte[] packetData = new byte[len + 4];
        // Chèn ID vào 4 byte đầu
        packetData[0] = (byte) ((id >> 24) & 0xFF);
        packetData[1] = (byte) ((id >> 16) & 0xFF);
        packetData[2] = (byte) ((id >> 8) & 0xFF);
        packetData[3] = (byte) (id & 0xFF);
        System.arraycopy(data, 0, packetData, 4, len);
        socket.send(new DatagramPacket(packetData, packetData.length, addr, port));
    }

    private boolean waitForACK(String expected) {
        String res = waitForACKExtended(2500); // Mặc định 2.5s cho file chunks
        return expected.equals(res);
    }

    private String waitForACKExtended(int timeoutMs) {
        try {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < timeoutMs) {
                String ack = ackQueue.poll(500, TimeUnit.MILLISECONDS);
                if (ack != null) return ack; // Trả về mã ACK nhận được (thành công, từ chối, v.v.)
            }
            return "TIMEOUT";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private void sendACK(String ack, InetAddress addr, int port) throws Exception {
        byte[] d = ack.getBytes();
        socket.send(new DatagramPacket(d, d.length, addr, port));
    }

    private void updateProgress(int current, int total) {
        SwingUtilities.invokeLater(() -> progress.setValue((int) ((double) current / total * 100)));
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
        });
    }

    public void cleanup() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}
