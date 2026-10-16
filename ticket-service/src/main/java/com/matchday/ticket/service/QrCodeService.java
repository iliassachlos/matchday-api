package com.matchday.ticket.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class QrCodeService {

    private static final int SIZE_PIXELS = 300;

    /**
     *  Generates a PNG image of a QR code for the given text.
     * 
     * @param text - the text to encode in the QR code
     * @return a byte array containing the PNG image data
     */
    public byte[] png(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, SIZE_PIXELS, SIZE_PIXELS);

            ByteArrayOutputStream image = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", image);

            return image.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not draw the QR code", e);
        }
    }
}
