package org.eso.ias.monitor;

import org.eso.ias.types.Alarm;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An enumerated with all the alarms generated by the Monitor tool.
 *
 * The monitor tool periodically sends these alarms.
 */
public enum MonitorAlarm {

    PLUGIN_DEAD,
    CONVERTER_DEAD,
    SUPERVISOR_DEAD,
    SINK_DEAD,
    CLIENT_DEAD;

    /**
     * The ID of each alarm
     */
    public final String id;

    /**
     * The last alarm sent for this monitorAlarm
     */
    private AtomicReference<Alarm> alarm = new AtomicReference<>(Alarm.CLEARED);

    /**
     * The IDs of the faulty monitored tools (plugins, converters...)
     * to be set as property of the IASValue
     */
    private AtomicReference<String> faultyIds = new AtomicReference<>("");

    /**
     * Constructor
     */
    private MonitorAlarm() {
        this.id = "IASMON-"+this.name();
    }

    /**
     * Clear the alarm
     */
    public void clear() {
        alarm.set(Alarm.cleared());
        faultyIds.set("");
    }

    /**
     * Set the alarm
     *
     * @param alarm the not null alarm to set
     * @param faultyIds the comma separated IDs of tools that did not sent the HB
     */
    public void set(Alarm alarm, String faultyIds) {
        Objects.requireNonNull(alarm);
        Objects.requireNonNull(faultyIds);
        if (alarm==Alarm.CLEARED) {
            clear();
        } else {
            this.alarm.set(alarm);
            this.faultyIds.set(faultyIds);
        }
    }

    /**
     * Set an alarm with the default priority
     *
     * @param faultyIds the comma separated IDs of tools that did not sent the HB
     */
    public void set(String faultyIds) {
        Objects.requireNonNull(faultyIds);
        alarm.set(Alarm.getSetDefault());
        this.faultyIds.set(faultyIds);
    }
}
