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

import org.candlepin.pki.PKIUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    private static final Logger log = LoggerFactory.getLogger(Zipper.class);

    private final PKIUtility pki;

    public Zipper(PKIUtility pki) {
        this.pki = pki;
    }

    /**
     * Create a tar.gz archive of the exported directory.
     *
     * @param exportDir Directory where Candlepin data was exported.
     * @return File reference to the new archive zip.
     */
    public Path makeArchive(String consumerUuid, Path tempDir, Path exportDir)
        throws ExportCreationException {
        String exportFileName = String.format("%s-%s.zip", consumerUuid, exportDir.getFileName());
        log.info("Creating archive of " + exportDir.toAbsolutePath() + " in: " +
            exportFileName);

        Path archive = createZipArchiveWithDir(tempDir, exportDir, "consumer_export.zip",
            "Candlepin export for " + consumerUuid);

        Path signedArchive = createSignedZipArchive(
            tempDir, archive, exportFileName,
            createSignature(archive),
            "signed Candlepin export for " + consumerUuid);

        log.debug("Returning file: " + archive.toAbsolutePath());
        return signedArchive;
    }

    private byte[] createSignature(Path archive) throws ExportCreationException {
        try (InputStream inputStream = Files.newInputStream(archive)) {
            return this.pki.getSHA256WithRSAHash(inputStream);
        }
        catch (IOException e) {
            throw new ExportCreationException("", e);
        }
    }

    private Path createZipArchiveWithDir(Path tempDir, Path exportDir,
        String exportFileName, String comment)
        throws ExportCreationException {

        Path archive = tempDir.resolve(exportFileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            out.setComment(comment);
            addFilesToArchive(out, exportDir);
        }
        catch (IOException e) {
            throw new ExportCreationException("", e);
        }
        return archive;
    }

    private Path createSignedZipArchive(
        Path tempDir, Path toAdd,
        String exportFileName, byte[] signature, String comment) throws ExportCreationException {

        Path archive = tempDir.resolve(exportFileName);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            out.setComment(comment);
            addFileToArchive(out, toAdd);
            addSignatureToArchive(out, signature);
        }
        catch (IOException e) {
            throw new ExportCreationException("", e);
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

    private void addFileToArchive3(ZipOutputStream out, Path file) throws IOException {
        log.debug("Adding file to archive: " + file.getFileName());
        out.putNextEntry(new ZipEntry(file.getFileName().toString()));
        Files.copy(file, out);
        out.closeEntry();
    }

    private void addSignatureToArchive(ZipOutputStream out, byte[] signature)
        throws IOException {

        log.debug("Adding signature to archive.");
        out.putNextEntry(new ZipEntry("signature"));
        out.write(signature, 0, signature.length);
        out.closeEntry();
    }

}