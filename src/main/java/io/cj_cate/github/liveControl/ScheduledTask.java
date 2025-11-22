package io.cj_cate.github.liveControl;

class ScheduledTask {
    long scheduledTime;
    Runnable function;

    ScheduledTask(long scheduledTime, Runnable function) {
        this.scheduledTime = scheduledTime;
        this.function = function;
    }
}
