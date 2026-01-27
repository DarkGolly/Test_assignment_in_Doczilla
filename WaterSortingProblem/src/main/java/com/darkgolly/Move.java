package com.darkgolly;

public record Move(int from, int to) {
    @Override
    public String toString() {
        return String.format("(" + from + ", " + to + ")");
    }
}