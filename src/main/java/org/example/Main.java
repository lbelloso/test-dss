package org.example;

import java.nio.file.Path;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws Exception {
        Path inputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pdf");
        Path patientSignedPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-patient-tsa.pdf");
        Path doctorSignedPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-doctor-tsa.pdf");
        Path outputPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test-third-tsa.pdf");
        Path patientPfxPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\test.pfx");
        Path doctorPfxPath = Path.of("C:\\Users\\luisb\\Desktop\\test\\doctor.pfx");
        Path thirdSignerPfxPath = patientPfxPath;
        Path patientSignatureImagePath = Path.of("C:\\Users\\luisb\\Desktop\\test\\firma-paciente.png");
        Path doctorSignatureImagePath = Path.of("C:\\Users\\luisb\\Desktop\\test\\firma-doctor.png");
        Path thirdSignatureImagePath = Path.of("C:\\Users\\luisb\\Desktop\\test\\firma-third.png");

        String pfxPassword = "123456";
        byte[] patientBioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a8kAAAAASUVORK5CYII=");
        byte[] doctorBioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAEAQH/cetH5QAAAABJRU5ErkJggg==");
        byte[] thirdBioSignatureBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAAD0lEQVR42mP8z8BQDwAE/wH+R0SxVQAAAABJRU5ErkJggg==");

        PdfSignService service = new PdfSignService("https://www.freetsa.org/");
        PdfSignService.SignatureRequest patientSignature = new PdfSignService.SignatureRequest(
                "Paciente",
                "Firmado por paciente",
                "Valencia - Consulta",
                "evidencia-paciente-001",
                patientBioSignatureBytes,
                patientSignatureImagePath,
                1,
                36,
                742,
                120,
                50
        );
        PdfSignService.SignatureRequest doctorSignature = new PdfSignService.SignatureRequest(
                "Doctor",
                "Firmado por doctor",
                "Valencia - Consulta",
                "evidencia-doctor-001",
                doctorBioSignatureBytes,
                doctorSignatureImagePath,
                1,
                180,
                742,
                120,
                50
        );
        PdfSignService.SignatureRequest thirdSignature = new PdfSignService.SignatureRequest(
                "Firmante prueba",
                "Firmado por firmante de prueba",
                "Valencia - Consulta",
                "evidencia-prueba-001",
                thirdBioSignatureBytes,
                thirdSignatureImagePath,
                1,
                324,
                742,
                120,
                50
        );

        Path patientGeneratedPath = service.signPdfWithPfx(
                inputPath,
                patientSignedPath,
                patientPfxPath,
                pfxPassword.toCharArray(),
                patientSignature
        );
        Thread.sleep(1000);
        Path generatedPath = service.signPdfWithPfx(
                patientGeneratedPath,
                doctorSignedPath,
                doctorPfxPath,
                pfxPassword.toCharArray(),
                doctorSignature
        );
        Thread.sleep(1000);
        Path thirdGeneratedPath = service.signPdfWithPfx(
                generatedPath,
                outputPath,
                thirdSignerPfxPath,
                pfxPassword.toCharArray(),
                thirdSignature
        );
        System.out.println("PDF firmado por paciente, doctor y firmante de prueba en: " + thirdGeneratedPath.toAbsolutePath());

        PdfReadService readService = new PdfReadService();
        PdfReadService.PdfSignatureDetails details = readService.getPdfSignatureDetails(thirdGeneratedPath);
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
