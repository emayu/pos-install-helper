/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.yaque.pos.installhelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.ZipFile;



/**
 *
 * @author yaque
 */
public class InstallHelper {

    private static final Logger LOG = Logger.getLogger(InstallHelper.class.getName());
    static final String BASE_INSTALL_PATH = System.getenv("LOCALAPPDATA")+"\\Unicenta oPos\\";
    
    
    public static void main(String[] args) {
        
        try{
            Config params = analizeArgs(args);
            
            backupCurrentVersion();
            
            //Download
            Path zipPath = downloadZip(params.targetURL, params.keepDownloadedContent);
            
            //Extract
            Path destUncrompress = Paths.get(BASE_INSTALL_PATH);
            unzipFolder(zipPath, destUncrompress);
            
            printHelp();
            
        } catch(IOException e){
            Logger.getLogger(InstallHelper.class.getName()).log(Level.SEVERE, "something went wrong", e);
        }
    }
    
    public static void printHelp(){
        System.out.println("+---------------------------------------------------------------------------------------------+");
        System.out.println(" Remember the place to locate the direct acess is ");
        System.out.println(String.format(" %s%s", System.getenv("APPDATA"),  "\\Microsoft\\Windows\\Start Menu\\Programs"));
        System.out.println(String.format(" The icon is located at: %s%s", BASE_INSTALL_PATH, "reports\\com\\openbravo\\images"));
    }
    
    public static Config analizeArgs(String... args){
        String providedArg;
        if(args == null || args.length == 0){
            LOG.severe("please provide a valid url in the first Argument to download the file");
            System.exit(-1);
            
        }
        providedArg = args[0];
        if("-h".equals(providedArg) || "--help".equals(providedArg)){
            printHelp();
            System.exit(0);
        }
        Config config = null;
        try{
            URL url = new URL(providedArg);
            LOG.info(() -> "Provided " + providedArg);
            config = new Config(url, false);
            if(args.length >= 2 ){
                config.keepDownloadedContent = args[1] != null && args[1].equals("keep");
            }
        }catch(MalformedURLException ex){
            LOG.log(Level.SEVERE, "The provided argument '" + args[0] + "' is not a valid URL", ex);
            System.exit(-1);
        }
        
        return config;
    }
    
    public static void backupCurrentVersion() throws IOException{
        final Path baseInstall = Paths.get(BASE_INSTALL_PATH);
        if(!baseInstall.toFile().exists()){
            return ;
        }
        
        try(DirectoryStream<Path> sourceDirectoryStream = Files.newDirectoryStream(baseInstall, path -> !path.endsWith("bk"))){
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyMMdd"));
            Path bkLocation = baseInstall.resolve("bk" + File.separator + today);
            final var iterator = sourceDirectoryStream.iterator();
            if (iterator.hasNext()) {
                int count = 0;
                do {
                    if (bkLocation.toFile().exists()) {
                        bkLocation = baseInstall.resolve("bk" + File.separator + today + " " + ++count);
                    }else{
                        Files.createDirectories(bkLocation);
                        break;
                    }
                } while (true);
                
                LOG.info("Copying backup files in " + bkLocation);
                do {
                    Path path = iterator.next();
                    LOG.finest("Copying " + path );
                    Path temp = bkLocation.resolve(path.getFileName());
                    Files.move(path, temp, StandardCopyOption.REPLACE_EXISTING);
                } while (iterator.hasNext());
            }
        }
    }
    
    public static Path downloadZip(URL target, boolean keepFile) throws IOException {
        LOG.info("Downloading");
        
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOG.severe("No file to download. Server http code response " + connection.getResponseCode());
            System.exit(-1);
            return null;
        }
        
        String contentType = connection.getContentType();
        if(!contentType.equalsIgnoreCase("application/zip")){
            LOG.severe("The file is not a zip file. Contect-Type " + contentType);
            System.exit(-1);
            return null;
        }
        Instant start = Instant.now();
        ProgressBar progress = new ProgressBar();
        progress.setProgress(0);
        progress.print();
        File tempFile = File.createTempFile("unicenta", ".tmp");
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream());
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            long leftWritten = connection.getContentLengthLong(), length =  leftWritten;
            long chunckSize = 1024 ;
            while (leftWritten > 0) {
                long written = fos.getChannel().transferFrom(readableByteChannel, length - leftWritten, chunckSize);
                leftWritten -= written;
                progress.setProgress( (int) (100 * (length - leftWritten) / length));
                progress.print();
            }
        }
        Instant finish = Instant.now();
        Duration duration = Duration.between(start, finish);
        System.out.println(String.format("Elapsed: %d minutes,  %d seconds (%d millis)", duration.toMinutes(), duration.toSecondsPart(), duration.toMillis()) );
        if (keepFile) {
            LOG.info(() -> " created -> " + tempFile);
        } else {
            tempFile.deleteOnExit();
        }
        return tempFile.toPath();

    }
    
    public static void unzipFolder(Path source, Path target) throws IOException {
        var sTarget = target.toString();
        LOG.info("Uncompressing at " + sTarget);
        new ZipFile(source.toFile())
                .extractAll(sTarget);
    }
}

class Config{
    
    URL targetURL;
    boolean keepDownloadedContent;

    public Config(URL targetURL, boolean keepDownloadedContent) {
        this.targetURL = targetURL;
        this.keepDownloadedContent = keepDownloadedContent;
    }
    
}