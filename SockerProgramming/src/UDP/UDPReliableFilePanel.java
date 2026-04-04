package UDP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

public class UDPReliableFilePanel extends JPanel {
    private static final int CHUNK_SIZE = 1024;
    private static final int PORT = 9999;
    private static final int TIMEOUT = 500;
    private static final Color primaryColor = new Color(93, 193, 242);
    private static final Color backColor = new Color(158, 158, 158);

    private JTextArea logArea;
    private JProgressBar progressBar;
    private JTextField txtDestIP, txtDestPort, txtMyPort;
    private JCheckBox chkSimulateLoss;
    private JButton btnSend, btnOpenFolder, btnOpenFile, btnSaveAs;
    private boolean isReceiving = true;
    private DatagramSocket serverSocket;
    private Random random = new Random();
    private File lastReceivedFile;
    private String physicalIP = "127.0.0.1";

    public UDPReliableFilePanel() {
        physicalIP = getLocalPhysicalIP();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(236, 240, 241));
        initUI();
        startReceiver();
    }

    private String getLocalPhysicalIP() {
        try {
            NetworkInterface fallbackNI = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String dN = ni.getDisplayName().toLowerCase();
                if (ni.isLoopback() || !ni.isUp()) continue;
                if (fallbackNI == null) fallbackNI = ni;
                
                if (!dN.contains("vmware") && !dN.contains("virtual") && !dN.contains("vbox") && !dN.contains("pseudo")) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        if (ia.getAddress() instanceof Inet4Address && ia.getBroadcast() != null) {
                            return ia.getAddress().getHostAddress();
                        }
                    }
                }
            }
            if (fallbackNI != null) {
                for (InterfaceAddress ia : fallbackNI.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address && ia.getBroadcast() != null) {
                        return ia.getAddress().getHostAddress();
                    }
                }
            }
        } catch (Exception e) { }
        return "127.0.0.1";
    }

    private void initUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row0.setOpaque(false);
        JLabel lblInfo = new JLabel("Địa chỉ IP vật lý của bạn (WiFi/4G): " + physicalIP);
        lblInfo.setFont(new Font("Arial", Font.BOLD, 13));
        lblInfo.setForeground(new Color(0, 104, 255));
        row0.add(lblInfo);
        topPanel.add(row0);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setOpaque(false);
        row1.add(new JLabel("Cổng của tôi (Lắng nghe):"));
        txtMyPort = new JTextField(String.valueOf(PORT), 5);
        row1.add(txtMyPort);
        JButton btnListen = new JButton("Đổi Cổng");
        btnListen.setBackground(backColor);
        btnListen.setForeground(Color.WHITE);
        btnListen.addActionListener(e -> restartReceiver());
        row1.add(btnListen);
        row1.add(new JLabel("   |   "));
        chkSimulateLoss = new JCheckBox("Giả lập mất 15% gói (Demo)");
        chkSimulateLoss.setOpaque(false);
        row1.add(chkSimulateLoss);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setOpaque(false);
        row2.add(new JLabel("Đích IP (Máy nhận):"));
        txtDestIP = new JTextField(physicalIP, 12);
        row2.add(txtDestIP);
        row2.add(new JLabel("Đích Port:"));
        txtDestPort = new JTextField(String.valueOf(PORT), 7);
        row2.add(txtDestPort);
        topPanel.add(row1);
        topPanel.add(row2);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.setOpaque(false);
        btnSend = new JButton("Gửi File (SENDER)");
        btnSend.setFont(new Font("Arial", Font.BOLD, 14));
        btnSend.setBackground(primaryColor);
        btnSend.setForeground(Color.WHITE);
        btnSend.addActionListener(e -> selectAndSendFile());

        btnOpenFolder = new JButton("Mở thư mục tạm");
        btnOpenFile = new JButton("Mở File");
        btnSaveAs = new JButton("Lưu thành... (Download)");
        btnSaveAs.setBackground(new Color(46, 204, 113));
        btnSaveAs.setForeground(Color.WHITE);
        btnSaveAs.setEnabled(false);
        btnOpenFile.setEnabled(false);

        btnOpenFolder.addActionListener(e -> openReceivedFolder());
        btnOpenFile.addActionListener(e -> openReceivedFile());
        btnSaveAs.addActionListener(e -> saveFileAs());

        row3.add(btnSend);
        row3.add(new JLabel("   |   "));
        row3.add(btnOpenFolder);
        row3.add(btnOpenFile);
        row3.add(btnSaveAs);
        topPanel.add(row3);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false);
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(46, 204, 113));
        centerPanel.add(new JLabel("Nhật ký truyền tin:"), BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void selectAndSendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> sendFile(file)).start();
        }
    }

    private void sendFile(File file) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress destAddr = InetAddress.getByName(txtDestIP.getText());
            int destPort = Integer.parseInt(txtDestPort.getText());
            log("[SENDER] Bắt đầu gửi file: " + file.getName());
            btnSend.setEnabled(false);
            byte[] metaData = ("META:" + file.getName() + ":" + file.length()).getBytes();
            boolean metaAcked = false;
            while (!metaAcked) {
                sendPacket(socket, (byte) 0, 0, metaData, destAddr, destPort);
                try {
                    byte[] ackBuf = new byte[10];
                    DatagramPacket ackPack = new DatagramPacket(ackBuf, ackBuf.length);
                    socket.receive(ackPack);
                    if (ackBuf[0] == 4) {
                        metaAcked = true;
                    }
                } catch (SocketTimeoutException e) {
                    log("[SENDER] Timeout Metadata, gửi lại...");
                }
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int seqNum = 1;
                long totalSent = 0;
                long fileSize = file.length();
                while ((bytesRead = fis.read(buffer)) != -1) {
                    boolean acked = false;
                    byte[] actualData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, actualData, 0, bytesRead);
                    while (!acked) {
                        sendPacket(socket, (byte) 1, seqNum, actualData, destAddr, destPort);
                        try {
                            byte[] ackBuf = new byte[10];
                            DatagramPacket ackPack = new DatagramPacket(ackBuf, ackBuf.length);
                            socket.receive(ackPack);
                            int receivedAck = ByteBuffer.wrap(ackBuf, 1, 4).getInt();
                            if (ackBuf[0] == 2 && receivedAck == seqNum) {
                                acked = true;
                                totalSent += bytesRead;
                                final int prog = (int) ((totalSent * 100) / fileSize);
                                SwingUtilities.invokeLater(() -> progressBar.setValue(prog));
                            }
                        } catch (SocketTimeoutException e) {
                            log("[SENDER] Timeout gói #" + seqNum + ", đang gửi lại...");
                        }
                    }
                    seqNum++;
                }
            }
            boolean eofAcked = false;
            while (!eofAcked) {
                sendPacket(socket, (byte) 3, 0, new byte[0], destAddr, destPort);
                try {
                    byte[] ackBuf = new byte[10];
                    DatagramPacket ackPack = new DatagramPacket(ackBuf, ackBuf.length);
                    socket.receive(ackPack);
                    if (ackBuf[0] == 5) {
                        eofAcked = true;
                    }
                } catch (SocketTimeoutException e) {
                    log("[SENDER] Đợi xác nhận kết thúc...");
                }
            }
            log("[SENDER] Gửi file THÀNH CÔNG!");
            JOptionPane.showMessageDialog(this, "Đã gửi file xong!");
        } catch (Exception e) {
            log("[SENDER] LỖI: " + e.getMessage());
        } finally {
            btnSend.setEnabled(true);
        }
    }

    private void sendPacket(DatagramSocket socket, byte type, int seq, byte[] data, InetAddress addr, int port)
            throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(5 + data.length);
        bb.put(type);
        bb.putInt(seq);
        bb.put(data);
        byte[] packetData = bb.array();
        socket.send(new DatagramPacket(packetData, packetData.length, addr, port));
    }

    private void startReceiver() {
        startReceiverOnPort(Integer.parseInt(txtMyPort.getText()));
    }

    private void restartReceiver() {
        isReceiving = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        isReceiving = true;
        log("[SYSTEM] Đã đổi cổng lắng nghe sang: " + txtMyPort.getText());
        startReceiverOnPort(Integer.parseInt(txtMyPort.getText()));
    }

    private void startReceiverOnPort(int port) {
        new Thread(() -> {
            try {
                serverSocket = new DatagramSocket(port);
                log("[RECEIVER] Đang lắng nghe tại port " + port + "...");
                while (isReceiving) {
                    byte[] buf = new byte[CHUNK_SIZE + 10];
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    serverSocket.receive(pack);
                    if (chkSimulateLoss.isSelected() && random.nextInt(100) < 15) {
                        log("[RECEIVER] (Giả lập) Đã 'đánh rơi' một gói tin!");
                        continue;
                    }
                    processReceivedPacket(pack);
                }
            } catch (Exception e) {
                if (isReceiving) {
                    log("[RECEIVER] Lỗi: " + e.getMessage());
                }
            }
        }).start();
    }

    private FileOutputStream receiverFos;
    private int expectedSeq = 0;

    private void processReceivedPacket(DatagramPacket pack) throws IOException {
        byte[] data = pack.getData();
        byte type = data[0];
        int seq = ByteBuffer.wrap(data, 1, 4).getInt();
        if (type == 0) { // META
            String info = new String(data, 5, pack.getLength() - 5);
            String[] parts = info.split(":");
            String fileName = "received_" + parts[1];
            log("[RECEIVER] Nhận file: " + fileName);
            lastReceivedFile = new File(fileName);
            receiverFos = new FileOutputStream(lastReceivedFile);
            expectedSeq = 1;
            btnOpenFile.setEnabled(false);
            btnSaveAs.setEnabled(false);
            sendAck(pack.getAddress(), pack.getPort(), (byte) 4, 0);
        } else if (type == 1) { // DATA
            if (seq == expectedSeq) {
                if (receiverFos != null) {
                    receiverFos.write(data, 5, pack.getLength() - 5);
                }
                sendAck(pack.getAddress(), pack.getPort(), (byte) 2, seq);
                expectedSeq++;
            } else if (seq < expectedSeq) {
                sendAck(pack.getAddress(), pack.getPort(), (byte) 2, seq);
            }
        } else if (type == 3) { // EOF
            if (receiverFos != null) {
                receiverFos.close();
                receiverFos = null;
            }
            log("[RECEIVER] Đã nhận xong file. Bạn có thể mở ngay!");
            btnOpenFile.setEnabled(true);
            btnSaveAs.setEnabled(true);
            sendAck(pack.getAddress(), pack.getPort(), (byte) 5, 0);
        }
    }

    private void openReceivedFolder() {
        try {
            Desktop.getDesktop().open(new File("."));
        } catch (Exception e) {
            log("[ERROR] Không thể mở thư mục: " + e.getMessage());
        }
    }

    private void openReceivedFile() {
        try {
            if (lastReceivedFile != null && lastReceivedFile.exists()) {
                Desktop.getDesktop().open(lastReceivedFile);
            }
        } catch (Exception e) {
            log("[ERROR] Không thể mở file: " + e.getMessage());
        }
    }

    private void saveFileAs() {
        if (lastReceivedFile == null || !lastReceivedFile.exists()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(lastReceivedFile.getName().replace("received_", "")));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = chooser.getSelectedFile();
            try {
                copyFile(lastReceivedFile, targetFile);
                log("[SYSTEM] Đã lưu file thành công tại: " + targetFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Đã tải file về máy thành công!");
            } catch (IOException e) {
                log("[ERROR] Lỗi khi lưu file: " + e.getMessage());
            }
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private void sendAck(InetAddress addr, int port, byte type, int seq) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put(type);
        bb.putInt(seq);
        byte[] ackData = bb.array();
        serverSocket.send(new DatagramPacket(ackData, ackData.length, addr, port));
    }

    public void cleanup() {
        isReceiving = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
