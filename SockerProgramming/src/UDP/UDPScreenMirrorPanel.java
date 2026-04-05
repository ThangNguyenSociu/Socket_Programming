package UDP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class UDPScreenMirrorPanel extends JPanel {
    private static final int DEFAULT_PORT = 5000;
    private static final int CHUNK_SIZE = 8192;
    private static final float JPG_QUALITY = 0.5f;

    private DatagramSocket socket;
    private boolean isSharing = false;
    private boolean isWatching = false;
    private String targetIP = "127.0.0.1";
    private int currentPort = DEFAULT_PORT;
    private int frameID = 0;

    private JLabel lblDisplay;
    private JButton btnStartShare, btnStopShare, btnWatch;
    private JTextField txtTargetIP, txtPort;
    private JLabel lblStatus;

    private Map<Integer, FrameBuffer> frameBuffers = new HashMap<>();

    class FrameBuffer {
        int totalChunks;
        byte[][] chunks;
        int receivedCount;
        long timestamp;

        FrameBuffer(int total) {
            this.totalChunks = total;
            this.chunks = new byte[total][];
            this.receivedCount = 0;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public UDPScreenMirrorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 246, 250));

        initUI();
        initSocket(DEFAULT_PORT);
    }

    private void initUI() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        toolBar.setOpaque(false);

        btnStartShare = createStyledButton("BẮT ĐẦU CHIA SẺ", new Color(46, 204, 113));
        btnStopShare = createStyledButton("DỪNG CHIA SẺ", new Color(231, 76, 60));
        btnStopShare.setEnabled(false);

        btnWatch = createStyledButton("XEM LIVE", new Color(52, 152, 219));

        txtTargetIP = new JTextField("127.0.0.1", 10);
        txtTargetIP.setFont(new Font("Segoe UI", Font.BOLD, 13));

        txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 5);
        txtPort.setFont(new Font("Segoe UI", Font.BOLD, 13));

        toolBar.add(new JLabel("Đích IP:"));
        toolBar.add(txtTargetIP);
        toolBar.add(new JLabel("Cổng:"));
        toolBar.add(txtPort);
        toolBar.add(btnStartShare);
        toolBar.add(btnStopShare);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(btnWatch);

        add(toolBar, BorderLayout.NORTH);

        lblDisplay = new JLabel(
                "<html><center><h2 style='color:#7f8c8d;'>CHỜ TÍN HIỆU...</h2><p>Tương tác qua Cổng truyền tải chuyên nghiệp</p></center></html>");
        lblDisplay.setHorizontalAlignment(JLabel.CENTER);
        lblDisplay.setBackground(Color.BLACK);
        lblDisplay.setOpaque(true);
        add(new JScrollPane(lblDisplay), BorderLayout.CENTER);

        lblStatus = new JLabel("Trạng thái: Sẵn sàng trên Cổng " + DEFAULT_PORT);
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(lblStatus, BorderLayout.SOUTH);

        btnStartShare.addActionListener(e -> startSharing());
        btnStopShare.addActionListener(e -> stopSharing());
        btnWatch.addActionListener(e -> startWatching());
    }

    private void initSocket(int port) {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            socket.setReceiveBufferSize(4 * 1024 * 1024);
            currentPort = port;
            lblStatus.setText("Trạng thái: Đã mở Cổng " + port);
        } catch (Exception e) {
            lblStatus.setText("Lỗi mở Cổng " + port + ": " + e.getMessage());
            try {
                if (socket == null || socket.isClosed())
                    socket = new DatagramSocket();
            } catch (Exception ex) {
            }
        }
    }

    private void startSharing() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            targetIP = txtTargetIP.getText().trim();
            isSharing = true;
            btnStartShare.setEnabled(false);
            btnStopShare.setEnabled(true);
            txtPort.setEnabled(false); // Khóa port khi đang chạy

            lblStatus.setText("Đang truyền tới " + targetIP + ":" + port);

            new Thread(() -> {
                try {
                    Robot robot = new Robot();
                    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    Rectangle captureRect = new Rectangle(screen);
                    InetAddress destAddr = InetAddress.getByName(targetIP);

                    while (isSharing) {
                        BufferedImage img = robot.createScreenCapture(captureRect);
                        updatePreview(img);
                        byte[] imageBytes = compressJPG(img);
                        if (imageBytes != null) {
                            sendImageInChunks(imageBytes, destAddr, port);
                        }
                        frameID++;
                        Thread.sleep(85);
                    }
                } catch (Exception e) {
                    stopSharing();
                }
            }).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Số cổng không hợp lệ!");
        }
    }

    private void updatePreview(BufferedImage img) {
        SwingUtilities.invokeLater(() -> {
            int w = lblDisplay.getWidth();
            if (w <= 0)
                w = 600;
            Image scaled = img.getScaledInstance(w, -1, Image.SCALE_FAST);
            lblDisplay.setIcon(new ImageIcon(scaled));
            lblDisplay.setText("");
        });
    }

    private byte[] compressJPG(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            return null;
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPG_QUALITY);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
        }
        writer.dispose();
        return baos.toByteArray();
    }

    private void sendImageInChunks(byte[] data, InetAddress addr, int port) throws IOException {
        int totalChunks = (int) Math.ceil((double) data.length / CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, data.length - offset);
            ByteBuffer bb = ByteBuffer.allocate(12 + length);
            bb.putInt(frameID);
            bb.putInt(totalChunks);
            bb.putInt(i);
            bb.put(data, offset, length);
            byte[] packetData = bb.array();
            socket.send(new DatagramPacket(packetData, packetData.length, addr, port));
        }
    }

    private void stopSharing() {
        isSharing = false;
        btnStartShare.setEnabled(true);
        btnStopShare.setEnabled(false);
        txtPort.setEnabled(true);
        lblStatus.setText("Đã dừng chia sẻ.");
    }

    private void startWatching() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            initSocket(port); // Thử gán cổng mới nếu người dùng đổi

            if (isWatching)
                return;
            isWatching = true;
            btnWatch.setEnabled(false);
            txtPort.setEnabled(false);
            lblDisplay.setText("<html><center><h2 style='color:#3498db;'>WATCHING PORT " + port
                    + "...</h2><p>Đang đón tín hiệu màn hình</p></center></html>");

            new Thread(() -> {
                byte[] buf = new byte[CHUNK_SIZE + 100];
                while (isWatching) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                        int fID = bb.getInt();
                        int total = bb.getInt();
                        int idx = bb.getInt();
                        processChunk(fID, total, idx, packet.getData(), 12, packet.getLength() - 12);
                        cleanupOldBuffers();
                    } catch (Exception e) {
                    }
                }
            }).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Số cổng không hợp lệ!");
        }
    }

    private void processChunk(int fID, int total, int idx, byte[] data, int off, int len) {
        frameBuffers.putIfAbsent(fID, new FrameBuffer(total));
        FrameBuffer fb = frameBuffers.get(fID);
        if (fb != null && fb.chunks != null && idx < fb.totalChunks && fb.chunks[idx] == null) {
            fb.chunks[idx] = new byte[len];
            System.arraycopy(data, off, fb.chunks[idx], 0, len);
            fb.receivedCount++;
            if (fb.receivedCount == fb.totalChunks) {
                displayIncompleteFrame(fb);
                frameBuffers.remove(fID);
            }
        }
    }

    private void displayIncompleteFrame(FrameBuffer fb) {
        try {
            ByteArrayOutputStream fullImage = new ByteArrayOutputStream();
            for (byte[] chunk : fb.chunks) {
                if (chunk != null)
                    fullImage.write(chunk);
            }
            byte[] rawImage = fullImage.toByteArray();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawImage));
            if (img != null)
                updatePreview(img);
        } catch (Exception e) {
        }
    }

    private void cleanupOldBuffers() {
        long now = System.currentTimeMillis();
        frameBuffers.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > 2000);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.putClientProperty("FlatLaf.style", "arc: 12");
        return btn;
    }

    public void cleanup() {
        isSharing = false;
        isWatching = false;
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}
