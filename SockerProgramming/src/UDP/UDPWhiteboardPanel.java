package UDP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;

public class UDPWhiteboardPanel extends JPanel {
    private static final int PORT = 9876;
    private DatagramSocket socket;
    private InetAddress broadcastAddress;
    
    private BufferedImage canvas;
    private Graphics2D g2;
    private Point lastPoint;
    private Color currentColor = new Color(52, 152, 219); // Peter River Blue
    
    private boolean isRunning = true;

    public UDPWhiteboardPanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Khởi tạo Canvas (với kích thước lớn để cuộn nếu cần)
        canvas = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
        g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(currentColor);

        initNetwork();
        setupInteraction();
        startReceiver();
        
        // Thanh công cụ nhỏ
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(new Color(236, 240, 241));
        
        JButton btnClear = new JButton("Xóa trắng bảng");
        btnClear.setFont(new Font("Times New Roman", Font.BOLD, 14));
        btnClear.addActionListener(e -> clearCanvas(true));
        
        JLabel lblHint = new JLabel(" (Mở 2 cửa sổ để test vẽ đồng bộ qua UDP - Port 9876)");
        lblHint.setFont(new Font("Times New Roman", Font.ITALIC, 13));
        
        toolbar.add(btnClear);
        toolbar.add(lblHint);
        add(toolbar, BorderLayout.NORTH);
    }

    private void initNetwork() {
        try {
            // Sử dụng cơ chế cho phép chạy nhiều instance trên 1 máy để test (ReuseAddress)
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setBroadcast(true);
            broadcastAddress = InetAddress.getByName("255.255.255.255");
        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo mạng Whiteboard: " + e.getMessage());
        }
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
                    drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, currentColor, true);
                    lastPoint = currentPoint;
                }
            }
        });
    }

    private void drawLine(int x1, int y1, int x2, int y2, Color color, boolean send) {
        g2.setColor(color);
        g2.drawLine(x1, y1, x2, y2);
        repaint();

        if (send) {
            String msg = String.format("DRAW:%d,%d,%d,%d", x1, y1, x2, y2);
            sendUDP(msg);
        }
    }

    private void clearCanvas(boolean send) {
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(currentColor);
        repaint();
        if (send) sendUDP("CLEAR");
    }

    private void sendUDP(String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, PORT);
            socket.send(packet);
        } catch (Exception e) {
            // Im lặng hoặc log nhẹ
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
                    if (isRunning) System.err.println("Whiteboard Receiver Error: " + e.getMessage());
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
                    drawLine(x1, y1, x2, y2, currentColor, false);
                } else if (msg.equals("CLEAR")) {
                    clearCanvas(false);
                }
            } catch (Exception e) {
                // Ignore malformed packets
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
