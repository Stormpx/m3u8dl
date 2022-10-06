package org.stormpx.dl.task;

import jdk.incubator.concurrent.StructuredTaskScope;
import org.stormpx.dl.kit.ProgressBar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class TaskManager {

    private StructuredTaskScope.ShutdownOnFailure taskScope;
//    private Executor executor;
    private Map<String, TaskUnit> tasks;
//    private Map<String,Exception> exceptions;

    private long completedTasks;

    private long timestamp;
    private long speed;
    private long finished;
    private Progress progress;

    public TaskManager() {
        this.taskScope=new StructuredTaskScope.ShutdownOnFailure();
        this.tasks=new LinkedHashMap<>();
//        this.exceptions=new ConcurrentHashMap<>();
    }

    public void execute(String name, Consumer<TaskUnit> runner){
        String id=UUID.randomUUID().toString();
        TaskUnit unit = new TaskUnit(id, name, runner,this);
        tasks.put(id,unit);
        taskScope.fork(()->{
            unit.run();
            return null;
        });
//        executor.execute(unit);

    }

    void complete(String id,Exception e){
        completedTasks +=1;
//        if (e!=null){
//            exceptions.put(id,e);
//        }
    }

    public void throwIfFailed() throws Exception {
//        Collection<Exception> exceptions = exceptions();
//        if (!exceptions.isEmpty())
//            throw exceptions.iterator().next();
        this.taskScope.throwIfFailed();
    }
    public int totalTask(){
        return tasks.size();
    }

    public boolean isAllDone(){
        return totalTask()==completedTasks;
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

        this.progress=new Progress(message,completedTasks,totalTask(),currentBytes,totalBytes,speed);

        return this.progress;
    }


    public Progress getProgress() {
        return progress;
    }
}
