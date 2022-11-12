package com.udacity.security.data;

import com.udacity.image.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityServiceTest {
    private static final float CONFIDENCE_THRESHOLD = 50.0f;
    private SecurityService securityService;
    private Sensor sensor;
    private BufferedImage image;
    private final String randomUUID = UUID.randomUUID().toString();
    @Mock
    private ImageService imageService;
    //@Mock
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
        securityRepository = Mockito.mock(SecurityRepository.class, Mockito.CALLS_REAL_METHODS);
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

        // When - A sensor becomes activated
        this.securityService.changeSensorActivationStatus(this.sensor, sensor.getActive());

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
        when(this.securityRepository.getSensors())
                .thenReturn(generateSensors(1, false));
        when(this.securityRepository.areSensorsArmed())
                .thenReturn(true);

        // When - A sensor becomes activated and the system is already pending
        this.securityService.changeSensorActivationStatus(this.sensor, this.sensor.getActive());

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
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.getSensors())
                .thenReturn(generateSensors(3, true));

        // Invocation State
        // [sensor1] -> [sensor2] (2 invocations)
        // this.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // [sensor3] (1 invocation)
        // this.setAlarmStatus(AlarmStatus.NO_ALARM);

        // Sensor 1 = ACTIVE
        // Sensor 2 = ACTIVE
        // Sensor 3 = ACTIVE

        // sensor1.IsActive = FALSE // INACTIVE
        // this.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // sensor2.IsActive = FALSE // INACTIVE
        // this.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // sensor3.IsActive = FALSE // INACTIVE
        // this.setAlarmStatus(AlarmStatus.NO_ALARM);

        this.securityRepository.getSensors()
                .forEach(sensor -> this.securityService.changeSensorActivationStatus(sensor, sensor.getActive()));

        // Then - Return to no alarm state
        verify(this.securityRepository, times(2))
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(this.securityRepository, times(1))
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
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.getSensors())
                .thenReturn(generateSensors(1, sensorStatus));

        // When
        var sensor = this.securityRepository.getSensors().iterator().next();
        // this.securityService.changeSensorActivationStatus(sensor, sensor.getActive());

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
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.areSensorsArmed())
                .thenReturn(true);

        var sensor1 = new Sensor("A", SensorType.DOOR);
        sensor1.setActive(true);
        var sensor2 = new Sensor("B", SensorType.DOOR);
        sensor2.setActive(false);

        when(this.securityRepository.getSensors())
                .thenReturn(new HashSet<>(Arrays.asList(sensor1, sensor2)));

        // When - a sensor is activated while already active
        var sensors = this.securityRepository.getSensors().stream()
                .sorted(Comparator.comparing(Sensor::getName))
                .collect(Collectors.toList());

        var sensor = sensors.get(1);
        this.securityService.changeSensorActivationStatus(sensor, sensor.getActive());

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
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED);

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
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED);

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
        when(this.securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(this.securityRepository.getSensors())
                .thenReturn(generateSensors(3, true));

        // When - Set arming status to armed
        this.securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Then - Set sensors to inactive
        verify(this.securityRepository, times(1))
                .setAllSensorsInactive();
        verify(this.securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(this.securityRepository, times(1))
                .setArmingStatus(ArmingStatus.ARMED_HOME);
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