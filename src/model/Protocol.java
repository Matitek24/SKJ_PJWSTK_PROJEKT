package model;

public enum Protocol {
    TCP,
    UDP;

    @Override
    public String toString() {
        return this.name();
    }
}