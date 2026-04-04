package UDP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
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
    private static final int PORT = 9999;
    private static final int CHUNK_SIZE = 8192; // 8KB mỗi gói tin (An toàn cho WiFi)
    private static final float JPG_QUALITY = 0.5f; // Chất lượng ảnh nén (0.1 - 1.0)

    private DatagramSocket socket;
    private boolean isSharing = false;
    private boolean isWatching = false;
    private String targetIP = "192.168.1.7";
    private int frameID = 0;

    private JLabel lblDisplay;
    private JButton btnStartShare, btnStopShare, btnWatch;
    private JTextField txtTargetIP;
    private JLabel lblStatus;

    // Buffer để ghép mảnh ảnh (Receiver)
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
        initSocket();
    }

    private void initUI() {
        // 1. Toolbar điều khiển
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        toolBar.setOpaque(false);

        btnStartShare = createStyledButton("BẮT ĐẦU CHIA SẺ", new Color(46, 204, 113));
        btnStopShare = createStyledButton("DỪNG CHIA SẺ", new Color(231, 76, 60));
        btnStopShare.setEnabled(false);

        btnWatch = createStyledButton("XEM LIVE", new Color(52, 152, 219));

        txtTargetIP = new JTextField("192.168.1.7", 12);
        txtTargetIP.setFont(new Font("Segoe UI", Font.BOLD, 13));

        toolBar.add(new JLabel("Đích IP:"));
        toolBar.add(txtTargetIP);
        toolBar.add(btnStartShare);
        toolBar.add(btnStopShare);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(btnWatch);

        add(toolBar, BorderLayout.NORTH);

        // 2. Màn hình hiển thị
        lblDisplay = new JLabel();
        lblDisplay.setHorizontalAlignment(JLabel.CENTER);
        lblDisplay.setBackground(Color.BLACK);
        lblDisplay.setOpaque(true);
        add(new JScrollPane(lblDisplay), BorderLayout.CENTER);

        // 3. Status Bar
        lblStatus = new JLabel("Trạng thái: Sẵn sàng");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(lblStatus, BorderLayout.SOUTH);

        // Actions
        btnStartShare.addActionListener(e -> startSharing());
        btnStopShare.addActionListener(e -> stopSharing());
        btnWatch.addActionListener(e -> startWatching());
    }

    private void initSocket() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setReceiveBufferSize(2 * 1024 * 1024); // Tăng buffer nhận tránh mất gói
        } catch (Exception e) {
            lblStatus.setText("Lỗi khởi tạo Socket: " + e.getMessage());
        }
    }

    // --- LOGIC GỬI (SENDER) ---
    private void startSharing() {
        isSharing = true;
        targetIP = txtTargetIP.getText().trim();
        btnStartShare.setEnabled(false);
        btnStopShare.setEnabled(true);
        lblStatus.setText("Đang chia sẻ màn hình tới: " + targetIP);

        new Thread(() -> {
            try {
                Robot robot = new Robot();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle captureRect = new Rectangle(screen);
                InetAddress destAddr = InetAddress.getByName(targetIP);

                while (isSharing) {
                    // 1. Chụp màn hình
                    BufferedImage img = robot.createScreenCapture(captureRect);

                    // 2. Nén ảnh JPG
                    byte[] imageBytes = compressJPG(img);
                    if (imageBytes == null)
                        continue;

                    // 3. Chia nhỏ và gửi (Chunking)
                    sendImageInChunks(imageBytes, destAddr);

                    frameID++;
                    Thread.sleep(70); // ~15 FPS
                }
            } catch (Exception e) {
                stopSharing();
            }
        }).start();
    }

    private byte[] compressJPG(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
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

    private void sendImageInChunks(byte[] data, InetAddress addr) throws IOException {
        int totalChunks = (int) Math.ceil((double) data.length / CHUNK_SIZE);

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, data.length - offset);

            // Header (12 bytes): FrameID(4) | TotalChunks(4) | ChunkIdx(4)
            ByteBuffer bb = ByteBuffer.allocate(12 + length);
            bb.putInt(frameID);
            bb.putInt(totalChunks);
            bb.putInt(i);
            bb.put(data, offset, length);

            byte[] packetData = bb.array();
            socket.send(new DatagramPacket(packetData, packetData.length, addr, PORT));
        }
    }

    private void stopSharing() {
        isSharing = false;
        btnStartShare.setEnabled(true);
        btnStopShare.setEnabled(false);
        lblStatus.setText("Đã dừng chia sẻ.");
    }

    // --- LOGIC NHẬN (RECEIVER) ---
    private void startWatching() {
        if (isWatching)
            return;
        isWatching = true;
        btnWatch.setEnabled(false);
        lblStatus.setText("Đang lắng nghe dữ liệu màn hình...");

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

                    // Xử lý ghép mảnh
                    processChunk(fID, total, idx, packet.getData(), 12, packet.getLength() - 12);

                    // Xóa các buffer cũ (quá 2 giây không hoàn thành)
                    cleanupOldBuffers();

                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void processChunk(int fID, int total, int idx, byte[] data, int off, int len) {
        frameBuffers.putIfAbsent(fID, new FrameBuffer(total));
        FrameBuffer fb = frameBuffers.get(fID);

        if (fb.chunks[idx] == null) {
            fb.chunks[idx] = new byte[len];
            System.arraycopy(data, off, fb.chunks[idx], 0, len);
            fb.receivedCount++;
        }

        // Nếu nhận đủ các mảnh của khung hình
        if (fb.receivedCount == fb.totalChunks) {
            displayIncompleteFrame(fb);
            frameBuffers.remove(fID); // Xong khung hình này
        }
    }

    private void displayIncompleteFrame(FrameBuffer fb) {
        ByteArrayOutputStream fullImage = new ByteArrayOutputStream();
        try {
            for (byte[] chunk : fb.chunks) {
                if (chunk != null)
                    fullImage.write(chunk);
            }
            byte[] rawImage = fullImage.toByteArray();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawImage));

            if (img != null) {
                SwingUtilities.invokeLater(() -> {
                    // Scale ảnh cho vừa khung hiển thị
                    Image scaled = img.getScaledInstance(lblDisplay.getWidth(), -1, Image.SCALE_FAST);
                    lblDisplay.setIcon(new ImageIcon(scaled));
                });
            }
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
        return btn;
    }

    public void cleanup() {
        isSharing = false;
        isWatching = false;
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}
