package org.stormpx.dl.kit;

import java.time.Instant;

public class ProgressBar {

    private int total;
    private int current=0;
    private int len;

    private boolean done;

    private String taskName;
    private String message;

    public ProgressBar(int total, int barLen, String taskName) {
        this.total = total;
        this.len = barLen;
        this.taskName =taskName;
    }

    public ProgressBar( int barLen, String taskName) {
        this.len = barLen;
        this.taskName =taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getMessage() {
        return message;
    }

    public int getTotal() {
        return total;
    }

    public int getCurrent() {
        return current;
    }


    public ProgressBar setTotal(int total) {
        this.total = total;
        return this;
    }

    public ProgressBar setMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isDone() {
        return done;
    }

    public ProgressBar stepTo(int latest){
        this.current=latest;
        if (this.current>=total){
            done=true;
        }
        return this;
    }
    public ProgressBar stepBy(int delta){
        return stepTo(this.current+delta);
    }

    public ProgressBar complete(){
        this.current=this.total;
        this.done=true;
        return this;
    }

    public ProgressBar failed(String message){
        this.done=true;
        this.message=message;
        return this;
    }

    public String getBarText(){
        var sb=new StringBuilder();
        if (taskName !=null)
            sb.append(taskName);
        if (this.total<=0){
            sb.append(String.format("%d/? ",this.current));
        }else{
            double percent= (double) current/total;
            int progress= (int) (len*percent);
            sb.append("[").append("#".repeat(progress));
            if (len>progress)
                sb.append(" ".repeat(len-progress));
            sb.append(String.format("] %d/%d (%.2f%%) ",this.current,this.total,percent*100));
        }
        if (this.message!=null){
            sb.append(this.message);
        }
        return sb.toString();
    }


    public void printf(){
        System.out.print("\r");
        System.out.print(getBarText());
    }


}
