import Utils.*;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/*De modo a simplificar a forma de autenticaçao no serviço, foi assumido que
as maquinas em comunicaçao se conheciam. Nomeadamente, foi considerado que
estas partilhavam um ficheiro de configuraçao da aplicaçao (designado config.ftr)
que contem os pares IP-chave de acesso. Assim, sempre que uma maquina recebe
um pedido inicial, tem 1º de verificar que a maquina que enviou o pedido e
de confianca, atraves da validaçao da chave desta na seu ficheiro de configuraçoes local.
*/

public class FFSync {
    public static void main( String[] args ) {
        final int portFTRapid = 8888;
        final String ipHost;
        final int maxSize = 512; //bytes
        final String pathConfigFile = "config.ftr";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        final String pathLogsFile = "logs_" + dtf.format(now);
        final Map<String,String> keys;
        final String keyHost;

        //obtem chaves das maquinas conhecidas
        keys = Utils.getKeys(pathConfigFile);

        //obtem chave da maquina atual
        ipHost = Utils.ipAddress();
        keyHost = keys.get(ipHost.substring(1));

        //inicializa os logs
        Logs logs = new Logs();

        //incializa estrutura para armazenar dados recebidos
        Map<String,byte[]> filesReceived;

        // thread que ficará à escuta de pedidos por TCP
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(portFTRapid)) {
                System.out.println("Socket TCP à escuta na porta " + portFTRapid);
                while (true) {
                    Socket client = serverSocket.accept();

                    // inicializa nova thread para cada pedido que chega
                    new Thread(new FFSyncWorkerTCP(client, logs.getLogs())).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // thread que ficará à escuta de pedidos por UDP
        new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(portFTRapid)) {
                System.out.println("Socket UDP à escuta na porta " + portFTRapid);
                while (true) {
                    //cria buffer onde os dados serão colocados
                    byte[] buf = new byte[maxSize];
                    //cria pacote de dados do tamanho do buffer
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    //recebe a mensagem e coloca no pacote preparado
                    udpSocket.receive(packet);
                    // inicializa nova thread para cada pedido que chega
                    new Thread(new FFSyncWorkerUDP(packet, keys, keyHost,
                            ipHost, portFTRapid, pathLogsFile, logs)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        if(args.length == 2){
            //cria variaveis para pasta e ipDestino
            String pasta = args[0];
            String ipDest = args[1];

            new FFSyncWorkerUDP(keys, keyHost,
                    ipHost, portFTRapid, pathLogsFile, pasta, ipDest, logs).run();
        }
    }
}
