package com.enderdevelopment.plugin;

import java.util.Locale;

public final class Quest {
    public enum Period { HOURLY, DAILY, WEEKLY }

    private final String id;
    private String name;
    private String description;
    private String category;
    private Period period;
    private int target;
    private double reward;

    public Quest(String id, String name, String description, String category, Period period, int target, double reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.period = period;
        this.target = target;
        this.reward = reward;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Period getPeriod() { return period; }
    public int getTarget() { return target; }
    public double getReward() { return reward; }
    public void setName(String value) { name = value; }
    public void setDescription(String value) { description = value; }
    public void setCategory(String value) { category = value.toLowerCase(Locale.ROOT); }
    public void setPeriod(Period value) { period = value; }
    public void setTarget(int value) { target = value; }
    public void setReward(double value) { reward = value; }
}
