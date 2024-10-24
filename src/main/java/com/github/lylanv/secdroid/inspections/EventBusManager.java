package com.github.lylanv.secdroid.inspections;

import com.google.common.eventbus.EventBus;

public class EventBusManager {
    private static final EventBus eventBus = new EventBus();

    public static void post(Object event) {
        eventBus.post(event);
    }

    public static void register(Object subscriber) {
        eventBus.register(subscriber);
    }

    public static void unregister(Object subscriber) {
        eventBus.unregister(subscriber);
    }
}
