package org.eso.ias.types;

/**
 * The priorities of the alarms
 * 
 * The priorities are defined from the lowest to the highest
 * so that the highest priority has also the greater ordinal number
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** 
     * @return the lowest priority
     */
    public static Priority getMinPriority() {
        return LOW;
    }

    /** 
     * @return the highest priority
     */
    public static Priority getMaxPriority() {
        return CRITICAL;
    }

    /** 
     * @return the default priority
     */
    public static Priority getDefaultPriority() {
        return MEDIUM;
    }

    /**
     * The priority higher than this priority
     * 
     * @return the priority higher than this one or
     *           this if it is the highest priority
     */
    public Priority getHigherPrio() {
        if (this==CRITICAL) {
            return this;
        } else {
            return Priority.values()[this.ordinal()+1];
        }
    }

    /**
     * The priority lower than this priority
     * 
     * @return the priority lower than this one or
     *           this if it is the lowest priority
     */
    public Priority getLowerPrio() {
        if (this==LOW) {
            return this;
        } else {
            return Priority.values()[this.ordinal()-1];
        }
    }
}
