// File: WebcamQRScanner.java
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.ResultPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.awt.Dimension;


public class webcamscanner {

    // Custom panel that draws overlay using ZXing result points
    static class OverlayWebcamPanel extends WebcamPanel {
        private volatile ResultPoint[] resultPoints;
        private final Webcam webcam;

        public OverlayWebcamPanel(Webcam webcam) {
            super(webcam);
            this.webcam = webcam;
            setOpaque(true);
        }

        public void setResultPoints(ResultPoint[] pts) {
            this.resultPoints = pts;
            SwingUtilities.invokeLater(this::repaint);
        }

        public void clearResultPoints() {
            this.resultPoints = null;
            SwingUtilities.invokeLater(this::repaint);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // draw centered scanning box as visual guidance
                int boxSize = Math.min(getWidth(), getHeight()) * 3 / 5;
                int boxX = (getWidth() - boxSize) / 2;
                int boxY = (getHeight() - boxSize) / 2;
                g2.setColor(new Color(0, 255, 0, 100));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(boxX, boxY, boxSize, boxSize);

                ResultPoint[] pts = resultPoints;
                if (pts == null || pts.length == 0) return;

                java.awt. Dimension imgSize = webcam.getViewSize();
                if (imgSize == null) return;

                double sx = getWidth() / (double) imgSize.width;
                double sy = getHeight() / (double) imgSize.height;


                double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
                double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
                for (ResultPoint p : pts) {
                    if (p == null) continue;
                    minX = Math.min(minX, p.getX());
                    minY = Math.min(minY, p.getY());
                    maxX = Math.max(maxX, p.getX());
                    maxY = Math.max(maxY, p.getY());
                }
                if (minX == Double.POSITIVE_INFINITY) return;

                int x1 = (int) Math.round(minX * sx);
                int y1 = (int) Math.round(minY * sy);
                int x2 = (int) Math.round(maxX * sx);
                int y2 = (int) Math.round(maxY * sy);

                int w = x2 - x1;
                int h = y2 - y1;
                int pad = Math.max(6, (int) (Math.min(Math.max(w, 0), Math.max(h, 0)) * 0.08));
                x1 = Math.max(0, x1 - pad);
                y1 = Math.max(0, y1 - pad);
                w = Math.min(getWidth() - x1, w + pad * 2);
                h = Math.min(getHeight() - y1, h + pad * 2);

                g2.setStroke(new BasicStroke(3f));
                g2.setColor(new Color(0, 200, 0, 200));
                g2.drawRect(x1, y1, Math.max(2, w), Math.max(2, h));

                // draw small circles at each result point
                g2.setStroke(new BasicStroke(1f));
                for (ResultPoint p : pts) {
                    if (p == null) continue;
                    int cx = (int) Math.round(p.getX() * sx);
                    int cy = (int) Math.round(p.getY() * sy);
                    int r = 6;
                    g2.fillOval(cx - r / 2, cy - r / 2, r, r);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // Pick a supported view size that's close to desired to avoid IllegalArgumentException
    private static Dimension chooseBestViewSize(Webcam webcam, Dimension desired) {
        Dimension[] supported = webcam.getViewSizes();
        System.out.println("Camera supported sizes: " + Arrays.toString(supported));

        // exact match?
        for (Dimension d : supported) {
            if (d.width == desired.width && d.height == desired.height) {
                System.out.println("Using desired size: " + d);
                return d;
            }
        }

        // choose the largest size <= desired (both dims), otherwise largest available
        Dimension best = null;
        for (Dimension d : supported) {
            if (d.width <= desired.width && d.height <= desired.height) {
                if (best == null || (d.width * d.height > best.width * best.height)) {
                    best = d;
                }
            }
        }
        if (best != null) {
            System.out.println("Falling back to closest <= desired size: " + best);
            return best;
        }

        for (Dimension d : supported) {
            if (best == null || d.width * d.height > best.width * best.height) best = d;
        }
        System.out.println("Falling back to largest available size: " + best);
        return best;
    }

    public static void main(String[] args) {
        // Optional: register OpenIMAJ driver if you added that dependency
        // Webcam.setDriver(new com.github.sarxos.webcam.ds.openimaj.OpenImajDriver());

        SwingUtilities.invokeLater(() -> {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("No webcam detected.");
                return;
            }

            Dimension desired = WebcamResolution.HD.getSize(); // try 1280x720
            Dimension chosen = chooseBestViewSize(webcam, desired);
            if (chosen != null) {
                try {
                    webcam.setViewSize(chosen);
                } catch (IllegalArgumentException e) {
                    System.err.println("Chosen size rejected by webcam; continuing with default: " + e.getMessage());
                }
            }

            try {
                webcam.open();
            } catch (Exception e) {
                System.err.println("Failed to open webcam: " + e.getMessage());
                return;
            }

            // UI components
            OverlayWebcamPanel panel = new OverlayWebcamPanel(webcam);
            panel.setFPSDisplayed(true);
            panel.setMirrored(true);
            panel.setPreferredSize(new Dimension(800, 600)); // preview will scale

            JTextArea resultsArea = new JTextArea(10, 30);
            resultsArea.setEditable(false);
            JScrollPane resultsScroll = new JScrollPane(resultsArea);

            JFrame window = new JFrame("Webcam QR Scanner");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLayout(new BorderLayout());
            window.add(panel, BorderLayout.CENTER);
            window.add(resultsScroll, BorderLayout.EAST);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            // Start scanner thread after window is visible and pass resultsArea
            Thread scanner = new Thread(() -> runScannerLoop(webcam, panel, window, resultsArea));
            scanner.setDaemon(true);
            scanner.start();
        });
    }

    // scanner loop now accepts resultsArea so it can append and copy results
    private static void runScannerLoop(Webcam webcam, OverlayWebcamPanel panel, JFrame window, JTextArea resultsArea) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Arrays.asList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        Reader reader = new MultiFormatReader();
        System.out.println("Scanner started. Move a QR code in front of the camera.");

        final long pauseAfterDetectionMs = 1500; // pause display of overlay before clearing

        try {
            while (window.isVisible()) {
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    Thread.sleep(50);
                    continue;
                }

                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    Result result = reader.decode(bitmap, hints);
                    if (result != null) {
                        final String text = result.getText();
                        System.out.println("Decoded: " + text);

                        ResultPoint[] pts = result.getResultPoints();
                        if (pts != null && pts.length > 0) {
                            panel.setResultPoints(pts);
                        }

                        // Append to resultsArea on EDT and copy to clipboard
                        SwingUtilities.invokeLater(() -> {
                            resultsArea.append(text + "\n");

                            // copy to clipboard
                            StringSelection selection = new StringSelection(text);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            try {
                                clipboard.setContents(selection, selection);
                            } catch (IllegalStateException ise) {
                                // clipboard busy; ignore
                                System.err.println("Clipboard unavailable: " + ise.getMessage());
                            }
                        });

                        // pause to show overlay, then clear overlay and continue scanning
                        Thread.sleep(pauseAfterDetectionMs);
                        panel.clearResultPoints();

                        // continue scanning (no break) — remove continue if you want to stop after one detection
                        continue;
                    }
                } catch (NotFoundException nfe) {
                    // nothing found in this frame, ignore and continue
                } catch (FormatException | ChecksumException e) {
                    // decoding error for this frame, ignore
                }

                Thread.sleep(100); // throttle loop
            }
        } catch (InterruptedException ignored) {
        } finally {
            // cleanup
            try {
                if (webcam.isOpen()) webcam.close();
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> {
                if (window != null) window.dispose();
            });
            System.out.println("Scanner stopped.");
        }
    }
}
