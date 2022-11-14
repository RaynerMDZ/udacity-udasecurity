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

        // GOOD
        if (armingStatus == ArmingStatus.DISARMED) {
            this.setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        // When ARMED
        else if (armingStatus == ArmingStatus.ARMED_HOME) {

            // if already armed, do nothing
            if (this.securityRepository.getArmingStatus() == ArmingStatus.ARMED_HOME) {
                // set all sensors to inactive
                this.securityRepository.setAllSensorsInactive();
                this.setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        // GOOD
        // if the system is armed-home while a cat is detected, the alarm should be set to alarm
        else {
            // Prevents the systems to hit a ConcurrentModificationException when two sensors are modified on the same thread.
            // Implemented using a fail-safe iterator.
            List<Sensor> sensors = new CopyOnWriteArrayList<>(this.getSensors());
            Iterator<Sensor> iterator = sensors.iterator();

            while (iterator.hasNext()) {
                Sensor sensor = iterator.next();

                if (sensor.getActive()) {
                    this.changeSensorActivationStatus(sensor, sensor.getActive());
                }
            }
        }

        // if system is armed, reset all sensors to inactive
        if (armingStatus == ArmingStatus.ARMED_AWAY) {
            for (Sensor sensor : securityRepository.getSensors()) {
                sensor.setActive(false);
            }
        }

        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        this.isCatDetected = cat;

        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            this.setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && this.getAllSensorsFromState(false)) {
            this.setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        this.statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        this.statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        this.statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        this.securityRepository.setAlarmStatus(status);
        this.statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if (this.securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }

        if (this.securityRepository.getArmingStatus() == ArmingStatus.ARMED_HOME) {
            if (this.securityRepository.areSensorsArmed()) {
                this.setAlarmStatus(AlarmStatus.ALARM);
            } else {
                this.setAlarmStatus(AlarmStatus.PENDING_ALARM);
            }
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        if (this.securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }

        // if all sensors are inactive
        if (this.getAllSensorsFromState(false)) {
            this.setAlarmStatus(AlarmStatus.NO_ALARM);
        } else if (!this.securityRepository.areSensorsArmed()) {
            this.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, boolean active) {
//        AlarmStatus actualAlarmStatus = this.securityRepository.getAlarmStatus();

        // update sensor to opposite of current status
        boolean previousState = sensor.getActive();
        sensor.setActive(!active);
        this.securityRepository.updateSensor(sensor);

        if (!active) {
            this.handleSensorActivated();
        } else if (previousState) {
            this.handleSensorDeactivated();
        }

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
        if (this.securityRepository.getSensors().size() > 0 && this.securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            this.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);

        // Case 1
        // List Sensors:
        // 1. Sensor 1 = ACTIVE
        // 2. Sensor 2 = INACTIVE
        // 3. Sensor 3 = INACTIVE

        // System Status: PENDING
        // Remove(1)
        // System Status: NO_ALARM

        // Case 2
        // List Sensors:
        // 1. Sensor 1 = ACTIVE
        // 2. Sensor 2 = ACTIVE

        // System Status: ALARM
        // Remove(1)
        // System Status: ALARM

        // Case 3 - check
        // List Sensors:
        // 1. Sensor 1 = ACTIVE
        // 2. Sensor 2 = INACTIVE

        // System Status: PENDING
        // Remove(2)
        // System Status: ALARM

        // Case 4 - check
        // List Sensors:
        // 1. Sensor 1 = ACTIVE

        // System Status: ALARM
        // Remove(1)
        // System Status: NO_ALARM

        // Case 5 - check
        // List Sensors:
        // 1. Sensor 1 = INACTIVE

        // System Status: NO_ALARM
        // Remove(1)
        // System Status: NO_ALARM

        // Case 6
        // List Sensors:
        // 1. Sensor 1 = ACTIVE
        // 2. Sensor 2 = INACTIVE

        // System Status: PENDING
        // Remove(1)
        // System Status: NO_ALARM

       if (this.securityRepository.getSensors().isEmpty()) {
            this.setAlarmStatus(AlarmStatus.NO_ALARM);
       }
       else if (this.securityRepository.areSensorsArmed()) {
           this.setAlarmStatus(AlarmStatus.ALARM);

       } else if (!this.securityRepository.areSensorsArmed() && this.securityRepository.getSensors().size() == 1) {
           this.setAlarmStatus(AlarmStatus.NO_ALARM);
       }
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
