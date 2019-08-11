package hello.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
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
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import javax.json.JsonArray;
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
        String line; // Содержимое обрабатываем строки
        int n=0; // Переменная определения номера раздела
        int a=0; // Корректирующая номер раздела переменная
        int StrCount=0; // Хранит номер строки
        int MaxSelection=0; // Количество уровней абзацев
        JSONArray arr = new JSONArray();
        JSONArray arr2 = new JSONArray();;
        ArrayList<Integer> IndexUp = new ArrayList();
        ArrayList<Integer> NumContents = new ArrayList(); // Строчный массив для хранения номеров разелов
        ArrayList<String> Content = new ArrayList(); // Строчный массив для хранения строк с разделами
        ArrayList<Integer> NumStrStart = new ArrayList();
        ArrayList<Integer> NumStrOver = new ArrayList();
        //Цикл считывающий текстовый файл
        while ((line = Text.readLine()) != null){
            StrCount++;
            //Проверка является ли строка абзацем
            if (line.charAt(0)==SIMBOL){
                NumStrStart.add(StrCount);
                NumStrOver.add(StrCount);
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
                if(MaxSelection<n){MaxSelection=n;}
                IndexUp.add(n);
                n=0;
                Content.add(line);
            }
        }
        System.out.println(IndexUp);
        System.out.println(Content);
        System.out.println(MaxSelection);
        //Создание объекта json
        // Запись Результатов в файл
        FileWriter writer = new FileWriter(InputFile);
        for (int i = 0; i < Content.size(); i++){
            JSONObject obj = new JSONObject();
            obj.put("Название раздела",Content.get(i));//Кодирование объекта json
            obj.put("Первая страница", NumStrStart.get(i));
            obj.put("Последняя страница",NumStrOver.get(i));
            obj.put("Размер раздела",NumStrStart.get(i)-NumStrOver.get(i));
            arr.add(obj);
            // if(i == Content.size()-1){writer.write(obj+"");} else{writer.write(obj+System.lineSeparator());}
            //System.out.println(Content.get(i));
        }


        writer.write(arr.toJSONString());
        writer.close();
        int t=0; // триггер
        // Цикл идет пока есть элементы
        while (IndexUp.indexOf(MaxSelection)>=0 && MaxSelection>1){
            //System.out.println(IndexUp.get(IndexUp.indexOf(MaxSelection)-1)+"           "+IndexUp.get(IndexUp.indexOf(MaxSelection)));
            if(IndexUp.get(IndexUp.indexOf(MaxSelection)-1)<IndexUp.get(IndexUp.indexOf(MaxSelection))){

                if(t!=IndexUp.indexOf(MaxSelection)){arr2.clear();}
                t=IndexUp.indexOf(MaxSelection);
                JSONObject obj1 = new JSONObject((Map) arr.get(IndexUp.indexOf(MaxSelection)-1));
                System.out.println(arr.get(IndexUp.indexOf(MaxSelection)-1));
                JSONObject obj2 = new JSONObject((Map) arr.get(IndexUp.indexOf(MaxSelection)));
                System.out.println(arr.get(IndexUp.indexOf(MaxSelection)));
                arr2.add(obj2);
                obj1.put("Подраздел", arr2);
                System.out.println(IndexUp.get(IndexUp.indexOf(MaxSelection)));
                arr.set(IndexUp.indexOf(MaxSelection)-1,obj1);
                arr.remove(IndexUp.indexOf(MaxSelection));
                IndexUp.remove(IndexUp.indexOf(MaxSelection));
                System.out.println(arr);
            }
            if(IndexUp.indexOf(MaxSelection)<0 ){MaxSelection--;}
        }


        /*

        Пока(индекс(максуровень)<0)
        Массив.индекс(максуровень)-1 вставить Массив.индекс(максуровень)
        Удалить найденый макс индекс
        Удалить json из массива
        Если(индекс(максуровень)<0)
        максуровень--

JSONObject obj3 = new JSONObject((Map) arr.get(1));
        obj3.put("Подраздел", arr);
        System.out.println(obj3);

*/

    }

}
