package org.telegram.services;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Ruben Bermudez
 * @version 2.0
 *  Task to be executed periodically
 */
@Setter
@Getter
public abstract class CustomTimerTask {
    private String taskName = ""; ///< Task name
    private int times = 1;

    /**
     * Constructor
     *
     * @param taskName Name of the task
     */
    public CustomTimerTask(String taskName, int times) {
        this.taskName = taskName;
        this.times = times;
    }

    public void reduceTimes() {
        if (this.times > 0) {
            this.times -= 1;
        }
    }

    /**
     * @abstract Should contain the functionality of the task
     */
    public abstract void execute();
}
