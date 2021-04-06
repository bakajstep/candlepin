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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        PKIUtility mock = mock(PKIUtility.class);
        when(mock.getSHA256WithRSAHash(any())).thenReturn(new byte[]{});
        Zipper zipper = new Zipper(mock);
        Path source = mkpath("/source");
        mkdirs(source);
        touch(source, "file1.txt");
        touch(source, "file2.txt");
        Path target = mkpath("/target");
        mkdirs(target);

        Path export = zipper.makeArchive("asd", target, source);

        assertTrue(verifyHasEntry(export,"file2.txt"));
    }

    /**
     * return true if export has a given entry named name.
     * @param export zip file to inspect
     * @param name entry
     * @return
     */
    private boolean verifyHasEntry(Path export, String name) {
        ZipInputStream zis = null;
        boolean found = false;

        try {
            zis = new ZipInputStream(Files.newInputStream(export));
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
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (zis != null) {
                try {
                    zis.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return found;
    }

}
