package com.stride.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

/** Draws a QR code as SVG: crisp on a projector at any size, and tiny to send. */
public final class QrCodes {

    private QrCodes() {
    }

    public static String svg(String text) {
        try {
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
            int w = m.getWidth();
            int h = m.getHeight();
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (m.get(x, y)) {
                        int run = 1;
                        while (x + run < w && m.get(x + run, y)) run++;
                        path.append('M').append(x).append(',').append(y).append('h').append(run).append("v1h-")
                                .append(run).append('z');
                        x += run - 1;
                    }
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + w + " " + h
                    + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code for checking in\">"
                    + "<rect width=\"" + w + "\" height=\"" + h + "\" fill=\"#fff\"/>"
                    + "<path fill=\"#000\" d=\"" + path + "\"/></svg>";
        } catch (WriterException e) {
            throw new IllegalStateException("Could not draw the QR code", e);
        }
    }
}
