import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class webcam {

    public static void main(String[] args) {

        // 1) Open default webcam
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("No webcam detected.");
            return;
        }

        // choose resolution (optional)
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        try {
            webcam.open();
        } catch (Exception e) {
            System.err.println("Failed to open webcam: " + e.getMessage());
            return;
        }

        // 2) Optional: show preview in a window
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setMirrored(true);

        JFrame window = new JFrame("Webcam QR Scanner");
        window.add(panel);
        window.pack();
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setVisible(true);

        // 3) ZXing reader with hints (faster / more reliable)
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Arrays.asList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        Reader reader = new MultiFormatReader();

        System.out.println("Scanning... move a QR code in front of the camera.");

        // 4) Loop and scan frames
        try {
            while (window.isShowing()) {
                // get image from webcam
                BufferedImage image = webcam.getImage();
                if (image == null) {
                    // no image available right now
                    Thread.sleep(50);
                    continue;
                }

                // try decode
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    Result result = reader.decode(bitmap, hints);
                    if (result != null) {
                        System.out.println("Decoded QR Code: " + result.getText());
                        // show popup and break
                        JOptionPane.showMessageDialog(window, "QR Detected:\n" + result.getText());
                        break;
                    }
                } catch (NotFoundException nfe) {
                    // no QR code found in this frame; ignore and continue
                } catch (FormatException | ChecksumException e) {
                    // decoding failed for this frame; continue scanning
                }

                // small pause to reduce CPU usage
                Thread.sleep(100);
            }
        } catch (InterruptedException ignored) {
        } finally {
            // 5) cleanup
            if (webcam.isOpen()) {
                webcam.close();
            }
            window.dispose();
            System.out.println("Scanner stopped.");
        }
    }
}

