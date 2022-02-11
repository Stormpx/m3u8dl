package org.stormpx.dl.kit;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ProgressGroup {

    private List<ProgressBar> progressBars;

    private Progress latest;

    private String residue="";

    private long timestamp;

    public ProgressGroup() {
        this.progressBars=new CopyOnWriteArrayList<>();
    }

    public void addBar(ProgressBar bar){
        this.progressBars.add(bar);
    }

    public int size(){
        return progressBars.size();
    }

    public void show(boolean moveUp){
        int size = progressBars.size();
        if (moveUp)
            System.out.printf("\u001b[%dA", size);
        for (int i = 0; i < size; i++) {
            progressBars.get(i).printf();
            System.out.println();
        }
    }

    public void clear(){
        if (progressBars.isEmpty())
            return;
        int size = progressBars.size();
        System.out.printf("\u001b[%dA", size);

        for (int i = 0; i < size; i++) {
            System.out.print("\u001b[0K\n");

        }
        System.out.printf("\u001b[%dA", size);
    }

    public void await() throws InterruptedException {
        boolean done=false;
        while (!done){
            int size = progressBars.size();
            done=true;
            for (int i = 0; i < size; i++) {
                var progressBar = progressBars.get(i);
                done=done&&progressBar.isDone();
                progressBar.printf();
                System.out.println();
            }
            Thread.sleep(100);
            if (!done) System.out.printf("\u001b[%dA", size);
        }

    }

    public void report(){
        if (this.timestamp==0){
            this.timestamp=System.currentTimeMillis();
        }
        Progress progress = new Progress(progressBars);
        System.out.printf("\r%s"," ".repeat(residue.length()));
        this.residue=String.format("\rdownloading.. %s (%d/%d) %s/%s %s",
                progress.progressingMessage!=null?progress.getProgressingMessage():"",progress.getDoneCount(),progressBars.size(),progress.getCurrent(), progress.getTotal(),progress.getBytePerSecond());
        System.out.print(residue);
    }

    public void reportAwait() throws InterruptedException {
        boolean done=false;
        while (!done){
            report();
            int size = progressBars.size();
            done=true;
            for (int i = 0; i < size; i++) {
                var progressBar = progressBars.get(i);
                done=done&&progressBar.isDone();
            }
            Thread.sleep(100);
        }

    }

    private class Progress{
        private String progressingMessage;
        private int doneCount;
        private long current;
        private long total;

        public Progress(List<ProgressBar> progressBars) {
            for (ProgressBar progressBar : progressBars) {
                if (this.progressingMessage ==null&&!progressBar.isDone())
                    this.progressingMessage =progressBar.getTaskName();
                if (progressBar.isDone()){
                    doneCount++;
                }
                current+=progressBar.getCurrent();
                total+=progressBar.getTotal();
            }
        }

        public String getProgressingMessage() {
            return progressingMessage;
        }

        public int getDoneCount() {
            return doneCount;
        }

        public String getBytePerSecond() {
            long v=(System.currentTimeMillis()-timestamp)/1000;
            v=v==0?1:v;
            return Strs.formatByteSize(this.current/v)+"/s";
        }

        public String getCurrent() {
            return Strs.formatByteSize(current);
        }

        public String getTotal() {
            return Strs.formatByteSize(total);
        }
    }



}
