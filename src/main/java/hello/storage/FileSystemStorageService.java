package hello.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.stream.Stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
        int r = new Random().nextInt(100000);
        String filename = StringUtils.cleanPath("id_"+r+"_"+file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.rootLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                .filter(path -> !path.equals(this.rootLocation))
                .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @SuppressWarnings("empty-statement")
    public void FileUpdate(String filename) throws FileNotFoundException, IOException {
        String s = "./upload-dir/" + filename;
        File InputFile = new File(s);
        BufferedReader Text = new BufferedReader(new FileReader(InputFile));
        char SIMBOL= '#'; // Константа определяющая разделов
        String line;
        int n=0;
        int a=0;
        ArrayList<Integer> NumContents = new ArrayList(); // Строчный массив для хранения номеров разелов
        ArrayList<String> Content = new ArrayList(); // Строчный массив для хранения строк с разделами
        //Цикл считывающий текстовый файл
        while ((line = Text.readLine()) != null){
            //Проверка является ли строка абзацем
            if (line.charAt(0)==SIMBOL){
                String GenSimbol = "";
                String NavigationContext ="";
                //Подсчет подразделов
                while (line.charAt(n)==SIMBOL){
                    n++;
                }
                //Определение номера раздела и его заполнение в месте с названием раздела
                if(n >= NumContents.size()){
                    NumContents.add(-1);
                    NumContents.set(n-1, NumContents.get(n-1)+1);
                }else{
                    NumContents.set(n-1, NumContents.get(n-1)+1);
                }
                for(int i = 0; i < n; i++) {
                    GenSimbol+="#";
                    NavigationContext+=(NumContents.get(i)+1)+".";
                }
                if (a != NumContents.get(n-1)){
                    a = NumContents.get(n-1);
                    NumContents.set(n, -1);
                }
                if(NavigationContext.length()>0){
                    NavigationContext=(NavigationContext.substring(0, NavigationContext.length() - 1));
                    line=(line.replaceAll(GenSimbol, NavigationContext));
                }
                n=0;
                Content.add(line);
            }
        }
        // Запись Результатов в файл
        FileWriter writer = new FileWriter(InputFile);
        for (int i = 0; i < Content.size(); i++){
            writer.write(Content.get(i)+System.lineSeparator());
            //System.out.println(Content.get(i));
        }
        writer.close();
    }

}
