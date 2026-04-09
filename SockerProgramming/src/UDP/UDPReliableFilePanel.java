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

    private JTextField txtDestIP, txtDestPort, txtMyPort;
    private JProgressBar progress;
    private JTextArea logArea;
    private JButton btnSend;

    public UDPReliableFilePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        physicalIP = getLocalPhysicalIP();
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
                String n = ni.getName().toLowerCase();
                if (ni.isLoopback() || !ni.isUp())
                    continue;
                if (fallbackNI == null)
                    fallbackNI = ni;

                // MẠNG VẬT LÝ ƯU TIÊN: WIFI / ETHERNET (LOẠI BỎ RADMIN/HAMACHI/VPN)
                if (!dN.contains("vmware") && !n.contains("vmware") && !dN.contains("virtual") && !n.contains("virtual") && !dN.contains("vbox") && !n.contains("vbox")
                        && !dN.contains("radmin") && !n.contains("radmin") && !dN.contains("hamachi") && !n.contains("hamachi") && !dN.contains("zerotier") && !n.contains("zerotier")
                        && !dN.contains("tunnel") && !n.contains("tunnel") && !dN.contains("teredo") && !n.contains("teredo") && !dN.contains("pseudo") && !n.contains("pseudo")) {
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
        } catch (Exception e) {
        }
        return "127.0.0.1";
    }

    private void initUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, javax.swing.BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row0.setOpaque(false);
        JLabel lblInfo = new JLabel("IP WiFi/LAN: " + physicalIP);
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblInfo.setForeground(primaryColor);
        row0.add(lblInfo);
        topPanel.add(row0);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setOpaque(false);
        row1.add(new JLabel("Đích IP:"));
        txtDestIP = new JTextField(physicalIP, 12);
        row1.add(txtDestIP);
        row1.add(new JLabel("Port:"));
        txtDestPort = new JTextField(String.valueOf(PORT), 5);
        row1.add(txtDestPort);
        topPanel.add(row1);

        btnSend = new JButton("CHỌN & GỬI FILE");
        btnSend.setBackground(primaryColor);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel rowSend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowSend.add(btnSend);
        topPanel.add(rowSend);

        add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(0, 25));
        add(progress, BorderLayout.SOUTH);

        btnSend.addActionListener(e -> selectAndSendFile());
    }

    private void startReceiver() {
        new Thread(() -> {
            try {
                if (socket == null || socket.isClosed()) {
                    socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(PORT));
                }
                log("Đang lắng nghe file tại cổng " + PORT + "...");

                while (true) {
                    byte[] buf = new byte[CHUNK_SIZE + 100];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    String msg = new String(p.getData(), 0, p.getLength());
                    if (msg.startsWith("FILE_HEADER:")) {
                        receiveFile(msg, p.getAddress(), p.getPort());
                    }
                }
            } catch (Exception e) {
            }
        }).start();
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
                for (int i = 0; i < totalChunks; i++) {
                    byte[] buf = new byte[CHUNK_SIZE + 100];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    // Phân tích chunk kèm ID
                    byte[] chunkContent = new byte[p.getLength()];
                    System.arraycopy(p.getData(), 0, chunkContent, 0, p.getLength());
                    fos.write(chunkContent);

                    sendACK("ACK_" + i, senderAddr, senderPort);
                    updateProgress(i + 1, totalChunks);
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
            socket.setSoTimeout(5000);
            if (!waitForACK("ACK_START")) {
                log("Bên nhận không phản hồi Header.");
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] chunk = new byte[CHUNK_SIZE];
                for (int i = 0; i < totalChunks; i++) {
                    int read = fis.read(chunk);
                    boolean delivered = false;
                    while (!delivered) {
                        sendPacketData(chunk, read, destAddr, destPort);

                        if (waitForACK("ACK_" + i))
                            delivered = true;
                        else
                            log("Gửi lại Chunk " + i + "...");
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

    private void sendPacketData(byte[] data, int len, InetAddress addr, int port) throws Exception {
        socket.send(new DatagramPacket(data, len, addr, port));
    }

    private boolean waitForACK(String expected) {
        try {
            byte[] b = new byte[100];
            DatagramPacket p = new DatagramPacket(b, b.length);
            socket.receive(p);
            String ack = new String(p.getData(), 0, p.getLength());
            return ack.equals(expected);
        } catch (SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
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
