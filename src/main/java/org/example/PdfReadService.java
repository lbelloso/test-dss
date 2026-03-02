package org.example;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PdfReadService {
    private static final String BIO_SIGNATURE_PREFIX = "biofirma-b64:";
    private static final String EVIDENCE_ID_PREFIX = "evidencia-id:";

    public PdfSignatureDetails getPdfSignatureDetails(Path signedPdfPath) throws IOException {
        Objects.requireNonNull(signedPdfPath, "signedPdfPath no puede ser null");
        if (!Files.exists(signedPdfPath)) {
            throw new IllegalArgumentException("No existe el PDF: " + signedPdfPath);
        }

        long fileSizeBytes = Files.size(signedPdfPath);
        try (PdfReader reader = new PdfReader(signedPdfPath.toString())) {
            int pageCount = reader.getNumberOfPages();
            AcroFields fields = reader.getAcroFields();
            List<String> signatureNames = fields.getSignatureNames();

            if (signatureNames == null || signatureNames.isEmpty()) {
                return new PdfSignatureDetails(
                        signedPdfPath.toAbsolutePath().toString(),
                        fileSizeBytes,
                        pageCount,
                        0,
                        Collections.emptyList(),
                        Collections.emptyList()
                );
            }

            List<SignatureMetadata> signatures = signatureNames.stream()
                    .map(signatureName -> buildSignatureMetadata(fields, signatureName))
                    .toList();

            return new PdfSignatureDetails(
                    signedPdfPath.toAbsolutePath().toString(),
                    fileSizeBytes,
                    pageCount,
                    signatureNames.size(),
                    signatures,
                    signatureNames
            );
        }
    }

    private static SignatureMetadata buildSignatureMetadata(AcroFields fields, String signatureName) {
        PdfDictionary signatureDictionary = fields.getSignatureDictionary(signatureName);
        String reason = getPdfString(signatureDictionary, PdfName.REASON);
        String location = getPdfString(signatureDictionary, PdfName.LOCATION);
        String contactInfo = getPdfString(signatureDictionary, PdfName.CONTACTINFO);
        String signerName = getPdfString(signatureDictionary, PdfName.NAME);
        return new SignatureMetadata(
                signatureName,
                signerName,
                reason,
                location,
                decodeEvidenceId(contactInfo),
                decodeBioSignature(contactInfo)
        );
    }

    private static String getPdfString(PdfDictionary dictionary, PdfName name) {
        if (dictionary == null) {
            return null;
        }
        return dictionary.getAsString(name) != null ? dictionary.getAsString(name).toUnicodeString() : null;
    }

    private static byte[] decodeBioSignature(String contactInfo) {
        String encoded = extractContactInfoValue(contactInfo, BIO_SIGNATURE_PREFIX);
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        return Base64.getDecoder().decode(encoded);
    }

    private static String decodeEvidenceId(String contactInfo) {
        return extractContactInfoValue(contactInfo, EVIDENCE_ID_PREFIX);
    }

    private static String extractContactInfoValue(String contactInfo, String prefix) {
        if (contactInfo == null || contactInfo.isBlank()) {
            return null;
        }
        String[] parts = contactInfo.split(";");
        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        return null;
    }

    public record PdfSignatureDetails(
            String pdfPath,
            long fileSizeBytes,
            int pageCount,
            int signatureCount,
            List<SignatureMetadata> signatures,
            List<String> signatureNames
    ) {
    }

    public record SignatureMetadata(
            String signatureName,
            String signerName,
            String reason,
            String location,
            String evidenceId,
            byte[] bioSignatureBytes
    ) {
    }
}
