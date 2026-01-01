package dev.xinxin.event;

import dev.xinxin.event.api.events.Event;
import dev.xinxin.event.api.events.EventStoppable;
import dev.xinxin.event.api.events.callables.EventCancellable;
import dev.xinxin.event.api.types.Priority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {
    private static final Logger logger = LogManager.getLogger("EventManager");

    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<>();

    private EventManager() {}

    public static void register(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventTarget.class)) continue;

            Class<?>[] params = method.getParameterTypes();
            int index = -1;
            for (int i = 0; i < params.length; i++) {
                if (Event.class.isAssignableFrom(params[i])) {
                    index = i;
                    break;
                }
            }
            if (index == -1) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) params[index];

            MethodData data = new MethodData(object, method, method.getAnnotation(EventTarget.class).value(), index);
            method.setAccessible(true);

            REGISTRY_MAP.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(data);
            sortListValue(eventType);
        }
    }

    public static void unregister(Object object) {
        for (List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.getSource().equals(object));
        }
        cleanMap(true);
    }

    public static Event call(Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());
        if (dataList == null) return event;

        try {
            if (event instanceof EventStoppable) {
                EventStoppable stoppable = (EventStoppable) event;
                for (MethodData data : dataList) {
                    if (!invoke(data, event)) continue;
                    if (stoppable.isStopped()) break;
                }
            } else if (event instanceof EventCancellable) {
                EventCancellable cancellable = (EventCancellable) event;
                for (MethodData data : dataList) {
                    if (!invoke(data, event)) continue;
                    if (cancellable.isCancelled()) break;
                }
            } else {
                for (MethodData data : dataList) {
                    invoke(data, event);
                }
            }
        } catch (Throwable t) {
            logger.error("Error while dispatching event: " + event.getClass().getSimpleName(), t);
        }

        return event;
    }

    private static boolean invoke(MethodData data, Event argument) {
        try {
            Method method = data.getTarget();
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                args[i] = (i == data.getEventParamIndex()) ? argument : getDefaultValue(method.getParameterTypes()[i]);
            }
            method.invoke(data.getSource(), args);
            return true;
        } catch (Throwable e) {
            logger.warn("Failed to invoke event method: " + data.getTarget(), e);
            return false;
        }
    }

    private static void sortListValue(Class<? extends Event> eventClass) {
        List<MethodData> list = REGISTRY_MAP.get(eventClass);
        if (list == null) return;

        List<MethodData> sorted = new ArrayList<>();
        for (byte priority : Priority.VALUE_ARRAY) {
            for (MethodData data : list) {
                if (data.getPriority() == priority) {
                    sorted.add(data);
                }
            }
        }

        REGISTRY_MAP.put(eventClass, new CopyOnWriteArrayList<>(sorted));
    }

    private static void cleanMap(boolean onlyEmpty) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> it = REGISTRY_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Class<? extends Event>, List<MethodData>> entry = it.next();
            if (!onlyEmpty || entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private static Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    private static final class MethodData {
        private final Object source;
        private final Method target;
        private final byte priority;
        private final int eventParamIndex;

        public MethodData(Object source, Method target, byte priority, int index) {
            this.source = source;
            this.target = target;
            this.priority = priority;
            this.eventParamIndex = index;
        }

        public Object getSource() {
            return source;
        }

        public Method getTarget() {
            return target;
        }

        public byte getPriority() {
            return priority;
        }

        public int getEventParamIndex() {
            return eventParamIndex;
        }
    }
}
