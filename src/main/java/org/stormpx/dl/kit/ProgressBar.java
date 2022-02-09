package org.stormpx.dl.kit;

public class ProgressBar {

    private int total;
    private int current=0;
    private int len;

    private boolean done;

    private String taskName;



    public ProgressBar(int total, int barLen, String taskName) {
        this.total = total;
        this.len = barLen;
        this.taskName =taskName;
    }

    public int getTotal() {
        return total;
    }

    public int getCurrent() {
        return current;
    }

    public int getLen() {
        return len;
    }

    public boolean isDone() {
        return done;
    }

    public ProgressBar stepTo(int current){
        this.current=current;
        if (done)
            return this;
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
    public void printf(){
        System.out.print("\r");
        if (taskName !=null)
            System.out.print(taskName);
        if (this.total<=0){
            System.out.printf("(%d)",this.current);
        }else{
            double percent= (double) current/total;
            int progress= (int) (len*percent);

            System.out.print("["+"#".repeat(progress));
            if (len>progress)
                System.out.print(" ".repeat(len-progress));
            System.out.printf("] %d/%d (%.2f%%)",this.current,this.total,percent*100);
//            System.out.flush();
        }

    }


}
