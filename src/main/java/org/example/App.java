package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // --- Database connection ---
        String url = "jdbc:mysql://localhost:3306/collage";
        String user = "root";
        String password = "Coder@1122";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ Connected to the database successfully!");
            // ... do DB operations here ...
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed!");
            e.printStackTrace();
        }

        // --- QR code generation ---
        String data = "https://example.com";   // text or URL to encode
        int width = 300;
        int height = 300;
        String filePath = "qrcode.png";

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // high error correction
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("✅ QR code saved to: " + path.toAbsolutePath());
        } catch (WriterException | IOException e) {
            System.out.println("❌ Failed to generate QR code");
            e.printStackTrace();
        }
    }
}
