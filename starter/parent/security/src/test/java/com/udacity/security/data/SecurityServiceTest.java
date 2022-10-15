package com.udacity.security.data;

import com.udacity.image.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private Set<Sensor> sensors;
    private SecurityService securityService;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;


    @BeforeEach
    void setUp() {
        sensors = Set.of(
                new Sensor("Door", SensorType.DOOR),
                new Sensor("Window", SensorType.WINDOW),
                new Sensor("Motion", SensorType.MOTION));

        securityService = new SecurityService(securityRepository, imageService);
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivated_setAlarmIntoPendingStatus() {
        // given
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        var sensor = sensors.iterator().next();
        sensor.setActive(true);

        // when
        securityService.changeSensorActivationStatus(sensor);

        // then
        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivatedAndSystemIsAlreadyPending_setAlarmIntoOnStatus() {
    }

    @Test
    void ifAlarmIsArmedAndSensorsAreInactive_returnToNoAlarmState() {
        // given
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        var sensor = sensors.iterator().next();
        sensor.setActive(false);

        // then
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifAlarmIsActive_ChangeInSensorStateShouldNotAffectTheAlarmState() {
    }

    @Test
    void ifASensorIsActivatedWhileAlreadyActiveAndSystemIsInPendingState_setItToAlarmState() {

    }

    @Test
    void IfASensorIsDeactivatedWhileAlreadyInactive_MakeNoChangesToTheAlarmState(AlarmStatus alarmStatus) {
    }

    @Test
    void ifImageContainsACatWhileTheSystemIsArmedHome_setSystemIntoAlarmStatus() {
    }

    @Test
    void ifImageDoesNotContainACat_setSystemIntoAlarmStatus() {
    }

    @Test
    void ifSystemIsDisarmed_setStatusToNoAlarm() {
    }

    @Test
    void ifSystemIsArmed_setAllSensorsToInactive(ArmingStatus armingStatus) {

    }

    @Test
    void ifSystemIsArmedHomeWhileTheImageIsACat_setAlarmStatusToAlarm() {
    }
























}