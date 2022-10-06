package org.stormpx.dl.task;

import java.util.Objects;
import java.util.function.Consumer;

public class TaskUnit implements Runnable {

    private String id;
    private String name;
    private TaskManager taskManager;

    private boolean done;
    private Exception exception;
    private long total;
    private long current;
    private String message;
    private Consumer<TaskUnit> runner;

    public TaskUnit(String id, String name, Consumer<TaskUnit> runner,TaskManager taskManager) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(runner);
        Objects.requireNonNull(taskManager);
        this.id = id;
        this.name = name;
        this.runner = runner;
        this.taskManager=taskManager;
    }

    @Override
    public void run() {
        try {
            runner.accept(this);
        } catch (Exception e) {
            complete(e);
        }
    }

    public boolean isFailed(){
        return done&&exception!=null;
    }


    public TaskUnit setTotal(long total) {
        this.total = total;
        return this;
    }

    public void stepBy(int readBytes){
        this.current+=readBytes;
    }


    public void complete(Exception e){
        this.done=true;
        this.exception=e;
        if (e!=null) {
            this.message = e.getMessage();
        }
        taskManager.complete(this.id,e);
    }

    public TaskUnit setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public boolean isDone() {
        return done;
    }

    public Exception ex() {
        return exception;
    }

    public long getTotal() {
        return total;
    }

    public long getCurrent() {
        return current;
    }

    public String getMessage() {
        return message;
    }





}
