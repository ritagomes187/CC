package FTRapid;

import Utils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/* classe esta que concretiza o PROTOCOLO de comunicaçao entre instancias diferentes do serviço FolderFastSync. 
A aplicaçao foi pensada desta forma de modo a permitir que, no futuro, possamos alterar o protocolo de comunicaçao sem que
isso cause grandes impactos no funcionamento global da aplicaçao.
Principais funcionalidades desta classe: - descodificaçao dos pacotes de dados recebidos, interpretando correctamente as mensagens que estes transportam, 
                                         - codificaçao de mensagens de resposta adequadas aos pedidos que vao chegando.
*/

/* VARIOS FORMATOS DAS MENSAGENS PROTOCOLARES (INICIO DE CONEXAO, LISTA, DADOS, PEDIDO FICHEIRO) */
public class FTRapid {
    //variáveis instância que constam no protocolo
    private byte[] mensagem; //conteudo dos campos seguintes em bytes
    private int iNumSequencia;
    private int iFlagUltimo;
    private String szData1;
    private String szData2;

    /*Quanto ao campo dos dados, este encontra-se dividido em 2 partes. Uma 1ª parte com indicaçao da pasta a sincronizar. 
    Uma 2ª com indicaçao da chave da maquina de destino de modo a permitir que esta valide que ambas as maquinas se conhecem.
    No final de cada uma das strings que definem a pasta e chave de destino adicionou-se uma marca (\0) 
    para permitir distinguir estes 2 campos. */

    //outras variáveis de instância
    private final Map<String,String> keys;
    private final String keyHost;
    private final Logs logs;

    //construtor chamado para enviar mensagem inicial
    public FTRapid(Map<String,String> keys, String keyHost, Logs logs){
        this.mensagem = null;
        this.iNumSequencia = -3;
        this.iFlagUltimo = -1;
        this.szData1 = "";
        this.szData2 = "";

        this.keys = keys;
        this.keyHost = keyHost;
        this.logs = logs;
    }

    public FTRapid(Map<String,String> keys, String keyHost, byte[] dados, Logs logs){
        this.mensagem = dados;
        this.iNumSequencia = ByteBuffer.
                wrap(Arrays.copyOfRange(dados,0,4)).getInt();
        this.iFlagUltimo = dados[4];
        this.szData1 = "";
        this.szData2 = "";

        this.keys = keys;
        this.keyHost = keyHost;
        this.logs = logs;
    }

    public List<byte[]> parseMessage(String ipSource){
        byte[] dados = this.mensagem;
        List<byte[]> msgEnviar = new ArrayList<>();
        String receivedMessage = new String(dados,5,dados.length-5);
        int[] breakIndex = new int[2];
        int count = 0;
        switch (this.iNumSequencia){
            case -3:
                //chegou um NOVO pedido de conexao
                //percorre a mensagem recebida à procura da posição do \0
                for(int i=0 ; i< receivedMessage.length() ; i++){
                    if(receivedMessage.charAt(i) == '\0')
                        breakIndex[count++] = i;
                    if(count==2)
                        break;
                }

                this.szData1 = receivedMessage.substring(0,breakIndex[0]);

                this.szData2 =receivedMessage.substring(breakIndex[0]+1,breakIndex[1]);

                //confirma chave e prepara mensagem com a lista de ficheiros
                if(this.szData2.equals(keyHost)){
                    byte[] newMessage = messageSendFilesList(Utils.fileList(this.szData1));
                    msgEnviar.add(newMessage);
                }
                break;
            case -2:
                //chegou uma LISTA com ficheiros para eu ver os que preciso
                //percorre o array de bytes à procura da posição do 1º \0 onde termina a lista
                for(int i=0 ; i< receivedMessage.length() ; i++){
                    if(receivedMessage.charAt(i) == '\0')
                        breakIndex[count++] = i;
                    if(count==1)
                        break;
                }

                //na verdade o que é enviado é a lista com nome, tamanho e data da 
                //ultima modificaçao de cada ficheiro. Esta info sera necessaria no destino para 
                //verificar se e ou nao necessario pedir uma copia de cada ficheiro
                this.szData1 = receivedMessage.substring(0,breakIndex[0]);
                String lista = this.szData1;
                //percorrer a lista e ver algum daqueles me faz falta
                if(!lista.isEmpty()){
                    //primeiro ficheiro da lista
                    String[] listaSplit = lista.split("\n",0);
                    String file = listaSplit[0];

                    //pasta a que a lista se refere
                    String[] fileSplit;
                    StringBuilder pastaAux = new StringBuilder();

                    if(Utils.getOperatingSystem().equals("Windows")){
                        fileSplit = file.split("\\\\",0);
                        for(int i=0 ; i<fileSplit.length-1 ; i++)
                            pastaAux.append(fileSplit[i]).append("\\");
                    } else {
                        fileSplit = file.split("/",0);
                        for(int i=0 ; i<fileSplit.length-1 ; i++)
                            pastaAux.append(fileSplit[i]).append("/");
                    }

                    //obter a minha lista daquela pasta
                    String[] myList = Utils.fileList(pastaAux.toString())
                            .split("\n", 0);

                    //se a minha lista estiver vazia é porque a pasta não existe
                    //nesse caso tenho de pedir todos os ficheiros
                    //caso contrario, a pasta existe e tenho de comparar os ficheiros
                    //para saber quais preciso de pedir
                    List<String> filesToSync;
                    if(myList[0].isEmpty()){
                        //filtrar apenas os nomes dos ficheiros
                        filesToSync = new ArrayList<>();
                        for(String s : listaSplit){
                            String[] partida = s.split(",",3);
                            filesToSync.add(partida[0]);
                            //criar o log para cada ficheiro que vai ser pedido
                            logs.insertLog(ipSource,partida[0],Integer.parseInt(partida[2]));
                        }
                    }else{
                        //comparar as duas listas para saber quais os ficheiros a pedir
                        //esta lista tem (fileID,fileLength)
                        filesToSync = Utils.compareLists(listaSplit, myList);

                        List<String> listaSoID = new ArrayList<>();
                        for(String s : filesToSync){
                            String[] splitted = s.split(",",2);
                            //criar o log para cada ficheiro que vai ser pedido
                            logs.insertLog(ipSource,splitted[0],Integer.parseInt(splitted[1]));

                            listaSoID.add(splitted[0]);
                        }
                        filesToSync.clear();
                        filesToSync.addAll(listaSoID);
                    }

                    //prepara mensagem com a lista de ficheiros a pedir ao outro lado
                    msgEnviar = messageSendFilesRequest(filesToSync,-1);
                }
                break;
            case -1:
                //chegou um pedido inicial de um ficheiro
                //percorre o array de bytes à procura da posição do 1º \0
                for(int i=0 ; i< receivedMessage.length() ; i++){
                    if(receivedMessage.charAt(i) == '\0')
                        breakIndex[count++] = i;
                    if(count==1)
                        break;
                }

                this.szData1 = receivedMessage.substring(0,breakIndex[0]);

                //prepara mensagem com o bloco do ficheiro a enviar
                //tamanho do buffer será 512 menos 4 byte para NSeq, 1 byte para FlagUlt
                //e 1 byte por cada caracter do nome do ficheiro + 1 byte do \0 final
                int maxBuffer = 512 - 4 - 1 - this.szData1.length() - 1;
                byte[] bloco = Utils.readFileBlock(this.szData1,0,maxBuffer);
                if(bloco != null){
                    //se leu menos bytes que os possiveis, significa que é o ultimo bloco
                    int flagUlt = bloco.length < maxBuffer ? 1 : 0;
                    msgEnviar.add(messageSendFileBlock(
                            this.szData1, bloco, 0, flagUlt));
                }
                break;
            default:
                //chegou uma entrega de um bloco de um ficheiro ou o pedido de um novo bloco
                // pelo que o nSequencia tem de ser >= zero. Caso contrário é para descartar esta mensagem.
                if(this.iNumSequencia>=0){
                    if(this.iFlagUltimo != -1){
                        //chegou uma entrega de um bloco de um ficheiro pois a flagUltimo != -1.
                        //percorre o array de bytes à procura da posição do 1º \0
                        for(int i=0 ; i< receivedMessage.length() ; i++){
                            if(receivedMessage.charAt(i) == '\0')
                                breakIndex[count++] = i;
                            if(count==1)
                                break;
                        }

                        this.szData1 = receivedMessage.substring(0,breakIndex[0]);

                        //guarda o bloco recebido
                        // breakIndex[0] + 1 (posicao a seguir) + 4 (nSeq e flagUlt 0-index)
                        int startPosition = breakIndex[0]+6;
                        byte[] data = Arrays.copyOfRange(dados,
                                startPosition,dados.length-startPosition);

                        //prepara lista com nome do ficheiro para continuar a pedir
                        List<String> files = new ArrayList<>();
                        files.add(this.szData1);

                        try {
                            Utils.writeFileBlock(this.szData1, data,
                                    this.iNumSequencia, this.iFlagUltimo);

                            //conseguiu colocar o bloco e agora vai pedir o seguinte
                            //caso este não tenha sido já o ultimo
                            if(this.iFlagUltimo == 0)
                                msgEnviar = messageSendFilesRequest(files,
                                        this.iNumSequencia + data.length);

                            //atualiza os logs com o bloco recebido
                            this.logs.updateLog(ipSource,this.szData1,data.length,this.iFlagUltimo);
                        }catch (IOException e){
                            //não conseguiu colocar este bloco no sitio, por isso
                            //vai pedi-lo novamente
                            System.out.println("\nnão conseguiu guardar o ficheiro!\n");
                            msgEnviar = messageSendFilesRequest(files,
                                    this.iNumSequencia);
                        }
                    }else{
                        //chegou um pedido não inicial de um ficheiro pois flagUltimo = -1
                        //percorre o array de bytes à procura da posição do 1º \0
                        for(int i=0 ; i< receivedMessage.length() ; i++){
                            if(receivedMessage.charAt(i) == '\0')
                                breakIndex[count++] = i;
                            if(count==1)
                                break;
                        }

                        this.szData1 = receivedMessage.substring(0,breakIndex[0]);

                        //prepara mensagem com o bloco do ficheiro a enviar
                        //tamanho do buffer será 512 menos 4 byte para NSeq, 1 byte para FlagUlt
                        //e 1 byte por cada caracter do nome do ficheiro + 1 byte do \0 final
                        int maxBuffer2 = 512 - 4 - 1 - this.szData1.length() - 1;
                        //lidos, caso contrario o bloco2 vai vazio ser adicionado eternamente"
                        byte[] bloco2 = Utils.readFileBlock(
                                this.szData1,this.iNumSequencia,maxBuffer2);
                        if(bloco2 != null){
                            //se leu menos bytes que os possiveis, significa que é o ultimo bloco
                            int flagUlt = bloco2.length < maxBuffer2 ? 1 : 0;
                            msgEnviar.add(messageSendFileBlock(
                                    this.szData1, bloco2, this.iNumSequencia, flagUlt));
                        }
                    }
                }
                break;
        }

        return msgEnviar;
    }

    @Override
    public String toString() {
        if(szData2.isEmpty()){
            return "FTRapid{" + '\n' +
                    "NumSequencia = " + iNumSequencia + '\n' +
                    "FlagUltimo = " + iFlagUltimo + '\n' +
                    "Data1 = " + szData1 + '\n' +
                    "keys = " + keys + '\n' +
                    "keyHost = " + keyHost + '\n' +
                    '}';
        }else{
            return "FTRapid{" + '\n' +
                    "NumSequencia = " + iNumSequencia + '\n' +
                    "FlagUltimo = " + iFlagUltimo + '\n' +
                    "Data1 = " + szData1 + '\n' +
                    "Data2 = " + szData2 + '\n' +
                    "keys = " + keys + '\n' +
                    "keyHost = " + keyHost + '\n' +
                    '}';
        }
    }

    /**
     * Prepara uma mensagem para inicio de conexao
     * @return mensagem
     */
    public byte[] messageConectionStart(String pasta, String chaveDestino) {
        //atualiza as variaveis de instancia
        this.iNumSequencia = -3;
        this.iFlagUltimo = -1;
        this.szData1 = pasta;
        this.szData2 = chaveDestino;

        //cria cabeçalho da mensagem de inicio
        byte[] nSequencia = ByteBuffer.allocate(4).putInt(this.iNumSequencia).array();
        int flagUltimo = this.iFlagUltimo;
        byte[] msg = (pasta + '\0' + chaveDestino + '\0').getBytes(StandardCharsets.UTF_8);

        //junta todos num unico
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(nSequencia);
            outputStream.write(flagUltimo);
            outputStream.write(msg);
        }catch (IOException e){
            outputStream.reset();
        }

        this.mensagem = outputStream.toByteArray();

        try {
            outputStream.close();
        }catch (IOException ignore){}

        return this.mensagem;
    }

    /**
     * Prepara uma mensagem para inicio de conexao
     * @return mensagem
     */
    public byte[] messageSendFilesList(String filesList) {
        //atualiza as variaveis de instancia
        this.iNumSequencia = -2;
        this.iFlagUltimo = -1;
        this.szData1 = filesList;
        this.szData2 = "";

        //cria cabeçalho da mensagem de inicio
        byte[] nSequencia = ByteBuffer.allocate(4).putInt(this.iNumSequencia).array();
        int flagUltimo = this.iFlagUltimo;
        byte[] lista = (filesList+'\0').getBytes(StandardCharsets.UTF_8);

        //junta todos num unico
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(nSequencia);
            outputStream.write(flagUltimo);
            outputStream.write(lista);
        }catch (IOException e){
            outputStream.reset();
        }

        this.mensagem = outputStream.toByteArray();

        return this.mensagem;
    }

    public List<byte[]> messageSendFilesRequest(List<String> filesToSync, int numSeq){
        //atualiza as variaveis de instancia
        this.iNumSequencia = numSeq;
        this.iFlagUltimo = -1;
        this.szData1 = String.join("\n",filesToSync);
        this.szData2 = "";

        List<byte[]> msgEnviar = new ArrayList<>();
        for(String s : filesToSync){
            //cria cabeçalho da mensagem de pedido inicial de ficheiros
            byte[] nSequencia = ByteBuffer.allocate(4).putInt(this.iNumSequencia).array();
            int flagUltimo = this.iFlagUltimo;
            byte[] fileID = (s+'\0').getBytes(StandardCharsets.UTF_8);

            //junta todos num unico
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(nSequencia);
                outputStream.write(flagUltimo);
                outputStream.write(fileID);
            }catch (IOException e){
                outputStream.reset();
            }
            msgEnviar.add(outputStream.toByteArray());
        }

        return msgEnviar;
    }

    public byte[] messageSendFileBlock(String file, byte[] bloco,
                                                   int numSeq, int flagUlt){
        //atualiza as variaveis de instancia
        this.iNumSequencia = numSeq;
        this.iFlagUltimo = flagUlt;
        this.szData1 = file;
        this.szData2 = "";

        //cria cabeçalho da mensagem de pedido inicial de ficheiros
        byte[] nSequencia = ByteBuffer.allocate(4).putInt(this.iNumSequencia).array();
        int flagUltimo = this.iFlagUltimo;
        byte[] fileID = (file+'\0').getBytes(StandardCharsets.UTF_8);

        //junta todos num unico
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(nSequencia);
            outputStream.write(flagUltimo);
            outputStream.write(fileID);
            outputStream.write(bloco);
        }catch (IOException e){
            outputStream.reset();
        }

        this.mensagem = outputStream.toByteArray();

        return this.mensagem;
    }
}
