package org.eso.ias.cdb.structuredtext;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Formatted text file supported by the CDB
 */
public enum TextFileType {
    JSON(".json"),
    YAML(".yaml");

    /** The extension of the supported type */
    public final String ext;

    private TextFileType(String ext) {
        this.ext=ext;
    }

    /**
     * Return the type of the CDB depending on the extension of
     * the passed file name.
     *
     * @param fileName the file name
     * @return the type corresponding to this file name
     *         or empty if the type has not been recognized
     */
    public static Optional<TextFileType> fromFile(String fileName) {
        Objects.requireNonNull(fileName);
        if (fileName.isEmpty()) {
           throw new IllegalArgumentException("Invalid empty file name");
        }
        // Get the extension of the file
        int pos=fileName.lastIndexOf('.');
        if (pos==-1) {
           throw new IllegalArgumentException("File has no extension: "+fileName);
        }
        String ext=fileName.substring(pos);
        if (ext.compareToIgnoreCase(TextFileType.JSON.ext)==0) {
            return Optional.of(TextFileType.JSON);
        } else if (ext.compareToIgnoreCase(TextFileType.YAML.ext)==0) {
            return Optional.of(TextFileType.YAML);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the type of the CDB from the passed folder.
     *
     * It checks the content of the CDB folder looking for
     * a file named ias.<literal>.<extension></literal>.
     *
     * @param folder The parent folder of the CDB
     * @return the type of the text files in the CDB folder,
     *         or empty if cannot recognize the type of the CDB of the text files
     * @throws IllegalArgumentException if the passed parameter does not correspond to the parent folder
     *                                  of a CDB
     */
    public static Optional<TextFileType> getCdbType(File folder) {
        Objects.requireNonNull(folder);
        if (!folder.isDirectory() || !folder.canRead()) {
            throw new IllegalArgumentException("Not a folder or unreadable: "+folder.getAbsolutePath());
        }
        File cdbFolder = new File(folder.getAbsolutePath()+File.separator+"CDB");
        if (!cdbFolder.isDirectory() || !cdbFolder.canRead()) {
            throw new IllegalArgumentException("The folder "+cdbFolder.getAbsolutePath()+" is unreadable or does not exist");
        }
        // Look for ias.*: there should be only one
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("ias.");
            }
        };
        File[] files = cdbFolder.listFiles(filter);
        if (files.length!=1) {
            return Optional.empty();
        } else {
            return fromFile(files[0].getName());
        }
    }
}
