package com.shyeuar.baity.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class TickSchedulerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/TickScheduler");

    private static TickSchedulerUtils instance;

    private int currentTick = 0;
    private final AbstractInt2ObjectMap<List<Runnable>> scheduledTasks = new Int2ObjectOpenHashMap<>();
    private final AtomicInteger nextTaskId = new AtomicInteger(0);

    private TickSchedulerUtils() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public static TickSchedulerUtils getInstance() {
        return instance == null ? instance = new TickSchedulerUtils() : instance;
    }

    public boolean cancelTask(int taskId) {
        boolean found = false;
        for (List<Runnable> taskList : scheduledTasks.values()) {
            Iterator<Runnable> iterator = taskList.iterator();
            while (iterator.hasNext()) {
                Runnable task = iterator.next();
                if (task instanceof ScheduledTask scheduled && scheduled.taskId == taskId) {
                    scheduled.cancelled = true;
                    iterator.remove();
                    found = true;
                }
            }
        }
        return found;
    }

    public void runLater(Runnable task, int delay, TimeUnit unit) {
        runLater(task, TickUtils.fromTime(Math.max(0, delay), unit));
    }

    public void runLater(Runnable task, int delayTicks) {
        ScheduledTask scheduled = new ScheduledTask(task);
        scheduled.taskId = nextTaskId.incrementAndGet();
        addTask(scheduled, currentTick + Math.max(0, delayTicks));
    }

    public int runRepeating(Runnable task, int interval, TimeUnit unit) {
        return runRepeating(task, TickUtils.fromTime(Math.max(1, interval), unit));
    }

    public int runRepeating(Runnable task, int intervalTicks) {
        ScheduledTask scheduled = new ScheduledTask(task, Math.max(1, intervalTicks), true);
        scheduled.taskId = nextTaskId.incrementAndGet();
        return addTask(scheduled, currentTick);
    }

    private int addTask(ScheduledTask task, int targetTick) {
        if (scheduledTasks.containsKey(targetTick)) {
            scheduledTasks.get(targetTick).add(task);
        } else {
            List<Runnable> list = new ArrayList<>();
            list.add(task);
            scheduledTasks.put(targetTick, list);
        }
        return task.taskId;
    }

    void onTick(Minecraft client) {
        if (scheduledTasks.containsKey(currentTick)) {
            List<Runnable> tasks = scheduledTasks.get(currentTick);
            for (int i = 0; i < tasks.size(); i++) {
                Runnable task = tasks.get(i);
                if (!executeTask(task)) {
                    scheduledTasks.computeIfAbsent(currentTick + 1, key -> new ArrayList<>()).add(task);
                }
            }
            scheduledTasks.remove(currentTick);
        }
        currentTick += 1;
    }

    private boolean executeTask(Runnable task) {
        try {
            task.run();
        } catch (Throwable error) {
            if (task instanceof ScheduledTask scheduled) {
                LOGGER.warn("Task #{} failed: {}", scheduled.taskId, error.toString());
            }
            return false;
        }
        return true;
    }

    private static class ScheduledTask implements Runnable {

        private final Runnable action;
        private final int interval;
        private final boolean repeating;
        private int taskId = -1;
        private boolean cancelled = false;

        ScheduledTask(Runnable action) {
            this(action, 0, false);
        }

        ScheduledTask(Runnable action, int interval, boolean repeating) {
            this.action = action;
            this.interval = interval;
            this.repeating = repeating;
        }

        @Override
        public void run() {
            if (cancelled) return;
            
            action.run();

            if (repeating) {
                if (Minecraft.getInstance() != null && !RenderSystem.isOnRenderThread()) {
                    Minecraft.getInstance().schedule(() -> instance.addTask(this, instance.currentTick + interval));
                } else {
                    instance.addTask(this, instance.currentTick + interval);
                }
            }
        }
    }
}
