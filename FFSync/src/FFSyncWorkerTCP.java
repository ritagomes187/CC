import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/*A classe FFSyncWorkerTCP limita-se a receber o socket com a ligaçao a maquina 
que fez o pedido HTTP e colocar neste o conte´udo actual da classe Logs*/

public class FFSyncWorkerTCP implements Runnable {
    private final Socket socket;
    private final byte[] logs;

    public FFSyncWorkerTCP(Socket socket, byte[] logs){
        this.socket = socket;
        this.logs = logs;
    }

    @Override
    public void run() {
        // envia resposta com estado do programa
        try {
            //coloca os logs no outputstream do socket
            //se não tiver havido atividade coloca mensagem
            if(logs.length!=0){
                socket.getOutputStream().write(logs);
            }else{
                socket.getOutputStream().write("No activity registered!\n".getBytes(StandardCharsets.UTF_8));
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
