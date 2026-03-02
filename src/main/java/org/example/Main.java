package org.example;

import eu.europa.esig.dss.enumerations.CertificationPermission;

import java.nio.file.Path;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws Exception {
        Path inputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pdf");
        Path patientSignedPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-patient-tsa.pdf");
        Path outputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-tsa.pdf");
        Path patientPfxPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pfx");
        Path doctorPfxPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\doctor.pfx");

        String pfxPassword = "123456";
        byte[] patientBioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a8kAAAAASUVORK5CYII=");
        byte[] doctorBioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAEAQH/cetH5QAAAABJRU5ErkJggg==");

        PdfSignService service = new PdfSignService("https://www.freetsa.org/");
        PdfSignService.SignatureRequest patientSignature = new PdfSignService.SignatureRequest(
                "Paciente",
                "Firmado por paciente",
                "Valencia - Consulta",
                "evidencia-paciente-001",
                patientBioSignatureBytes,
                CertificationPermission.MINIMAL_CHANGES_PERMITTED
        );
        PdfSignService.SignatureRequest doctorSignature = new PdfSignService.SignatureRequest(
                "Doctor",
                "Firmado por doctor",
                "Valencia - Consulta",
                "evidencia-doctor-001",
                doctorBioSignatureBytes,
                null
        );

        Path patientGeneratedPath = service.signPdfWithPfx(
                inputPath,
                patientSignedPath,
                patientPfxPath,
                pfxPassword.toCharArray(),
                patientSignature
        );
        Path generatedPath = service.signPdfWithPfx(
                patientGeneratedPath,
                outputPath,
                doctorPfxPath,
                pfxPassword.toCharArray(),
                doctorSignature
        );
        System.out.println("PDF firmado por paciente y doctor en: " + generatedPath.toAbsolutePath());

        PdfReadService readService = new PdfReadService();
        PdfReadService.PdfSignatureDetails details = readService.getPdfSignatureDetails(generatedPath);
        System.out.println("Detalle PDF:");
        System.out.println("  paginas=" + details.pageCount());
        System.out.println("  tamanoBytes=" + details.fileSizeBytes());
        System.out.println("  firmas=" + details.signatureCount());
        for (PdfReadService.SignatureMetadata signature : details.signatures()) {
            int bioLength = signature.bioSignatureBytes() == null ? 0 : signature.bioSignatureBytes().length;
            System.out.println("  firma=" + signature.signatureName());
            System.out.println("    firmante=" + signature.signerName());
            System.out.println("    reason=" + signature.reason());
            System.out.println("    location=" + signature.location());
            System.out.println("    evidenciaId=" + signature.evidenceId());
            System.out.println("    biofirmaBytes=" + bioLength);
        }
    }
}
