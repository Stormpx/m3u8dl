package org.stormpx.dl.task;

import org.stormpx.dl.kit.Strs;

public class Progress {
    private String message;
    private long completedTasks;
    private long totalTask;
    private long currentBytes;
    private long totalBytes;
    private long speed;


    private String pretty;

    public Progress(String message, long completedTasks, long totalTask, long currentBytes, long totalBytes, long speed) {
        this.message = message;
        this.completedTasks = completedTasks;
        this.totalTask = totalTask;
        this.currentBytes = currentBytes;
        this.totalBytes = totalBytes;
        this.speed = speed;
    }



    public String getMessage() {
        return message;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public String placeHolder(){
        if (this.pretty ==null)
            return "";
        return " ".repeat(pretty.length());
    }

    public long getTotalTask() {
        return totalTask;
    }

    public long getCurrentBytes() {
        return currentBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getSpeed() {
        return speed;
    }


    public String getPretty(){
        this.pretty ="%s.. %s (%d/%d) %s/%s %s/s".formatted(
                "downloading",this.message !=null?this.message :"",
                this.completedTasks, this.totalTask,
                Strs.formatByteSize(this.currentBytes),Strs.formatByteSize(this.totalBytes),
                Strs.formatByteSize(this.speed)
        );
        return this.pretty;
    }

}
