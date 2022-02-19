import FTRapid.FTRapid;
import Utils.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/* A classe FFSyncWorkerUDP trata de processar os pacotes UDP recebidos,
solicitar a classe FTRapid a sua descodificaçao e, por fim, trata do envio de um
novo pacote UDP com a resposta que a classe FTRapid definiu */

public class FFSyncWorkerUDP implements Runnable {
    private DatagramPacket packet;
    private final FTRapid protocolo;
    private final String ipHost;
    private final int portFTRapid;
    private final String pathLogsFile;
    private final String folderToSync;
    private final String ipDest;
    private final Map<String,String> keys;

    //construtor a usar quando é para enviar a mensagem inicial
    public FFSyncWorkerUDP(Map<String,String> keys, String keyHost,
                           String ipHost, int portFTRapid, String pathLogsFile,
                           String pasta, String ipDest, Logs logs){
        this.packet = null;
        this.protocolo = new FTRapid(keys,keyHost, logs);
        this.ipHost = ipHost;
        this.portFTRapid = portFTRapid;
        this.pathLogsFile = pathLogsFile;
        this.folderToSync = pasta;
        this.ipDest = ipDest;
        this.keys = keys;
    }

    //construtor a usar sempre que recebe uma mensagem nova
    public FFSyncWorkerUDP(DatagramPacket packet, Map<String,String> keys, String keyHost,
                           String ipHost, int portFTRapid, String pathLogsFile, Logs logs){
        this.packet = packet;
        this.protocolo = new FTRapid(keys,keyHost, packet.getData(), logs);
        this.ipHost = ipHost;
        this.portFTRapid = portFTRapid;
        this.pathLogsFile = pathLogsFile;
        this.folderToSync = "";
        this.ipDest = "";
        this.keys = keys;
    }

    @Override
    public void run() {
        //o packet só será nulo se for o envio da mensagem inicial
        if(this.packet != null){
            //cria variaveis com ip e porta de quem enviou a mensagem
            InetAddress ipReceived = packet.getAddress();
            int portReceived = packet.getPort();

            //adiciona esta informação aos logs
            //  sourceIP       dastIP port               timestamp size1
            //  10.3.3.1     10.1.1.1 8888 2021-12-12 23:51:27.492    25
            String log = String.format("%s,%s,%s,%s,%s,%s",
                    ipReceived,portReceived,
                    this.ipHost,this.portFTRapid,LocalDateTime.now(),packet.getLength());
            Utils.writeFile(pathLogsFile,log);

            //analisa os dados recebidos e decide o que fazer
            //o parseMessage retorna uma lista com as várias mensagens
            //que deverão ser enviadas em resposta à mensagem recebida
            List<byte[]> msgEnviar = protocolo.parseMessage(ipReceived.toString());

            //se a mensagem criada for vazia, significa que não é para fazer mais nada
            //caso contrario será para enviar a mensagem
            for(byte[] msg : msgEnviar){
                if(msg.length > 0) {
                    sendPacket(msg, ipReceived, portFTRapid);
                }
            }
        }else{
            //obtem a chave da maquina de destino
            String keyDest = this.keys.get(this.ipDest);
            //pede ao protocolo a mensagem de inicio a enviar
            System.out.println("antes\n" + protocolo.toString());
            byte[] msg = protocolo.messageConectionStart(this.folderToSync, keyDest);
            System.out.println("depois\n" + protocolo.toString());
            System.out.println(new String(msg,0,msg.length));

            try {
                sendPacket(msg, InetAddress.getByName(this.ipDest), portFTRapid);
            } catch (UnknownHostException e){ // nao tem endereço correto (InetAddress addr)
                e.printStackTrace();
            }
        }
    }

    public void sendPacket(byte[] msg, InetAddress ip, int port){ //UDP
        try {
            //Monta o pacote a ser enviado
            packet = new DatagramPacket(msg,msg.length, ip, port);
            //abre o socket na porta de envio
            DatagramSocket udpSocket = new DatagramSocket();
            //Envia a mensagem
            udpSocket.send(packet);

            //adiciona esta informação aos logs
            //  sourceIP       dastIP port               timestamp size1
            //  10.3.3.1     10.1.1.1 8888 2021-12-12 23:51:27.492    25
            String log = String.format("%s,%s,%s,%s,%s,%s",
                    ipHost,udpSocket.getPort(),
                    ip,port,LocalDateTime.now(),msg.length);
            Utils.writeFile(pathLogsFile,log);

            //Fecha o DatagramSocket
            udpSocket.close();
        } catch (IOException e){
            e.printStackTrace(); // não consegue enviar udpSocket.send(packet);
        }
    }
}
