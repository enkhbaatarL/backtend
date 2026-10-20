package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.FileRef;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** 파일 한 역할씩 생성/검증/소비 후 삭제한다. source 원본에는 쓰지 않는다. */
final class InputDraftFinalizationFiles {
    /** A의 성공 chain 적용·허용 계산·구조·보존 검증 경계. 모든 역할/모든 성공 revision을 책임진다. */
    interface SuccessfulReplay {
        void applyAndValidate(InputDraftFinalizationService.Job job, FileRef role, Path fixedOriginal, Path target) throws Exception;
    }
    interface GeneratedAction<T> { T use(Path path) throws Exception; }

    private final InputWorkbookReader reader;
    private final InputTempFiles scratch;
    private final SuccessfulReplay replay;

    InputDraftFinalizationFiles(InputWorkbookReader reader, InputTempFiles scratch, SuccessfulReplay replay) {
        this.reader = reader;
        this.scratch = scratch;
        this.replay = replay;
    }

    <T> T withGenerated(InputDraftFinalizationService.Job job, String role, GeneratedAction<T> action) throws Exception {
        FileRef file = job.files().stream().filter(f -> role.equals(f.role())).findFirst().orElseThrow(InputDraftException::internal);
        // withLocal의 저장소 host/회사 bucket/SCNRCASE 검증과 0600 임시 사본 수명을 재사용한다.
        return reader.withLocal(file, job.owner().companyCode(), original -> {
            Path generated = null;
            try {
                generated = scratch.create();
                if (job.draft().revision() == 0) {
                    validatePackage(original);
                    Files.copy(original, generated, StandardCopyOption.REPLACE_EXISTING);
                    if (!digest(original).equals(digest(generated))) throw InputDraftException.internal();
                } else {
                    if (replay == null) throw InputDraftException.conflict("POLICY_UNSUPPORTED", job.draft().revision());
                    replay.applyAndValidate(job, file, original, generated);
                }
                validatePackage(generated);
                return action.use(generated);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new InputDraftFinalizationFailure(ex);
            } finally {
                if (generated != null) try { Files.deleteIfExists(generated); }
                catch (Exception ex) { throw new InputDraftFinalizationFailure(ex); }
            }
        });
    }

    static String digest(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) { return digest(input); }
    }

    static String digest(Path path, Runnable checkpoint) throws Exception {
        try(InputStream input=Files.newInputStream(path)){return digest(input,checkpoint);}
    }
    static String digest(InputStream input) throws Exception {
        return digest(input, () -> {});
    }

    static String digest(InputStream input, Runnable checkpoint) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536];
        int read;
        while ((read = input.read(buffer)) != -1) { checkpoint.run(); digest.update(buffer, 0, read); }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** streaming CRC·크기·OOXML workbook 검증. revision0의 byte 동일성은 모든 수식/저장값/구조 보존도 보장한다. */
    static void validatePackage(Path path) throws Exception {
        validatePackage(path, () -> {});
    }

    static void validatePackage(Path path, Runnable checkpoint) throws Exception {
        checkpoint.run();
        long compressed = Files.size(path);
        if (compressed <= 0 || compressed > 300L * 1024 * 1024) throw InputDraftException.internal();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (zip.size() > 10000) throw InputDraftException.internal();
            Set<String> names = new HashSet<>();
            var entries = zip.entries();
            byte[] buffer = new byte[65536];
            while (entries.hasMoreElements()) {
                checkpoint.run();
                var entry = entries.nextElement();
                String name = entry.getName();
                long maximum = name.equals("xl/sharedStrings.xml") ? 64L * 1024 * 1024
                        : name.equals("xl/styles.xml") ? 8L * 1024 * 1024 : 512L * 1024 * 1024;
                if (!names.add(name) || name.startsWith("/") || name.contains("..") || name.contains("\\")
                        || entry.getSize() < 0 || entry.getSize() > maximum
                        || (entry.getSize() > 102400 && entry.getCompressedSize() > 0
                            && (double) entry.getCompressedSize() / entry.getSize() < 0.01)) throw InputDraftException.internal();
                CRC32 crc = new CRC32();
                long size = 0;
                try (InputStream input = zip.getInputStream(entry)) {
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        checkpoint.run();
                        size += read;
                        if (size > maximum) throw InputDraftException.internal();
                        crc.update(buffer, 0, read);
                    }
                }
                if (size != entry.getSize() || crc.getValue() != entry.getCrc()) throw InputDraftException.internal();
            }
            if (!names.containsAll(Set.of("[Content_Types].xml", "xl/workbook.xml", "xl/_rels/workbook.xml.rels"))) {
                throw InputDraftException.internal();
            }
        }
        try (OPCPackage pkg = OPCPackage.open(path.toFile(), PackageAccess.READ);
                InputStream workbook = new XSSFReader(pkg).getWorkbookData()) {
            var parser = XMLHelper.newXMLReader();
            parser.setContentHandler(new DefaultHandler());
            parser.parse(new InputSource(workbook));
        }
    }

    private static final class InputDraftFinalizationFailure extends RuntimeException {
        InputDraftFinalizationFailure(Exception cause) { super("finalization file processing failed", cause); }
    }
}
