package com.udacity.security.data;

import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public final class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners;
    private boolean isCatDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        statusListeners = new HashSet<>();
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {

        switch (armingStatus) {
            case DISARMED -> this.setAlarmStatus(AlarmStatus.NO_ALARM);
            case ARMED_HOME -> {
                if (this.isCatDetected) {
                    this.setAlarmStatus(AlarmStatus.ALARM);
                } else {
                    this.setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
            default -> {

                // Prevents the systems to hit a ConcurrentModificationException when two sensors are modified on the same thread.
                List<Sensor> sensors = new CopyOnWriteArrayList<>(this.getSensors());
                Iterator<Sensor> iterator = sensors.iterator();

                while (iterator.hasNext()) {
                    Sensor sensor = iterator.next();
                    this.changeSensorActivationStatus(sensor, false);
                }
            }
        }

        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        this.isCatDetected = cat;

        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && this.getAllSensorsFromState(false)) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if (this.securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }

        switch (this.securityRepository.getAlarmStatus()) {
            case NO_ALARM -> this.setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> this.setAlarmStatus(AlarmStatus.ALARM);
            default -> {} //do nothing if the alarm is already active
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        if (this.securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }

        switch (this.securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> this.setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> this.setAlarmStatus(AlarmStatus.PENDING_ALARM);
            default -> {} //do nothing
        }
    }

    public void changeSensorActivationStatus(Sensor sensor) {
        AlarmStatus actualAlarmStatus = this.securityRepository.getAlarmStatus();
        ArmingStatus actualArmingStatus = this.securityRepository.getArmingStatus();

        if (actualAlarmStatus == AlarmStatus.PENDING_ALARM && !sensor.getActive()) {
            this.handleSensorDeactivated();
        } else if (actualAlarmStatus == AlarmStatus.ALARM && actualArmingStatus == ArmingStatus.DISARMED) {
            this.handleSensorDeactivated();
        }
        this.securityRepository.updateSensor(sensor);
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     *
     */
    public void changeSensorActivationStatus(Sensor sensor, boolean active) {
        AlarmStatus actualAlarmStatus = this.securityRepository.getAlarmStatus();

        if (actualAlarmStatus != AlarmStatus.ALARM) {
            if (active) {
                this.handleSensorActivated();
            } else if (sensor.getActive()) {
                this.handleSensorDeactivated();
            }
        }

        // update sensor to opposite of current status
        sensor.setActive(!active);
        this.securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    private boolean getAllSensorsFromState(boolean state) {
        return getSensors()
                .stream()
                .allMatch(sensor -> sensor.getActive() == state);
    }
}
