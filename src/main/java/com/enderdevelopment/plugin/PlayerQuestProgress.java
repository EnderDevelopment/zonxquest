package com.enderdevelopment.plugin;

public final class PlayerQuestProgress {
    private int progress;
    private long periodStarted;
    private boolean paid;

    public PlayerQuestProgress(long periodStarted) { this.periodStarted = periodStarted; }
    public int getProgress() { return progress; }
    public long getPeriodStarted() { return periodStarted; }
    public boolean isPaid() { return paid; }
    public void setProgress(int value) { progress = value; }
    public void setPaid(boolean value) { paid = value; }
    public void reset(long started) { progress = 0; periodStarted = started; paid = false; }
}
