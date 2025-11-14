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

/**
 * Example: fetch attendance rows and create QR codes.
 * - One QR file per row: "qrcode_<RollNum>.png"
 * - Or combine rows into a single QR (uncomment generateSingleQR(...) if needed).
 *
 * Update DB_URL, DB_USER, DB_PASSWORD and SQL as needed.
 */
public class data {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/collage";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Coder@1122";

    public static void main(String[] args) {
        // Example A: generate one QR per row (recommended when many rows)
        generateQRCodesPerRow();

        // Example B: generate a single QR containing concatenated rows (uncomment if needed)
        // generateSingleQRCodeForAllRows();
    }

    private static void generateQRCodesPerRow() {
        String sql = "SELECT RollNum, Name, Branch, FatherName FROM Studentregistration"; // adjust columns if needed

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("Connected to DB, generating QR codes per row...");

            while (rs.next()) {
                // RollNum read (assuming it's numeric in DB)
                Integer rollNumObj = (Integer) rs.getObject("RollNum");
                if (rollNumObj == null) {
                    System.err.println("Skipping row with NULL RollNum");
                    continue;
                }
                int rollNum = rollNumObj;

                // NAME must be read as string (NOT getInt)
                String Name = rs.getString("Name");
                if (Name != null) Name = Name.trim();

                String Branch = rs.getString("Branch");
                if (Branch != null) Branch = Branch.trim();

                String FatherName = rs.getString("FatherName");
                if (FatherName != null) FatherName = FatherName.trim();

                // Build a text payload for the QR code. Change formatting as desired.
                StringBuilder payload = new StringBuilder();
                payload.append("RollNum: ").append(rollNum).append("\n");
                payload.append("Name: ").append(Name == null ? "NULL" : Name).append("\n");
                payload.append("Branch: ").append(Branch == null ? "NULL" : Branch).append("\n");
                payload.append("FatherName: ").append(FatherName == null ? "NULL" : FatherName);

                String fileName = String.format("qrcode_%d.png", rollNum);
                generateQRCodeImage(payload.toString(), 300, 300, fileName);
                System.out.println("Saved QR for RollNum=" + rollNum + " -> " + fileName);
            }

        } catch (SQLException e) {
            System.err.println("Database error while reading rows:");
            e.printStackTrace();
        }
    }

    private static void generateSingleQRCodeForAllRows() {
        String sql = "SELECT RollNum, Name, Branch, FatherName FROM Studentregistration ORDER BY RollNum";

        StringBuilder allRows = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Integer rollNumObj = (Integer) rs.getObject("RollNum");
                if (rollNumObj == null) continue;
                int rollNum = rollNumObj;

                String Name = rs.getString("Name");
                if (Name != null) Name = Name.trim();

                String Branch = rs.getString("Branch");
                if (Branch != null) Branch = Branch.trim();

                String FatherName = rs.getString("FatherName");
                if (FatherName != null) FatherName = FatherName.trim();

                allRows.append("RollNum=").append(rollNum)
                        .append(", Name=").append(Name == null ? "NULL" : Name)
                        .append(", Branch=").append(Branch == null ? "NULL" : Branch)
                        .append(", FatherName=").append(FatherName == null ? "NULL" : FatherName)
                        .append("\n");
            }

            if (allRows.length() == 0) {
                System.out.println("No rows found in attendance; nothing to encode.");
                return;
            }

            // write a single QR with all rows combined
            generateQRCodeImage(allRows.toString(), 500, 500, "qrcode_all_rows.png");
            System.out.println("Saved combined QR as qrcode_all_rows.png");

        } catch (SQLException e) {
            System.err.println("Database error while reading rows:");
            e.printStackTrace();
        }
    }

    /**
     * Uses ZXing to generate a PNG QR file.
     */
    private static void generateQRCodeImage(String text, int width, int height, String filePath) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (WriterException | IOException e) {
            System.err.println("Failed to generate QR code for: " + filePath);
            e.printStackTrace();
        }
    }
}
