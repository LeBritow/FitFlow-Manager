package com.mycompany.academia.core.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    public static class Event {
        public final String component;
        public final String action;
        public final String detail;
        public final long timestamp;

        public Event(String component, String action, String detail) {
            this.component = component;
            this.action = action;
            this.detail = detail;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public interface Listener {
        void onEvent(Event e);
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void emit(String component, String action, String detail) {
        Event e = new Event(component, action, detail);
        for (Listener l : listeners) {
            try { l.onEvent(e); } catch (Exception ignored) {}
        }
    }

    public static void subscribe(Listener listener) {
        listeners.add(listener);
    }

    public static void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }
}
