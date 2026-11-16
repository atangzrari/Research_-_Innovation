package com.stride.model;

/** Classes attended out of classes held for one module. */
public class AttendanceRecord {
    private final String moduleCode;
    private final String moduleName;
    private final int attended;
    private final int total;

    public AttendanceRecord(String moduleCode, String moduleName, int attended, int total) {
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.attended = attended;
        this.total = total;
    }

    public String getModuleCode() { return moduleCode; }
    public String getModuleName() { return moduleName; }
    public int getAttended() { return attended; }
    public int getTotal() { return total; }
    public int getPercent() { return total == 0 ? 0 : Math.round(100f * attended / total); }
    public int getMissed() { return total - attended; }
    /** Below the at-risk line? The threshold comes from configuration. */
    public boolean isBelow(int percent) { return total > 0 && getPercent() < percent; }
}
