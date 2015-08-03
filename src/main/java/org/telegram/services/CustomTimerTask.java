package org.telegram.services;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * @brief Task to be execute periodically
 * @date 28/01/15
 */
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

    /**
     * Get name
     *
     * @return name
     */
    public String getTaskName() {
        return this.taskName;
    }

    /**
     * Set name
     *
     * @param taskName new name
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * Getter for the times
     *
     * @return Remainint times the task must be executed
     */
    public int getTimes() {
        return this.times;
    }

    /**
     * Setter for the times
     *
     * @param times Number of times the task must be executed
     */
    public void setTimes(int times) {
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
