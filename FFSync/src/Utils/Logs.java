package Utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/* A classe Logs contem um conjunto de entidades Log, uma por cada ficheiro solicitado por cada uma das 
maquinas que estejam ligadas ao serviço FolderFastSync e a comunicar com a maquina actual. */

public class Logs {
    private ReentrantLock logsLock;

    // map para os logs, contém:
    // IP da máquina que vai enviar o ficheiro
    // tinha tinham SIZETOTAL e já recebi SIZERECEBIDO
    // ipOrigem, map<fullPathficheiros,(startTime, endTime, tamanhoTotal, tamanhoRecebido)>
    /*
        10.1.1.1
                    /pasta1/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30
                    /pasta1/subpasta/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30
                    /pasta1/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30
        10.3.3.1
                    /pasta1/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30
                    /pasta1/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30
                    /pasta1/file.txt    2021-12-12 10:00:11/2021-12-12 10:00:11/130/30

    NOTA: Assim, por exemplo a pasta ”/folder1” estara na raiz
         e a pasta ”/folder1/folder2” e uma sub-pasta da anterior.
     */
    private Map<String, Map<String, Log>> logs;

    public Logs(){
        this.logsLock = new ReentrantLock();
        this.logs = new HashMap<>();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        //logs header
        sb.append("SenderIP, FileFullPath, TimeStart, TimeEnd, TimeTotal, SizeTotal, SizeTransferred, TransferRate\n");

        try{
            this.logsLock.lock();
            //logs content
            for(Map.Entry<String, Map<String, Log>> e : this.logs.entrySet()){
                sb.append(e.getKey()).append(", ");
                for(Map.Entry<String, Log> r : e.getValue().entrySet()){
                    sb.append(r.getKey()).append(", ");
                    sb.append(r.getValue().toString()).append("\n");
                }
            }
        }finally {
            this.logsLock.unlock();
        }

        return sb.toString();
    }

    public void insertLog(String ipOrigem, String fullPathficheiros, int tamanhoTotal){
        Log logDetails = new Log();
        logDetails.setSizeTotal(tamanhoTotal);

        HashMap<String, Log> newLog = new HashMap<>();
        newLog.put(fullPathficheiros,logDetails);

        try{
            this.logsLock.lock();
            this.logs.put(ipOrigem,newLog);
        }finally {
            this.logsLock.unlock();
        }

    }

    public void updateLog(String ipSource, String fileFullPath, int sizeNewBlock, int flagLastBlock){
        try{
            this.logsLock.lock();
            this.logs.get(ipSource).get(fileFullPath).updateLog(sizeNewBlock, flagLastBlock);
        }finally {
            this.logsLock.unlock();
        }
    }

    public byte[] getLogs() {
        return this.toString().getBytes(StandardCharsets.UTF_8);
    }
}
