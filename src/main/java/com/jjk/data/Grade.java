package com.jjk.data;

public enum Grade {
    GRADE_4("grade_4",      "4급",  1.00f),
    GRADE_3("grade_3",      "3급",  1.15f),
    GRADE_2("grade_2",      "2급",  1.30f),
    GRADE_1("grade_1",      "1급",  1.50f),
    SEMI_SPECIAL("semi_grade_1",  "준특급", 1.65f),
    SPECIAL("special_grade",      "특급",  1.80f);

    public final String key;
    public final String display;
    public final float multiplier;

    Grade(String key, String display, float multiplier) {
        this.key = key;
        this.display = display;
        this.multiplier = multiplier;
    }

    /** Matches both English key ("grade_4") and Korean display ("4급"). Returns GRADE_4 on unknown input. */
    public static Grade fromKey(String key) {
        if (key == null) return GRADE_4;
        for (Grade g : values()) {
            if (g.key.equals(key) || g.display.equals(key)) return g;
        }
        return GRADE_4;
    }
}
