package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    // DB connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/collage";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Coder@1122";

    public static void main(String[] args) {
        System.out.println("Starting application...");

        // Query to fetch the value to encode in the QR code.
        // Change table/column/query as needed.
        String query = "SELECT status FROM attendance WHERE RollNum = 1";

        // Fetch data from DB (returns null if nothing found or on error)
        String dataToEncode = fetchDataFromDatabase(DB_URL, DB_USER, DB_PASSWORD, query);

        if (dataToEncode == null || dataToEncode.trim().isEmpty())
        {
            System.out.println("No data fetched from DB; using fallback URL.");
            dataToEncode = "https://example.com";
        } else {
            System.out.println("Data fetched from DB: " + dataToEncode);
        }

        // --- QR code generation ---
        int width = 300;
        int height = 300;
        String filePath = "qrcode.png";

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // high error correction
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(dataToEncode, BarcodeFormat.QR_CODE, width, height, hints);

            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("✅ QR code saved to: " + path.toAbsolutePath());
        } catch (WriterException | IOException e) {
            System.out.println("❌ Failed to generate QR code");
            e.printStackTrace();
        }
    }

    /**
     * Fetches a single string value from the database using the provided SQL query.
     * The query should select exactly one column (first column will be read).
     *
     * @return the fetched string or null on error / no-row
     */
    private static String fetchDataFromDatabase(String url, String user, String password, String sqlQuery) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("✅ Connected to the database successfully!");

            if (rs.next()) {
                return rs.getString(1); // first column of result set
            } else {
                System.out.println("⚠️ Query returned no rows.");
                return null;
            }

        } catch (SQLException e) {
            System.out.println("❌ Database operation failed!");
            e.printStackTrace();
            return null;
        }
    }
}
