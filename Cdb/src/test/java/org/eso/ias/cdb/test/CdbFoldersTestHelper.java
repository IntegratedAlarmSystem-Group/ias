package org.eso.ias.cdb.test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A collection of helper methods to support the tests
 * 
 * @author acaproni
 *
 */
public class CdbFoldersTestHelper {
	
	public static void deleteFolder(Path folderPath) throws IOException {
		if (!Files.exists(folderPath)) {
			return;
		}
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // delete file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); // delete directory after its contents
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
