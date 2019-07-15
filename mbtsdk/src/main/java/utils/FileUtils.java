package utils;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by Vincent on 19/08/2015.
 */
@Keep
public final class FileUtils {
    private final static String TAG = FileUtils.class.getName();;

    /**
     * Copies an entire directory and all its sub-directories recursively to a specified destination
     * @param source the source directory. If it's a file, it will be directly copied
     * @param destination the destination directory.
     * @throws IOException in case an IO error occurs while writing files
     * @exception ExceptionUtils.NullArg in case one of the arguments is null
     * @see <a href=http://www.mkyong.com/java/how-to-copy-directory-in-java/>Public Source Code</a>
     */
    public static final void copyDirectoryToAnotherLocation(@Nullable File source, @Nullable File destination)
            throws IOException{
        if (source == null)
            throw new ExceptionUtils.NullArg("source");
        if (destination == null)
            throw new ExceptionUtils.NullArg("destination");
        if(source.isDirectory()){

            //if directory not exists, create it
            if(!destination.exists()){
                destination.mkdir();
            }

            //list all the directory contents
            final String files[] = source.list();

            for (final String file : files) {
                //construct the src and dest file structure
                final File srcFile = new File(source, file);
                final File destFile = new File(destination, file);
                //recursive copy
                copyDirectoryToAnotherLocation(srcFile, destFile);
            }

        } else{
            //if file, then copy it
            //Use bytes stream to support all file types
            final InputStream in = new FileInputStream(source);
            final OutputStream out = new FileOutputStream(destination, false);

            final byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    /**
     * Compresses a file with ZIP.
     * @param toCompress the file to be compressed Must not be <code>NULL</code> !
     * @param outPutName the file to be created with the compressed content from the original file
     *                   Note: if a file exists with the same name, it will be deleted first.
     *                   Note: the file extension will be <code>.zip</code> by default. No need to specify it
     * @return the compressed file result of the compression
     * @throws IOException
     * @exception IllegalStateException if the file to compress does not exist or is not accessible
     */
    @NonNull
    public static final File compressFile(@Nullable final File toCompress, final String outPutName) throws IOException {
        if (toCompress == null)
            throw new ExceptionUtils.NullArg("toCompress");
        if (TextUtils.isEmpty(outPutName))
            throw new ExceptionUtils.NullEmptyArg("outPutName");
        if (!toCompress.exists())
            throw new IllegalStateException("toCompress MUST EXIST and be ACCESSIBLE!");

        final File outPutFile =
                new File(toCompress.getPath().replace(toCompress.getName(), "") + outPutName + ".zip");

        if (outPutFile.isDirectory())
            throw new IllegalArgumentException("Error : output result already exist as a directory!");

        final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outPutFile));
        zos.setLevel(Deflater.DEFAULT_COMPRESSION);
        zos.putNextEntry(new ZipEntry(toCompress.getName()));
        final FileInputStream fis = new FileInputStream(toCompress);

        final byte[] buffer = new byte[1024];

        int len = -1;
        while ((len = fis.read(buffer)) > 0)
            zos.write(buffer, 0, len);
        fis.close();
        zos.closeEntry();
        zos.finish();
        zos.close();
        return outPutFile;
    }

    /**
     * Decompresses a ZIPPED file back to its original state and store it next to the compressed version
     * @param toDeCompress the file to be decompressed. Must not be <code>NULL</code> !
     * @return the decompressed file result of the decompression
     * @throws IOException
     * @exception IllegalStateException if the file to decompress is null, does not exist or is not accessible
     */
    @NonNull
    public static final File deCompressFile(@Nullable final File toDeCompress) throws IOException {
        if (toDeCompress == null)
            throw new ExceptionUtils.NullArg("toDeCompress");
        if (!toDeCompress.exists())
            throw new IllegalStateException("the file to be decompressed MUST EXIST and be ACCESSIBLE");


        final ZipInputStream zis = new ZipInputStream(new FileInputStream(toDeCompress));
        final byte[] buffer = new byte[1024];

        final ZipEntry entry = zis.getNextEntry();
        if (entry == null)
            throw new IllegalArgumentException("compressed file is not that of a proper ZIP Archive");

        // Setting output file from ZIP Archive unique entry
        final File outPutFile =
                new File(toDeCompress.getParentFile().getAbsolutePath()
                        + File.separator + entry.getName());

        // Checking if output already exists as a directory
        if (outPutFile.isDirectory())
            throw new IllegalArgumentException("Error : output result already exist as a directory!");

        final FileOutputStream fos = new FileOutputStream(outPutFile);

        int len = -1;
        while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
            fos.flush();
        }
        fos.close();
        zis.closeEntry();
        zis.close();
        return outPutFile;
    }

    /**
     * Extract the file content into a byte array
     * @param filePath is the file to extract
     * @return the file content as a byte array
     */
    public static final byte[] extractFile(@Nullable final String filePath) throws FileNotFoundException {
        return null;
    }
}
