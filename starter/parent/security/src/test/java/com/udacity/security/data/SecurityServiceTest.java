package com.udacity.security.data;

import com.udacity.image.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    private static final float CONFIDENCE_THRESHOLD = 50.0f;
    private SecurityService securityService;
    private Sensor sensor;
    private BufferedImage image;
    private final String randomUUID = UUID.randomUUID().toString();
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    private Set<Sensor> generateSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(randomUUID, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @BeforeEach
    void setUp() {
        this.securityService = new SecurityService(securityRepository, imageService);
        this.sensor = new Sensor(this.randomUUID, SensorType.DOOR);
        this.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * This test is from the video lesson provided on Slack.
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @Test
    void ifAlarmIsArmedAndSensorBecomesActivated_setAlarmIntoPendingStatus() {
        // Given - System status is armed
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        // When - A sensor becomes activated
        this.securityService.changeSensorActivationStatus(this.sensor, true);

        // Then - Put the alarm into pending status
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
     */
    @Test
    void ifAlarmIsArmedAndSensorBecomesActivatedAndSystemIsAlreadyPending_setAlarmIntoOnStatus() {
        // Given - System alarm is armed
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        // When - A sensor becomes activated and the system is already pending
        this.securityService.changeSensorActivationStatus(this.sensor, true);

        // Then - Put the alarm into on status
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    void ifAlarmIsPendingAndAllSensorsAreInactive_returnToNoAlarmState() {
        // Given - System alarm is armed
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        // When - All sensors are inactive
        Set<Sensor> sensors = this.generateSensors(5, false);
        sensors.forEach(sensor -> this.securityService.changeSensorActivationStatus(sensor));

        // Then - Return to no alarm state
        verify(this.securityRepository, times(5))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * This test is from the video lesson provided on Slack.
     * If alarm is active, change in sensor state should not affect the alarm state.
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_ChangeInSensorStateShouldNotAffectTheAlarmState(boolean sensorStatus) {
        // Given
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        // When
        this.sensor.setActive(sensorStatus);
        this.securityService.changeSensorActivationStatus(this.sensor, sensorStatus);

        // Then
        verify(this.securityRepository, never())
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    void ifASensorIsActivatedWhileAlreadyActiveAndSystemIsInPendingState_setItToAlarmState() {
        // Given - when the system is in pending state
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        // When - a sensor is activated while already active
        this.sensor.setActive(true);
        this.securityService.changeSensorActivationStatus(this.sensor, true);

        // Then - change it to alarm state
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * This test is from the video lesson provided on Slack.
     * If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    void IfASensorIsDeactivatedWhileAlreadyInactive_MakeNoChangesToTheAlarmState(AlarmStatus alarmStatus) {
        // Given
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(alarmStatus);

        // When
        this.sensor.setActive(false);
        this.securityService.changeSensorActivationStatus(this.sensor, false);

        // Then
        verify(this.securityRepository, never())
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
     */
    @Test
    void ifImageContainsACatWhileTheSystemIsArmedHome_setSystemIntoAlarmStatus() {
        // Given - System is armed-home
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.imageService.imageContainsCat(this.image, CONFIDENCE_THRESHOLD))
                .thenReturn(true);

        // When
        this.securityService.processImage(this.image);
        this.securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Then
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
     */
    @Test
    void ifImageDoesNotContainACat_setSystemIntoAlarmStatus() {
        // Given - Image does not contain a cat
        when(this.imageService.imageContainsCat(this.image, CONFIDENCE_THRESHOLD))
                .thenReturn(false);

        // When - process image
        this.securityService.processImage(this.image);

        // Then - Change the status to no alarm as long as the sensors are not active
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    /**
     * If the system is disarmed, set the status to no alarm.
     */
    @Test
    void ifSystemIsDisarmed_setStatusToNoAlarm() {
        // Given - System is disarmed
//        when(securityRepository.getArmingStatus())
//                .thenReturn(ArmingStatus.DISARMED);

        // When - System is disarmed
        this.securityService.setArmingStatus(ArmingStatus.DISARMED);

        // Then
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * If the system is armed, reset all sensors to inactive.
     */
    @Test
    void ifSystemIsArmed_setAllSensorsToInactive() {
        // Given - System is armed, generate 3 sensors
        when(this.securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);
        when(this.securityRepository.getSensors())
                .thenReturn(generateSensors(3, true));

        // When - Set arming status to armed
        this.securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Then - Set sensors to inactive
        this.securityRepository.getSensors()
                .forEach(sensor -> this.securityService.changeSensorActivationStatus(sensor, false));

        for (Sensor sensor : this.securityRepository.getSensors()) {
            assertFalse(sensor.getActive());
        }
    }

    /**
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    void ifSystemIsArmedHomeWhileTheImageIsACat_setAlarmStatusToAlarm() {
        // Given - System is armed-home and the image is a cat
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(this.image, CONFIDENCE_THRESHOLD))
                .thenReturn(true);

        // When - The system is armed-home and the image is a cat
        securityService.processImage(this.image);

        // Then - Set the alarm status to alarm
        verify(securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }
}