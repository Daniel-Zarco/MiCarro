package com.micarro.backend.dto;

public class SyncResult {

    private final int received;
    private final int created;
    private final int updated;
    private final int unchanged;
    private final int deactivated;
    private final int errors;

    public SyncResult(
            int received,
            int created,
            int updated,
            int unchanged,
            int deactivated,
            int errors) {

        this.received = received;
        this.created = created;
        this.updated = updated;
        this.unchanged = unchanged;
        this.deactivated = deactivated;
        this.errors = errors;
    }

    public int getReceived() {
        return received;
    }

    public int getCreated() {
        return created;
    }

    public int getUpdated() {
        return updated;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public int getDeactivated() {
        return deactivated;
    }

    public int getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "SyncResult{"
                + "received=" + received
                + ", created=" + created
                + ", updated=" + updated
                + ", unchanged=" + unchanged
                + ", deactivated=" + deactivated
                + ", errors=" + errors
                + '}';
    }
}
