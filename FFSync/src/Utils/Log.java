package Utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*A classe Log contem apenas a estrutura de dados do log tipo. Este contem 4 parametros 
que sao armazenados/atualizados a cada novo pacote recebido. */

public class Log {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int sizeTotal;
    private int sizeTransferred;

    public Log(){
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.sizeTotal = 0;
        this.sizeTransferred = 0;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getSizeTotal() {
        return sizeTotal;
    }

    public void setSizeTotal(int sizeTotal) {
        this.sizeTotal = sizeTotal;
    }

    public int getSizeTransferred() {
        return sizeTransferred;
    }

    public void setSizeTransferred(int sizeTransferred) {
        this.sizeTransferred = sizeTransferred;
    }

    @Override
    public String toString(){
        //TimeStart, TimeEnd, TimeTotal, SizeTotal, SizeTransferred, TransferRate
        StringBuilder sb = new StringBuilder();

        sb.append(this.startTime).append(", ");

        if(this.endTime!=null){
            sb.append(this.endTime).append(", ");
            int timeTotalSeconds = (int) ChronoUnit.SECONDS.between(this.startTime, this.endTime);
            sb.append(timeTotalSeconds).append(", ");

            sb.append(this.sizeTotal).append(", ");
            sb.append(this.sizeTransferred).append(", ");

            int transferRateBitPerSecond = this.sizeTotal * 8 / timeTotalSeconds;
            sb.append(transferRateBitPerSecond);
        }else{
            sb.append("ongoing").append(", ");
            sb.append("ongoing").append(", ");

            sb.append(this.sizeTotal).append(", ");
            sb.append(this.sizeTransferred).append(", ");

            sb.append("ongoing");
        }

        return sb.toString();
    }

    public void updateLog(int sizeNewBlock, int flagLastBlock){
        this.sizeTransferred += sizeNewBlock;
        if(flagLastBlock==1){
            this.endTime = LocalDateTime.now();
        }
    }
}
