package com.github.lylanv.secdroid.events;

public class ApplicationStoppedEvent {
    private boolean applicationStopped;

    public ApplicationStoppedEvent(final boolean applicationStopped) {
        this.applicationStopped = applicationStopped;
    }

    public boolean getApplicationStopped() {
        return applicationStopped;
    }
}
