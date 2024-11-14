package com.github.lylanv.secdroid.utils;

import java.util.Objects;

public class TwoStringKey {
    private String part1, part2;

    public TwoStringKey(String part1, String part2) {
        this.part1 = part1;
        this.part2 = part2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TwoStringKey key = (TwoStringKey) o;
        return Objects.equals(part1, key.part1) && Objects.equals(part2, key.part2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(part1, part2);
    }

    public String getPart1() {
        return part1;
    }

    public String getPart2() {
        return part2;
    }
}
