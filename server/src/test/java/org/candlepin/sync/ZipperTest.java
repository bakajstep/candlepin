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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.pki.PKIUtility;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Tag("integration")
public class ZipperTest {

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    void createdArchiveShouldContainerFilesFromSource() throws ExportCreationException, IOException {
        PKIUtility mock = mock(PKIUtility.class);
        when(mock.getSHA256WithRSAHash(any())).thenReturn(new byte[]{});
        Zipper zipper = new Zipper(mock);
        Path source = mkPath("/source");
        mkDirs(source);
        touch(source, "file1.txt");
        touch(source, "file2.txt");
        Path target = mkPath("/target");
        mkDirs(target);

        Path export = zipper.makeArchive("consumer_uuid_1", target, source);

        assertTrue(verifyHasEntry(export, "file1.txt"));
        assertTrue(verifyHasEntry(export, "file2.txt"));
    }

    /**
     * return true if export has an entry with a given name.
     *
     * @param export zip file to inspect
     * @param name entry name
     * @return true if archive contains the entry
     */
    private boolean verifyHasEntry(Path export, String name) throws IOException {
        boolean found = false;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(export))) {
            ZipEntry entry = null;

            while ((entry = zis.getNextEntry()) != null) {
                byte[] buf = new byte[1024];

                if (entry.getName().equals("consumer_export.zip")) {
                    Path exportdata = export.resolveSibling("consumer_export.zip");
                    try (OutputStream os = Files.newOutputStream(exportdata)) {
                        int n;
                        while ((n = zis.read(buf, 0, 1024)) > -1) {
                            os.write(buf, 0, n);
                        }
                    }
                    // open up the zip and look for the metadata
                    found = verifyHasEntry(exportdata, name);
                }
                else if (entry.getName().equals(name)) {
                    found = true;
                }

                zis.closeEntry();
            }

        }
        return found;
    }

    private Path mkPath(String path) {
        return this.fileSystem.getPath(path);
    }

    void touch(Path path, String fileName) throws IOException {
        Path filePath = path.resolve(fileName);
        Files.createFile(filePath);
    }

    void mkDirs(Path path) throws IOException {
        Files.createDirectories(path);
    }

}
