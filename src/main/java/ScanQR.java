import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScanQR {

    public static void main(String[] args) {
        // Option A: hardcode full path (use actual filename + extension)
        // File qrFile = new File("D:\\Java project\\AttendSys\\qrcode_1.jpg");

        // Option B: accept path from args (recommended)
        File qrFile;
        if (args.length > 0) {
            qrFile = new File(args[0]);
        } else {
            // fallback path you tried
            qrFile = new File("D:\\Java project\\AttendSys\\qrcode_2.png");
        }

        System.out.println("Program working dir: " + new File(".").getAbsolutePath());
        System.out.println("Checking file: " + qrFile.getPath());
        System.out.println("Absolute path: " + qrFile.getAbsolutePath());
        System.out.println("Exists? " + qrFile.exists());
        System.out.println("Is file? " + qrFile.isFile());
        System.out.println("Can read? " + qrFile.canRead());

        if (!qrFile.exists() || !qrFile.isFile()) {
            // helpful debug: list files in parent directory
            File parent = qrFile.getParentFile();
            if (parent != null && parent.exists() && parent.isDirectory()) {
                System.out.println("Listing files in parent folder: " + parent.getAbsolutePath());
                File[] children = parent.listFiles();
                if (children != null) {
                    for (File f : children) {
                        System.out.printf(" - %s   (isFile=%b)\n", f.getName(), f.isFile());
                    }
                } else {
                    System.out.println("Could not list parent directory (maybe permission issue).");
                }

                // fallback: try first image file in folder (helps you test quickly)
                for (File f : children) {
                    if (f.isFile()) {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                            System.out.println("Using fallback image: " + f.getAbsolutePath());
                            qrFile = f;
                            break;
                        }
                    }
                }
            } else {
                System.err.println("Parent directory not found or not a directory: " + (parent == null ? "null" : parent.getAbsolutePath()));
            }

            if (!qrFile.exists() || !qrFile.isFile()) {
                System.err.println("QR code image file not found at: " + qrFile.getAbsolutePath());
                return;
            }
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(qrFile);
            if (bufferedImage == null) {
                System.err.println("ImageIO.read returned null — file isn't a supported image format or is corrupted.");
                return;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap);
            System.out.println("Decoded QR Code: " + result.getText());

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
        } catch (ReaderException e) {
            System.err.println("Error decoding QR code: " + e.getMessage());
        }
    }
}
