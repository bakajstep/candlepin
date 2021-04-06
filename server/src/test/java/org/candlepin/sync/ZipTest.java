/*
 *  Copyright (c) 2009 - ${YEAR} Red Hat, Inc.
 *
 *  This software is licensed to you under the GNU General Public License,
 *  version 2 (GPLv2). There is NO WARRANTY for this software, express or
 *  implied, including the implied warranties of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 *  along with this software; if not, see
 *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 *  Red Hat trademarks are not licensed under GPLv2. No permission is
 *  granted to use or replicate Red Hat trademarks that are incorporated
 *  in this software or its documentation.
 */

package org.candlepin.sync;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTest {

    private static final Logger log = LoggerFactory.getLogger(ZipTest.class);

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    private Path mkpath(String path) {
        return this.fileSystem.getPath(path);
    }

    void touch(Path path, String fileName) {
        Path filePath = path.resolve(fileName);
        try {
            Files.createFile(filePath);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    void mkdirs(Path path) {
        try {
            Files.createDirectories(path);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Test
    void name() throws IOException {
        Path source = mkpath("/source");
        mkdirs(source);
        touch(source, "file1.txt");
        touch(source, "file2.txt");
        System.out.println(Files.list(source).count());
        Path target = mkpath("/target");
        mkdirs(target);
        System.out.println(Files.list(target).count());

        makeArchive("asd", target, source);

        System.out.println(Files.list(target).count());
        Files.list(target).forEach(System.out::println);
    }

    /**
     * Create a tar.gz archive of the exported directory.
     *
     * @param exportDir Directory where Candlepin data was exported.
     * @return File reference to the new archive zip.
     */
    private Path makeArchive(String consumerUuid, Path tempDir, Path exportDir)
        throws IOException {
        String exportFileName = String.format("%s-%s.zip", consumerUuid, exportDir.getFileName());
        log.info("Creating archive of " + exportDir.toAbsolutePath() + " in: " +
            exportFileName);

        Path archive = createZipArchiveWithDir(tempDir, exportDir, "consumer_export.zip",
            "Candlepin export for " + consumerUuid);

        final byte[] bytes = Files.readAllBytes(archive);
        Path signedArchive = createSignedZipArchive(
            tempDir, archive, exportFileName,
            getSHA256WithRSAHash(bytes),
            "signed Candlepin export for " + consumerUuid);

        log.debug("Returning file: " + archive.toAbsolutePath());
        return signedArchive;
    }

    private Path createZipArchiveWithDir(Path tempDir, Path exportDir,
        String exportFileName, String comment)
        throws IOException {

        Path archive = tempDir.resolve(exportFileName);
//        File archive = new File(tempDir, exportFileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            out.setComment(comment);
            addFilesToArchive(out, exportDir);
        }
        return archive;
    }

    private Path createSignedZipArchive(
        Path tempDir, Path toAdd,
        String exportFileName, byte[] signature, String comment) throws IOException {

//        File archive = new File(tempDir, exportFileName);
        Path archive = tempDir.resolve(exportFileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            out.setComment(comment);
            addFileToArchive(out, toAdd);
            addSignatureToArchive(out, signature);
        }
        return archive;
    }

    private void addFilesToArchive(ZipOutputStream out, Path directory) {
        list(directory).forEach(path -> {
            if (Files.isDirectory(path)) {
                addFilesToArchive(out, path);
            }
            else {
                addFileToArchive(out, path);
            }
        });
//        for (File file : directory.listFiles()) {
//            if (file.isDirectory()) {
//                addFilesToArchive(out, charsToDropFromName, file);
//            } else {
//                addFileToArchive(out, charsToDropFromName, file);
//            }
//        }
    }

    private void addFileToArchive(ZipOutputStream out, Path file) {
        try {
            addFileToArchive3(out, file);
        }
        catch (IOException e) {
            throw new RuntimeException("fail", e);
        }
    }

    private Stream<Path> list(Path directory) {
        try {
            return Files.list(directory);
        }
        catch (IOException e) {
            throw new RuntimeException("fail", e);
        }
    }

    private void addFileToArchive2(ZipOutputStream out, Path file) throws IOException {
        log.debug("Adding file to archive: " + file.getFileName());
        out.putNextEntry(new ZipEntry(file.getFileName().toString()));
        try (FileInputStream in = new FileInputStream(file.toString())) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.closeEntry();
        }
    }

    private void addFileToArchive3(ZipOutputStream out, Path file) throws IOException {
        log.debug("Adding file to archive: " + file.getFileName());
        out.putNextEntry(new ZipEntry(file.getFileName().toString()));
        Files.copy(file, out);
        out.closeEntry();
//        try (FileInputStream in = new FileInputStream(file.toString())) {
//            byte[] buf = new byte[1024];
//            int len;
//            while ((len = in.read(buf)) > 0) {
//                out.write(buf, 0, len);
//            }
//            out.closeEntry();
//        }
    }

    private void addSignatureToArchive(ZipOutputStream out, byte[] signature)
        throws IOException {

        log.debug("Adding signature to archive.");
        out.putNextEntry(new ZipEntry("signature"));
        out.write(signature, 0, signature.length);
        out.closeEntry();
    }

    private byte[] getSHA256WithRSAHash(byte[] archiveInputStream) {
        return new byte[0];
    }

}