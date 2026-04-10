package UDP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import com.sun.management.OperatingSystemMXBean;

/**
 * Demo UDP: Giám sát hệ thống từ xa (Remote System Monitor)
 * - Tự động phát quảng bá thông số CPU/RAM của máy hiện tại.
 * - Nhận và hiển thị thông số của các máy khác trong mạng.
 */
public class UDPSystemMonitorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_PORT = 9999;
    private int currentPort = DEFAULT_PORT;

    private static final Color primaryColor = new Color(0, 104, 255);
    private static final Color bgSecondary = new Color(245, 247, 250);
    private static final Color successColor = new Color(40, 167, 69);
    private static final Color warningColor = new Color(255, 193, 7);
    private static final Color dangerColor = new Color(220, 53, 69);

    private DatagramSocket socket;
    private String hostname = "Unknown";
    private String osName = System.getProperty("os.name");
    private java.util.List<String> myLocalIPs = new java.util.ArrayList<>();
    private java.util.Set<String> targetIPs = new java.util.HashSet<>();

    private JPanel cardsPanel;
    private javax.swing.JTextField txtIP, txtTargetPort, txtListenPort;
    private Map<String, MachineCard> machineMap = new HashMap<>();
    private Timer monitorTimer;
    private boolean isRunning = true;

    public UDPSystemMonitorPanel() {
        setLayout(new BorderLayout());
        setBackground(bgSecondary);
        initUI();
        initNetwork();
        startMonitoring();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(25, 30, 15, 30));

        JLabel title = new JLabel("Hệ thống Giám sát UDP");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(44, 62, 80));
        header.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Theo dõi tài nguyên CPU/RAM của các máy tính trong mạng nội bộ thời gian thực");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(127, 140, 141));
        header.add(subtitle, BorderLayout.SOUTH);

        // Control Panel: Nhập IP & Port
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        controlPanel.setOpaque(false);

        txtListenPort = new javax.swing.JTextField(String.valueOf(DEFAULT_PORT), 5);
        txtListenPort.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        javax.swing.JButton btnRestart = new javax.swing.JButton("Đổi Port Nhận");
        btnRestart.addActionListener(e -> restartSocket());

        txtIP = new javax.swing.JTextField(12);
        txtIP.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Địa chỉ IP");
        txtIP.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        txtTargetPort = new javax.swing.JTextField(String.valueOf(DEFAULT_PORT), 5);
        txtTargetPort.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        javax.swing.JButton btnAdd = new javax.swing.JButton("Kết nối & Giám sát");
        btnAdd.setBackground(primaryColor);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        btnAdd.addActionListener(e -> subscribeToIP(txtIP.getText().trim(), txtTargetPort.getText().trim()));

        controlPanel.add(new JLabel("Cổng nhận:"));
        controlPanel.add(txtListenPort);
        controlPanel.add(btnRestart);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(new JLabel("Mục tiêu:"));
        controlPanel.add(txtIP);
        controlPanel.add(new JLabel(":"));
        controlPanel.add(txtTargetPort);
        controlPanel.add(btnAdd);
        header.add(controlPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Content: Danh sách máy tính (Cards)
        cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        cardsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Banner thông tin máy hiện tại
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 30, 10, 30));
        JLabel lblStatus = new JLabel("● Trạng thái: Đang phát tín hiệu hệ thống...");
        lblStatus.setForeground(successColor);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footer.add(lblStatus, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    private void initNetwork() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            detectLocalIPs();

            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(currentPort));

            // Thread nhận dữ liệu
            new Thread(this::receiveLoop).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void detectLocalIPs() {
        try {
            myLocalIPs.clear();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address) {
                        myLocalIPs.add(ia.getAddress().getHostAddress());
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void restartSocket() {
        try {
            int newPort = Integer.parseInt(txtListenPort.getText().trim());
            if (socket != null) socket.close();
            currentPort = newPort;
            initNetwork();
            javax.swing.JOptionPane.showMessageDialog(this, "Đã đổi cổng nhận sang: " + currentPort);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Lỗi port: " + e.getMessage());
        }
    }

    private void subscribeToIP(String ip, String portStr) {
        if (ip.isEmpty()) return;
        try {
            int targetPort = Integer.parseInt(portStr);
            String msg = "SUBSCRIBE:" + currentPort; // Gửi kèm port mình đang nghe để bên kia biết gửi về đâu
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), targetPort));
            // Thông báo đã gửi yêu cầu
            updateMachineCard(ip, "Đang kết nối...", 0, 0, "Port " + targetPort, "Đang quét...", 0, "Đang quét...");
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Lỗi IP/Port: " + e.getMessage());
        }
    }

    private void startMonitoring() {
        monitorTimer = new Timer(true);
        monitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning) return;
                sendSystemStats();
                updateLocalUI();
            }
        }, 0, 1000); // Gửi mỗi giây
    }

    private Map<String, Integer> subscriberPorts = new HashMap<>();

    private void sendSystemStats() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpu = osBean.getSystemCpuLoad() * 100;
            if (cpu < 0) cpu = 0; 

            long totalMem = osBean.getTotalPhysicalMemorySize();
            long freeMem = osBean.getFreePhysicalMemorySize();
            double ramUsed = totalMem > 0 ? (double) (totalMem - freeMem) / totalMem * 100 : 0;

            // Lấy thông tin ổ đĩa (Disk)
            java.io.File[] roots = java.io.File.listRoots();
            StringBuilder diskInfo = new StringBuilder();
            for (java.io.File root : roots) {
                if (root.getTotalSpace() > 0) {
                    double usage = (double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace() * 100;
                    diskInfo.append(root.getPath()).append(" (").append(String.format("%.0f", usage)).append("%) | ");
                }
            }
            
            // Số lượng process & Danh sách tên process (Top 15)
            java.util.List<ProcessHandle> allProcs = ProcessHandle.allProcesses().collect(java.util.stream.Collectors.toList());
            int processCount = allProcs.size();
            String procList = allProcs.stream()
                .limit(15)
                .map(p -> {
                    String cmd = p.info().command().orElse("");
                    if (cmd.isEmpty()) return "PID-" + p.pid() + " (System)";
                    int lastIdx = Math.max(cmd.lastIndexOf("/"), cmd.lastIndexOf("\\"));
                    return (lastIdx >= 0 ? cmd.substring(lastIdx + 1) : cmd) + " [" + p.pid() + "]";
                })
                .collect(java.util.stream.Collectors.joining(", "));

            String msg = String.format("STATS|#|%s|#|%.1f|#|%.1f|#|%s|#|%s|#|%d|#|%s", hostname, cpu, ramUsed, osName, diskInfo.toString(), processCount, procList);
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);

            // Gửi tới tất cả các máy đã Subscribe
            for (String targetIP : targetIPs) {
                try {
                    int port = subscriberPorts.getOrDefault(targetIP, DEFAULT_PORT);
                    socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(targetIP), port));
                } catch (Exception e) {}
            }

        } catch (Exception e) {}
    }

    private void updateLocalUI() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpu = osBean.getSystemCpuLoad() * 100;
        long totalMem = osBean.getTotalPhysicalMemorySize();
        long freeMem = osBean.getFreePhysicalMemorySize();
        double ramUsed = totalMem > 0 ? (double) (totalMem - freeMem) / totalMem * 100 : 0;
        
        // Local Disk & Process
        java.util.List<ProcessHandle> allProcs = ProcessHandle.allProcesses().collect(java.util.stream.Collectors.toList());
        int processCount = allProcs.size();
        String procList = allProcs.stream().limit(15)
            .map(p -> {
                String cmd = p.info().command().orElse("");
                if (cmd.isEmpty()) return "PID-" + p.pid() + " (System)";
                return cmd.substring(Math.max(cmd.lastIndexOf("/"), cmd.lastIndexOf("\\")) + 1) + " [" + p.pid() + "]";
            })
            .collect(java.util.stream.Collectors.joining(", "));

        java.io.File[] roots = java.io.File.listRoots();
        StringBuilder disks = new StringBuilder();
        for(java.io.File r : roots) {
            if (r.getTotalSpace() > 0) {
                double u = (double)(r.getTotalSpace() - r.getFreeSpace()) / r.getTotalSpace() * 100;
                disks.append(r.getPath()).append(" (").append(String.format("%.0f", u)).append("%) | ");
            }
        }

        String myIP = myLocalIPs.isEmpty() ? "127.0.0.1" : myLocalIPs.get(0);
        updateMachineCard(myIP, hostname + " (MÁY NÀY)", cpu, ramUsed, osName, disks.toString(), processCount, procList);
    }

    private void receiveLoop() {
        byte[] buf = new byte[1024];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String senderIP = packet.getAddress().getHostAddress();

                if (myLocalIPs.contains(senderIP)) continue; // Bỏ qua máy mình (đã tự update local)

                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (msg.startsWith("SUBSCRIBE")) {
                    targetIPs.add(senderIP);
                    // Lấy port mà máy kia đang nghe (nếu có gửi kèm)
                    String[] parts = msg.split(":");
                    int port = (parts.length > 1) ? Integer.parseInt(parts[1]) : DEFAULT_PORT;
                    subscriberPorts.put(senderIP, port);
                    System.out.println("[MONITOR] Máy " + senderIP + " vừa đăng ký theo dõi tại cổng " + port);
                } else if (msg.startsWith("STATS")) {
                    String[] parts = msg.split("\\|#\\|");
                    if (parts.length >= 8) { // STATS, Name, CPU, RAM, OS, Disk, ProcessCount, ProcessList
                        String hName = parts[1];
                        double cpu = Double.parseDouble(parts[2]);
                        double ram = Double.parseDouble(parts[3]);
                        String os = parts[4];
                        String disk = parts[5];
                        int procCount = Integer.parseInt(parts[6]);
                        String procList = parts[7];
                        updateMachineCard(senderIP, hName, cpu, ram, os, disk, procCount, procList);
                    }
                }
            } catch (Exception e) {}
        }
    }

    private void updateMachineCard(String ip, String hName, double cpu, double ram, String os, String disk, int procCount, String procList) {
        SwingUtilities.invokeLater(() -> {
            MachineCard card = machineMap.get(ip);
            if (card == null) {
                card = new MachineCard(ip, hName, os);
                machineMap.put(ip, card);
                cardsPanel.add(card);
                cardsPanel.revalidate();
                cardsPanel.repaint();
            }
            card.updateIdentity(hName, os); // Cập nhật tên và OS nếu có thay đổi (từ "Đang kết nối...")
            card.updateStats(cpu, ram, disk, procCount, procList);
        });
    }

    private void removeMachine(String ip) {
        targetIPs.remove(ip);
        subscriberPorts.remove(ip);
        MachineCard card = machineMap.remove(ip);
        if (card != null) {
            cardsPanel.remove(card);
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }
    }

    public void cleanup() {
        isRunning = false;
        if (monitorTimer != null) monitorTimer.cancel();
        if (socket != null) socket.close();
    }

    // ========== INNER CLASS: MACHINE CARD UI ==========

    class MachineCard extends JPanel {
        private String ip;
        private String machineName;
        private String machineOS;
        private JLabel lblName, lblIP; // Thêm để update sau này
        private JLabel lblCPU, lblRAM;
        private JProgressBar barCPU, barRAM;
        private long lastUpdate;

        public MachineCard(String ip, String name, String os) {
            this.ip = ip;
            this.machineName = name;
            this.machineOS = os;
            this.lastUpdate = System.currentTimeMillis();

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(340, 220));
            setBackground(Color.WHITE);
            putClientProperty(FlatClientProperties.STYLE, "arc: 20");
            setBorder(new EmptyBorder(20, 15, 20, 15));

            // Header: Icon + Tên
            JPanel head = new JPanel(new BorderLayout(10, 0));
            head.setOpaque(false);
            JLabel lblIcon = new JLabel("DEVICE");
            lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblIcon.setForeground(Color.WHITE);
            lblIcon.setBackground(primaryColor);
            lblIcon.setOpaque(true);
            lblIcon.setBorder(new EmptyBorder(2, 6, 2, 6));
            head.add(lblIcon, BorderLayout.WEST);

            JPanel titleGroup = new JPanel(new GridLayout(2, 1));
            titleGroup.setOpaque(false);
            lblName = new JLabel(name);
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblName.setToolTipText(name); // Di chuột vào để xem tên đầy đủ
            lblName.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            lblName.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    javax.swing.JOptionPane.showMessageDialog(MachineCard.this, "Tên máy đầy đủ:\n" + name, "Thông tin", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            });

            lblIP = new JLabel(ip + " | " + os);
            lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblIP.setForeground(Color.GRAY);
            titleGroup.add(lblName);
            titleGroup.add(lblIP);
            head.add(titleGroup, BorderLayout.CENTER);

            // Nút Xóa (Delete)
            javax.swing.JButton btnDelete = new javax.swing.JButton("X");
            btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnDelete.setFocusPainted(false);
            btnDelete.setBorderPainted(false);
            btnDelete.setContentAreaFilled(false);
            btnDelete.setForeground(Color.LIGHT_GRAY);
            btnDelete.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            btnDelete.setToolTipText("Gỡ bỏ máy này");
            btnDelete.addActionListener(e -> removeMachine(ip));
            btnDelete.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { btnDelete.setForeground(dangerColor); }
                public void mouseExited(java.awt.event.MouseEvent e) { btnDelete.setForeground(Color.LIGHT_GRAY); }
            });
            head.add(btnDelete, BorderLayout.EAST);

            add(head, BorderLayout.NORTH);

            // Stats Center
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            center.add(Box.createVerticalStrut(20));

            // CPU Info
            JPanel cpuLabelRow = new JPanel(new BorderLayout());
            cpuLabelRow.setOpaque(false);
            cpuLabelRow.add(new JLabel("CPU Usage"), BorderLayout.WEST);
            lblCPU = new JLabel("0.0%");
            lblCPU.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cpuLabelRow.add(lblCPU, BorderLayout.EAST);
            center.add(cpuLabelRow);

            barCPU = new JProgressBar(0, 100);
            barCPU.setPreferredSize(new Dimension(0, 8));
            barCPU.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
            center.add(Box.createVerticalStrut(5));
            center.add(barCPU);

            center.add(Box.createVerticalStrut(15));

            // RAM Info
            JPanel ramLabelRow = new JPanel(new BorderLayout());
            ramLabelRow.setOpaque(false);
            ramLabelRow.add(new JLabel("Memory Usage"), BorderLayout.WEST);
            lblRAM = new JLabel("0.0%");
            lblRAM.setFont(new Font("Segoe UI", Font.BOLD, 12));
            ramLabelRow.add(lblRAM, BorderLayout.EAST);
            center.add(ramLabelRow);

            barRAM = new JProgressBar(0, 100);
            barRAM.setPreferredSize(new Dimension(0, 8));
            barRAM.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
            center.add(Box.createVerticalStrut(5));
            center.add(barRAM);

            add(center, BorderLayout.CENTER);

            // Footer: Life status
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            
            JLabel lblLife = new JLabel("● Online", SwingConstants.LEFT);
            lblLife.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblLife.setForeground(successColor);
            foot.add(lblLife, BorderLayout.WEST);

            javax.swing.JButton btnDetail = new javax.swing.JButton("Chi tiết");
            btnDetail.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btnDetail.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 2,5,2,5");
            btnDetail.addActionListener(e -> showDetails());
            foot.add(btnDetail, BorderLayout.EAST);

            add(foot, BorderLayout.SOUTH);
        }

        private String currentDiskInfo = "";
        private int currentProcCount = 0;
        private String currentProcList = "";

        public void updateIdentity(String name, String os) {
            // Chỉ cập nhật nếu tên khác với "Đang kết nối..." hoặc hostname thật thay đổi
            if (!this.machineName.equals(name)) {
                this.machineName = name;
                lblName.setText(name);
                lblName.setToolTipText(name);
            }
            if (!this.machineOS.equals(os)) {
                this.machineOS = os;
                lblIP.setText(ip + " | " + os);
            }
        }

        public void updateStats(double cpu, double ram, String disk, int procCount, String procList) {
            this.currentDiskInfo = disk;
            this.currentProcCount = procCount;
            this.currentProcList = procList;
            
            lastUpdate = System.currentTimeMillis();
            lblCPU.setText(String.format("%.1f %%", cpu));
            lblRAM.setText(String.format("%.1f %%", ram));
            barCPU.setValue((int) cpu);
            barRAM.setValue((int) ram);

            // Đổi màu theo mức độ tải
            barCPU.setForeground(getColorForValue(cpu));
            barRAM.setForeground(getColorForValue(ram));
        }

        private void showDetails() {
            String info = String.format(
                "<html><body style='width: 300px; font-family: Segoe UI;'>" +
                "<h3 style='color:#0068FF'>Thông tin máy: %s</h3>" +
                "<b>IP:</b> %s<br>" +
                "<b>OS:</b> %s<br><hr>" +
                "<b>Tổng số tiến trình:</b> %d<br><br>" +
                "<b>Ổ đĩa:</b><br>%s<hr>" +
                "<b>Danh sách Top tiến trình:</b><br>" +
                "<div style='background:#f0f0f0; padding:5px; border: 1px solid #ccc; font-size: 10px;'>" +
                "%s" +
                "</div>" +
                "</body></html>",
                machineName, ip, machineOS, currentProcCount, currentDiskInfo.replace("|", "<br>"), currentProcList.replace(", ", "<br>• ")
            );
            javax.swing.JOptionPane.showMessageDialog(this, info, "Chi tiết hệ thống", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }

        private Color getColorForValue(double val) {
            if (val < 60) return successColor;
            if (val < 85) return warningColor;
            return dangerColor;
        }
    }
}
