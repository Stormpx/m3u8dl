package org.stormpx.dl.task;

import org.stormpx.dl.kit.ProgressBar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class TaskManager {

    private Executor executor;
    private Map<String, TaskUnit> tasks;
    private Map<String,Exception> exceptions;

    private long totalTask;
    private long completedTasks;

    private long timestamp;
    private long speed;
    private long finished;
    private Progress progress;

    public TaskManager(Executor executor) {
        this.executor = executor;
        this.tasks=new LinkedHashMap<>();
        this.exceptions=new ConcurrentHashMap<>();
    }

    public void execute(String name, Consumer<TaskUnit> runner){
        String id=UUID.randomUUID().toString();
        TaskUnit unit = new TaskUnit(id, name, runner,this);
        tasks.put(id,unit);
        totalTask+=1;
        executor.execute(unit);

    }

    void complete(String id,Exception e){
        completedTasks +=1;
        if (e!=null){
            exceptions.put(id,e);
        }
    }

    public Collection<Exception> exceptions(){
        if (this.exceptions.isEmpty()){
            return List.of();
        }
        return exceptions.values();
    }


    public boolean isAllDone(){
        return totalTask==completedTasks;
    }

    private long calculateSpeed(long finished){
        long now = System.currentTimeMillis();

        if ((now-timestamp)/1000>=1){
            this.timestamp=now;
            this.speed=finished-this.finished;
            this.finished=finished;
        }

        return this.speed;
    }

    public Progress generateProgress(){

        String message="";
        long currentBytes=0;
        long totalBytes=0;
        for (TaskUnit unit : tasks.values()) {
            if (!unit.isDone()){
                message="%s: %s".formatted(unit.getName(),unit.getMessage()==null?"":unit.getMessage());
            }
            currentBytes+=unit.getCurrent();
            totalBytes+=unit.getTotal();
        }
        long speed=calculateSpeed(currentBytes);

        this.progress=new Progress(message,completedTasks,totalTask,currentBytes,totalBytes,speed);

        return this.progress;
    }


    public Progress getProgress() {
        return progress;
    }
}
