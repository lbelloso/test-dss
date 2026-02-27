package org.example;

import java.nio.file.Path;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws Exception {
        Path inputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pdf");
        Path outputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-tsa.pdf");
        Path pfxPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pfx");

        String pfxPassword = "123456";
        byte[] bioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a8kAAAAASUVORK5CYII=");

        PdfSignService service = new PdfSignService("https://www.freetsa.org/");
        Path generatedPath = service.signPdfWithPfx(inputPath, outputPath, pfxPath, pfxPassword.toCharArray(), bioSignatureBytes);
        System.out.println("PDF firmado y sellado TSA en: " + generatedPath.toAbsolutePath());
    }
}
