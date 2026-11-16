package com.stride.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chart geometry worked out on the server, so the page draws plain SVG with no script and no
 * inline styles (the Content-Security-Policy stays strict). Every chart is also shown as a table.
 */
public final class Chart {

    public enum Type { BAR, LINE }

    public static final int WIDTH = 720;
    public static final int HEIGHT = 300;
    public static final int LEFT = 56;
    public static final int RIGHT = 16;
    public static final int TOP = 16;
    public static final int BOTTOM = 64;

    public static final class Bar {
        private final String label;
        private final String valueText;
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final boolean alert;

        Bar(String label, String valueText, int x, int y, int w, int h, boolean alert) {
            this.label = label;
            this.valueText = valueText;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.alert = alert;
        }

        public String getLabel() { return label; }
        public String getValueText() { return valueText; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getW() { return w; }
        public int getH() { return h; }
        public int getCx() { return x + w / 2; }
        public int getLabelY() { return HEIGHT - BOTTOM + 20; }
        public int getValueY() { return Math.max(TOP + 12, y - 6); }
        public boolean isAlert() { return alert; }
    }

    public static final class Tick {
        private final int y;
        private final String label;

        Tick(int y, String label) {
            this.y = y;
            this.label = label;
        }

        public int getY() { return y; }
        public String getLabel() { return label; }
        public int getTextY() { return y + 5; }
    }

    private final Type type;
    private final String title;
    private final String unit;
    private final List<Bar> bars = new ArrayList<>();
    private final List<Tick> ticks = new ArrayList<>();
    private String points = "";
    private Integer lineY;
    private String lineLabel;

    private Chart(Type type, String title, String unit) {
        this.type = type;
        this.title = title;
        this.unit = unit;
    }

    /** Bars for the given values. Bars at or below alertBelow (if not null) are hatched as a warning. */
    public static Chart bars(String title, String unit, List<String> labels, List<Double> values, double max,
                             Double alertBelow) {
        Chart c = new Chart(Type.BAR, title, unit);
        double top = max > 0 ? max : 1;
        c.axis(top);
        int n = Math.max(1, labels.size());
        int plotW = WIDTH - LEFT - RIGHT;
        int slot = plotW / n;
        int w = Math.max(8, Math.min(96, (int) (slot * 0.62)));
        for (int i = 0; i < labels.size(); i++) {
            double v = values.get(i);
            int h = (int) Math.round((v / top) * plotH());
            int x = LEFT + i * slot + (slot - w) / 2;
            boolean alert = alertBelow != null && v < alertBelow;
            c.bars.add(new Bar(labels.get(i), format(v) + unit, x, HEIGHT - BOTTOM - h, w, h, alert));
        }
        if (alertBelow != null) {
            c.lineY = HEIGHT - BOTTOM - (int) Math.round((alertBelow / top) * plotH());
            c.lineLabel = format(alertBelow) + unit + " line";
        }
        return c;
    }

    /** A line through the values (e.g. weekly attendance), with a dot and label at each point. */
    public static Chart line(String title, String unit, List<String> labels, List<Double> values, double max,
                             Double guide) {
        Chart c = bars(title, unit, labels, values, max, guide);
        Chart line = new Chart(Type.LINE, title, unit);
        line.ticks.addAll(c.ticks);
        line.lineY = c.lineY;
        line.lineLabel = c.lineLabel;
        StringBuilder pts = new StringBuilder();
        for (Bar b : c.bars) {
            line.bars.add(new Bar(b.label, b.valueText, b.getCx() - 5, b.y - 5, 10, 10, b.alert));
            pts.append(b.getCx()).append(',').append(b.y).append(' ');
        }
        line.points = pts.toString().trim();
        return line;
    }

    private void axis(double top) {
        for (int i = 0; i <= 4; i++) {
            double v = top * i / 4;
            int y = HEIGHT - BOTTOM - (int) Math.round(plotH() * i / 4.0);
            ticks.add(new Tick(y, format(v) + unit));
        }
    }

    private static int plotH() {
        return HEIGHT - TOP - BOTTOM;
    }

    static String format(double v) {
        return Math.abs(v - Math.rint(v)) < 0.05 ? String.valueOf((long) Math.rint(v))
                : String.format(Locale.ROOT, "%.1f", v);
    }

    public boolean isLine() { return type == Type.LINE; }
    public String getTitle() { return title; }
    public List<Bar> getBars() { return bars; }
    public List<Tick> getTicks() { return ticks; }
    public String getPoints() { return points; }
    public Integer getLineY() { return lineY; }
    public String getLineLabel() { return lineLabel; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
    public int getLeft() { return LEFT; }
    public int getRight() { return WIDTH - RIGHT; }
    public int getBaseY() { return HEIGHT - BOTTOM; }
    public String getViewBox() { return "0 0 " + WIDTH + " " + HEIGHT; }
}
