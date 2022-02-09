package org.stormpx.dl.kit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProgressGroup {

    private List<ProgressBar> progressBars;

    public ProgressGroup() {
        this.progressBars=new CopyOnWriteArrayList<>();
    }

    public void addBar(ProgressBar bar){
        this.progressBars.add(bar);
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
        int size = progressBars.size();
        System.out.printf("\u001b[%dA", size);

        for (int i = 0; i < size; i++) {
            System.out.print("\u001b[0K\n");

        }
        System.out.printf("\u001b[%dA", size);
    }

    public void await() throws InterruptedException {
        System.out.println();
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

}
