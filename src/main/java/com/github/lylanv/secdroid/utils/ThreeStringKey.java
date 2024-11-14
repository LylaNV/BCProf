package com.github.lylanv.secdroid.utils;

import java.util.Objects;

public class ThreeStringKey {
    private String part1, part2, part3;

    public ThreeStringKey(String part1, String part2, String part3) {
        this.part1 = part1;
        this.part2 = part2;
        this.part3 = part3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreeStringKey key = (ThreeStringKey) o;
        return Objects.equals(part1, key.part1) && Objects.equals(part2, key.part2) && Objects.equals(part3, key.part3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(part1, part2, part3);
    }

    public String getPart1() {
        return part1;
    }

    public String getPart2() {
        return part2;
    }

    public String getPart3() {
        return part3;
    }

}
