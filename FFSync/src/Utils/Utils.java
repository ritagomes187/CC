package Utils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/* Esta classe contem um conjunto de metodos necessarios para input e output, bem como
outras tarefas de manuseamento e comparaçao de estruturas de dados, que as classes principais necessitam */


public class Utils {
    public static String getOperatingSystem(){
        //para conseguir lidar com windows/linux
        String osName = System.getProperty("os.name");
        if(osName.contains("Window"))
            return "Windows";
        else
            return "Linux";
    }

    public static List<String> readFile(String fileName) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch(IOException exc) {
            lines = new ArrayList<>();
        }
        return lines;
    }

    public static byte[] readFileBlock (String fileName, int offset, int numBytes){
        try{
            //abre o ficheiro para leitura
            RandomAccessFile file = new RandomAccessFile(fileName, "r");

            //coloca a ler na posicao do offset
            file.seek(offset);

            if(file.length()-offset < numBytes){
                int size = (int)file.length()-offset;
                //cria buffer para colocar os dados lidos
                byte[] buffer = new byte[size];
                int bytesRead = file.read(buffer);

                file.close();

                //coloca tag no final do buffer
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                output.write(buffer);
                output.write((""+'\0').getBytes(StandardCharsets.UTF_8));

                return output.toByteArray();
            }else{
                //cria buffer para colocar os dados lidos
                byte[] buffer = new byte[numBytes];
                int bytesRead = file.read(buffer);

                file.close();

                return buffer;
            }


        } catch(IOException e){
            System.out.println("Não abriu o datastream!");
            return null;
        }
    }

    public static void writeFile(String fileName, String data){
        try{
            Files.writeString(Paths.get(fileName), data + "\n", CREATE, APPEND);
        } catch(IOException ignored){
        }
    }

    public static void writeFileBlock (String fileName, byte[] data,
                                       int iNumSequencia, int iFlagUltimo)
            throws IOException {
        //tenta abrir o ficheiro passado
        File file = new File(fileName);

        if(iNumSequencia==0){
            //verifica se o ficheiro existe, caso contrário cria as diretorias
            //parentes daquela onde o ficheiro deve estar
            if (!file.exists()){
                if(!file.getParentFile().mkdirs() || file.createNewFile()) {
                    //não conseguiu criar as pastas acima, logo é melhor dar erro
                    //para ver o que se passa
                    System.out.println("a pasta não existe e não conseguiu criar");
                    throw new IOException();
                }
                System.out.println("a pasta não existe mas conseguiu criar");
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.close();
        }else{
            //abre o ficheiro para escrita
            RandomAccessFile fileToWrite = new RandomAccessFile(fileName, "rw");

            //coloca a ler na posicao do offset
            fileToWrite.seek(iNumSequencia);

            if(iFlagUltimo==1){
                //encontra o \0 para só escrever até aí
                int pos = data.length;
                for(int i=0 ; i< data.length ; i++){
                    char temp = (char)data[i];
                    if(temp == '\0'){
                        pos = i;
                        break;
                    }
                }
                //escrever os dados
                fileToWrite.write(Arrays.copyOfRange(data,0,pos));
            }else{
                //escrever os dados
                fileToWrite.write(data);
            }

            fileToWrite.close();
        }


    }

    public static String getKey(List<String> list, String ip){
        String key = "";
        for(String s : list){
            String[] lineSplit = s.split(",",2);
            if(lineSplit[0].equals(ip)){
                key = lineSplit[1];
                break;
            }
        }
        return key;
    }

    public static Map<String,String> getKeys(String fileName){
        Map<String,String> keys = new HashMap<>();
        List<String> fileContent = readFile(fileName);
        for(String s : fileContent){
            String[] lineSplit = s.split(",",2);
            keys.put(lineSplit[0],lineSplit[1]);
        }
        return keys;
    }

    /**
     * devolve a lista de ficheiros normais (ignora as pastas) dentro da pasta passada
     * a lista terá filename, filesize (e.g. "file1.txt,21" sendo o tamanho de file1 igual a 21 bytes)
     * @param folderID cuja lista de ficheiros se pretende
     * @return lista dos ficheiros na pasta ou lista vazia se a pasta não existir ou estiver vazia
     */
    public static String fileList(String folderID){
        String os = getOperatingSystem();
        File folder;
        if(os.equals("Windows"))
            folder = new File(System.getProperty("user.dir") + "\\" + folderID);
        else
            folder = new File(System.getProperty("user.dir") + "/" + folderID);

        File[] listOfFiles = folder.listFiles();

        StringBuilder sb = new StringBuilder();

        if(listOfFiles != null){
            for(File file : listOfFiles) {
                if(file.isFile()) {
                    if(os.equals("Windows"))
                        sb.append(folderID).append("\\");
                    else
                        sb.append(folderID).append("/");
                    sb.append(file.getName()).append(",")
                            .append(file.lastModified()).append(",")
                            .append(file.length()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static List<String> compareLists(String[] other, String[] mine){
        List<String> result = new ArrayList<>();
        //insere a minha lista num hashmap para acelerar procura
        HashMap<String,Long> myList = new HashMap<>();

        for(String s : mine){
            String[] split = s.split(",",3);
            myList.put(split[0],Long.valueOf(split[1]));
        }

        //percorre a outra lista e compara
        for(String s : other){
            String[] split = s.split(",",3);
            String fileID = split[0];
            String fileLength = split[2];
            Timestamp fileLastModified = new Timestamp(Long.parseLong(split[1]));

            if(myList.containsKey(fileID)){
                Timestamp myfileLastModified = new Timestamp(myList.get(fileID));
                if(fileLastModified.after(myfileLastModified))
                    result.add(fileID + "," + fileLength);
            }else{
                result.add(fileID + "," + fileLength);
            }
        }

        return result;
    }

    public static String ipAddress() {
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for(NetworkInterface i : Collections.list(interfaces)) {
                if (!i.isLoopback() || !i.isUp()) {
                    Enumeration<InetAddress> inetAddresses = i.getInetAddresses();

                    for(InetAddress j : Collections.list(inetAddresses) ) {
                        if (j instanceof Inet4Address)
                            ip = j;
                    }
                }
            }
            if(ip != null)
                return ip.toString();
            else
                return "";
        }catch (SocketException e){
            return "";
        }
    }
}
