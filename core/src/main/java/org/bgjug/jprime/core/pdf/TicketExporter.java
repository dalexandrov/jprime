package org.bgjug.jprime.core.pdf;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.enterprise.context.ApplicationScoped;
import net.sf.jasperreports.engine.JasperReport;
import org.bgjug.jprime.core.model.JPrimeException;

/** Renders the conference ticket PDF and the QR code image that goes into the ticket e-mail. */
@ApplicationScoped
public class TicketExporter {

    static final String LOGO = "ticket/jprime_ticket_logo_2026.png";
    private static final String QR_JSON =
        "{ \"organizer\" : \"%s\", \"event\" : \"%s\", \"type\" : \"%s\", \"ticket\" : \"%s\" }";

    public byte[] exportTicket(TicketData data, InvoiceLanguage language) {
        String resource = language == InvoiceLanguage.BG ? "ticket/ticket_bg.jasper" : "ticket/ticket_en.jasper";
        JasperReport report = JasperSupport.load(resource);
        try (InputStream logo = Thread.currentThread().getContextClassLoader().getResourceAsStream(LOGO)) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("logoStream", logo);
            parameters.put("organizer", data.getOrganizer());
            parameters.put("event", data.getEvent());
            if (data.getLocation() != null) {
                parameters.put("location", data.getLocation());
            }
            if (data.getDateTime() != null) {
                parameters.put("dateTime", data.getDateTime());
            }
            return JasperSupport.exportPdf(report, parameters, new ArrayList<>(data.getDetails()));
        } catch (IOException e) {
            throw new JPrimeException(e);
        }
    }

    /** QR code (PNG) for the first ticket in the data. Same JSON payload as before - the check-in app reads it. */
    public byte[] generateTicketQrCode(TicketData data) {
        TicketDetail detail = data.getDetails().getFirst();
        String payload = String.format(QR_JSON, data.getOrganizer(), data.getEvent(), detail.getType(),
            detail.getNumber());
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 164, 164,
                Map.of(EncodeHintType.CHARACTER_SET, "utf-8"));
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new JPrimeException(e);
        }
    }
}
