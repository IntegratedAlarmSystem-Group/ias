package org.eso.ias.types;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The operational mode of a monitor point value
 * 
 * @author acaproni
 */
public enum OperationalMode {
    /** Shutting down */
    CLOSING,

    /** Only partially operational */
    DEGRADED,

    /** Initialization on going */
    INITIALIZATION,

    /** Maintenance */
    MAINTENANCE,

    /** Fully operational */
    OPERATIONAL,

    /** Shut down */
    SHUTTEDDOWN,

    /** Starting up */
    STARTUP,

    /** Unknown state */
    UNKNOWN,

    /** Device malfunctioning */
    MALFUNCTIONING;

    /**
     * Get the OperationalMode from the modes if they agree or
     * returns the passed fallbackMode.
     *
     * This method is an helper to provide the mode of the output depending of the many
     * inputs of the TF.
     * If there is only one input or the modes of the inputs are all the same
     * than that mode is returned.
     * Otherwise the default is returned.
     *
     * @param modesOfInputs the modes of the inputs of the TF
     * @param fallbackMode the mode to return if the modes of the inputs disagree
     * @return the OperationalMode from the modes if they agree;
     * returns the passed fallbackMode otherwise
     *
     * @see <a href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/119">Issue #119</a>
     */
    public static OperationalMode getModeFromInputs(Iterable<OperationalMode> modesOfInputs, OperationalMode fallbackMode) {
        Objects.requireNonNull(fallbackMode, "Invalid null fallbackMode");
        Objects.requireNonNull(modesOfInputs, "The modes of the inputs can't be null");

        Set<OperationalMode> modes = new HashSet<>();

        modesOfInputs.forEach(mode ->
            modes.add(mode));
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("The modes of the inputs can't be empty");
        }
        if (modes.size() == 1) {
            return modes.iterator().next();
        } else {
            return fallbackMode;
        }
    }

    /**
     * Get a OperationalMode from the modes if they agree or UNKNOWN
     *
     * This method is an helper to provide the mode of the output depending of the many
     * inputs of the TF.
     * If there is only one input or the modes of the inputs are all the same
     * than that mode is returned.
     * Otherwise the default is returned.
     *
     * @param modesOfInputs the modes of the inputs of the TF
     * @return the OperationalMode from the modes if they agree; otherwise UNKNOWN
     *
     * @see <a href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/119">Issue #119</a>
     */
    public static OperationalMode getModeFromInputs(Iterable<OperationalMode> modesOfInputs) {
        return OperationalMode.getModeFromInputs(modesOfInputs, UNKNOWN);
    }
}
