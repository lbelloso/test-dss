package org.example;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class PdfSignService {

    private final PAdESService padesService;

    public PdfSignService(String tspServerUrl) {
        Objects.requireNonNull(tspServerUrl, "tspServerUrl no puede ser null");

        OnlineTSPSource tspSource = new OnlineTSPSource(normalizeTspUrl(tspServerUrl));
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();

        this.padesService = new PAdESService(certificateVerifier);
        this.padesService.setTspSource(tspSource);
    }

    public Path signPdfWithPfx(Path inputPdfPath, Path outputPdfPath, Path pfxPath, char[] pfxPassword, byte[] bioSignatureBytes) throws IOException {
        Objects.requireNonNull(inputPdfPath, "inputPdfPath no puede ser null");
        Objects.requireNonNull(outputPdfPath, "outputPdfPath no puede ser null");
        Objects.requireNonNull(pfxPath, "pfxPath no puede ser null");
        Objects.requireNonNull(pfxPassword, "pfxPassword no puede ser null");
        Objects.requireNonNull(bioSignatureBytes, "bioSignatureBytes no puede ser null");

        if (!Files.exists(inputPdfPath)) {
            throw new IllegalArgumentException("No existe el PDF de entrada: " + inputPdfPath);
        }
        if (!Files.exists(pfxPath)) {
            throw new IllegalArgumentException("No existe el certificado PFX: " + pfxPath);
        }
        if (bioSignatureBytes.length == 0) {
            throw new IllegalArgumentException("La biofirma de prueba no puede estar vacia.");
        }

        DSSDocument documentToSign = new FileDocument(inputPdfPath.toFile());
        PAdESSignatureParameters parameters = buildSignatureParameters(bioSignatureBytes);

        try (Pkcs12SignatureToken signingToken = new Pkcs12SignatureToken(
                pfxPath.toFile(),
                new KeyStore.PasswordProtection(pfxPassword))) {

            DSSPrivateKeyEntry privateKey = getFirstPrivateKey(signingToken.getKeys());
            parameters.setSigningCertificate(privateKey.getCertificate());
            parameters.setCertificateChain(privateKey.getCertificateChain());

            ToBeSigned dataToSign = padesService.getDataToSign(documentToSign, parameters);
            SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);

            DSSDocument signedDocument = padesService.signDocument(documentToSign, parameters, signatureValue);
            return writeDocument(signedDocument, outputPdfPath);
        }
    }

    private static PAdESSignatureParameters buildSignatureParameters(byte[] bioSignatureBytes) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
        parameters.setReason("Aprobacion contractual de prueba");
        parameters.setLocation("Valencia - Oficina Central");
        parameters.setContactInfo("biofirma-b64:" + Base64.getEncoder().encodeToString(bioSignatureBytes));
        return parameters;
    }

    private static DSSPrivateKeyEntry getFirstPrivateKey(List<DSSPrivateKeyEntry> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException("El PFX no contiene claves privadas.");
        }
        return keys.get(0);
    }

    private static Path writeDocument(DSSDocument document, Path outputPdfPath) throws IOException {
        Path parent = outputPdfPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            try (OutputStream outputStream = Files.newOutputStream(outputPdfPath)) {
                document.writeTo(outputStream);
            }
            return outputPdfPath;
        } catch (FileSystemException ex) {
            Path fallbackPath = buildFallbackOutputPath(outputPdfPath);
            try (OutputStream outputStream = Files.newOutputStream(fallbackPath)) {
                document.writeTo(outputStream);
            }
            return fallbackPath;
        }
    }

    private static Path buildFallbackOutputPath(Path outputPdfPath) {
        String fileName = outputPdfPath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        String baseName = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        String extension = lastDot > 0 ? fileName.substring(lastDot) : "";
        String suffix = String.valueOf(System.currentTimeMillis());
        String fallbackName = baseName + "-" + suffix + extension;
        Path parent = outputPdfPath.getParent();
        return parent == null ? Path.of(fallbackName) : parent.resolve(fallbackName);
    }

    private static String normalizeTspUrl(String tspServerUrl) {
        String clean = tspServerUrl.trim();
        if (clean.endsWith("/tsr")) {
            return clean;
        }
        if (clean.endsWith("/")) {
            return clean + "tsr";
        }
        return clean + "/tsr";
    }
}
