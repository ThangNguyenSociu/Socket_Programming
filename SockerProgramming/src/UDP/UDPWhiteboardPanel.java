package UDP;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class UDPWhiteboardPanel extends JPanel {
    private static final int PORT = 9876;
    private DatagramSocket socket;
    private InetAddress broadcastAddress;

    private BufferedImage canvas;
    private Graphics2D g2;
    private Point lastPoint;
    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean isEraserMode = false;

    private boolean isRunning = true;

    public UDPWhiteboardPanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Khởi tạo Canvas (với kích thước lớn)
        canvas = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        updateStroke();

        initNetwork();
        setupInteraction();
        startReceiver();

        // --- Thanh công cụ (Toolbar) ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBackground(new Color(236, 240, 241));
        Font btnFont = new Font("Times New Roman", Font.BOLD, 13);

        // Các nút màu
        Color[] availableColors = { Color.BLACK, new Color(231, 76, 60), new Color(46, 204, 113),
                new Color(52, 152, 219), new Color(155, 89, 182) };
        for (Color c : availableColors) {
            JButton btnColor = new JButton();
            btnColor.setPreferredSize(new Dimension(25, 25));
            btnColor.setBackground(c);
            btnColor.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            btnColor.addActionListener(e -> {
                isEraserMode = false;
                currentColor = c;
                currentStrokeSize = 3;
                updateStroke();
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            });
            toolbar.add(btnColor);
        }

        // Nút Tẩy (Eraser)
        JButton btnEraser = new JButton("Tẩy (Eraser)");
        btnEraser.setFont(btnFont);
        btnEraser.setBackground(new Color(93, 193, 242));
        btnEraser.setForeground(Color.WHITE);
        btnEraser.putClientProperty("FlatLaf.style", "arc: 999");
        btnEraser.addActionListener(e -> {
            isEraserMode = true;
            currentStrokeSize = 30; // Nét tẩy to
            updateStroke();
            // Đổi cursor sang hình tròn (vòng tròn tẩy)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        });

        JButton btnClear = new JButton("Xóa trắng");
        btnClear.setFont(btnFont);
        btnClear.setBackground(new Color(158, 158, 158));
        btnClear.setForeground(Color.WHITE);
        btnClear.putClientProperty("FlatLaf.style", "arc: 999");
        btnClear.addActionListener(e -> clearCanvas(true));

        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(btnEraser);
        toolbar.add(btnClear);
        toolbar.add(new JLabel(" (Port 9876 - Common Subnet)"));

        add(toolbar, BorderLayout.NORTH);
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    private void updateStroke() {
        if (g2 != null) {
            g2.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
    }

    private void initNetwork() {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);

            // Tìm địa chỉ Broadcast KHÔNG phải máy ảo
            broadcastAddress = discoverBroadcastAddress();
        } catch (Exception e) {
            System.err.println("Lỗi mạng Whiteboard: " + e.getMessage());
        }
    }

    private InetAddress discoverBroadcastAddress() {
        try {
            NetworkInterface fallbackNI = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                String name = ni.getDisplayName().toLowerCase();
                if (ni.isLoopback() || !ni.isUp()) continue;
                if (fallbackNI == null) fallbackNI = ni;

                if (!name.contains("vmware") && !name.contains("virtual") && !name.contains("vbox") && !name.contains("host-only") && !name.contains("pseudo")) {
                    for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                        InetAddress broad = addr.getBroadcast();
                        if (broad != null) return broad;
                    }
                }
            }
            if (fallbackNI != null) {
                for (InterfaceAddress addr : fallbackNI.getInterfaceAddresses()) {
                    InetAddress broad = addr.getBroadcast();
                    if (broad != null) return broad;
                }
            }
        } catch (Exception e) { }
        try { return InetAddress.getByName("255.255.255.255"); } catch (Exception e) { return null; }
    }

    private void setupInteraction() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();
                if (lastPoint != null) {
                    Color drawColor = isEraserMode ? Color.WHITE : currentColor;
                    drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, drawColor, currentStrokeSize,
                            true);
                    lastPoint = currentPoint;
                }
            }
        });
    }

    private void drawLine(int x1, int y1, int x2, int y2, Color color, int size, boolean send) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        repaint();

        if (send) {
            // Gửi dữ liệu màu qua định dạng: R,G,B
            String msg = String.format("DRAW:%d,%d,%d,%d,%d,%d,%d,%d",
                    x1, y1, x2, y2, color.getRed(), color.getGreen(), color.getBlue(), size);
            sendUDP(msg);
        }
    }

    private void clearCanvas(boolean send) {
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2.setComposite(AlphaComposite.SrcOver);
        repaint();
        if (send) {
            sendUDP("CLEAR");
        }
    }

    private void sendUDP(String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
            socket.send(packet);
        } catch (Exception e) {
        }
    }

    private void startReceiver() {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    if (socket != null && !socket.isClosed()) {
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        processMessage(msg);
                    }
                } catch (Exception e) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (msg.startsWith("DRAW:")) {
                    String[] parts = msg.substring(5).split(",");
                    int x1 = Integer.parseInt(parts[0]);
                    int y1 = Integer.parseInt(parts[1]);
                    int x2 = Integer.parseInt(parts[2]);
                    int y2 = Integer.parseInt(parts[3]);
                    int r = Integer.parseInt(parts[4]);
                    int g = Integer.parseInt(parts[5]);
                    int b = Integer.parseInt(parts[6]);
                    int size = Integer.parseInt(parts[7]);
                    drawLine(x1, y1, x2, y2, new Color(r, g, b), size, false);
                } else if (msg.equals("CLEAR")) {
                    clearCanvas(false);
                }
            } catch (Exception e) {
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvas != null) {
            g.drawImage(canvas, 0, 0, null);
        }
    }

    public void cleanup() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
