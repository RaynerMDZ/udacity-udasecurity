package com.udacity.security.data;

import com.udacity.image.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    private final String sensorId = UUID.randomUUID().toString();
    private Sensor sensor;
    private SecurityService securityService;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    private Set<Sensor> getSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(sensorId, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @BeforeEach
    void setUpSecurityService() {
        sensor = new Sensor(sensorId, SensorType.DOOR);
        securityService = new SecurityService(securityRepository, imageService);
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivated_setAlarmIntoPendingStatus() {
        //given
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor, true);

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifAlarmIsArmedAndSensorBecomesActivatedAndSystemIsAlreadyPending_setAlarmIntoOnStatus() {
        //given
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor, true);

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifAlarmIsArmedAndSensorsAreInactive_returnToNoAlarmState() {
        //given
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);


        // when
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_ChangeInSensorStateShouldNotAffectTheAlarmState(boolean status) {
        //given
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor, status);

        //then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifASensorIsActivatedWhileAlreadyActiveAndSystemIsInPendingState_setItToAlarmState() {
        //given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        //when
        securityService.changeSensorActivationStatus(sensor, true);

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void IfASensorIsDeactivatedWhileAlreadyInactive_MakeNoChangesToTheAlarmState(AlarmStatus alarmStatus) {
        //given
        when(securityRepository.getAlarmStatus())
                .thenReturn(alarmStatus);
        sensor.setActive(false);

        //when
        securityService.changeSensorActivationStatus(sensor, false);

        //then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifImageContainsACatWhileTheSystemIsArmedHome_setSystemIntoAlarmStatus() {
        //given
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        //when
        securityService.processImage(catImage);

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifImageDoesNotContainACat_setSystemIntoAlarmStatus() {
        //given
        Set<Sensor> sensors = getSensors(3, false);
        when(securityRepository.getSensors())
                .thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(false);

        //when
        securityService.processImage(mock(BufferedImage.class));

        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifSystemIsDisarmed_setStatusToNoAlarm() {
        //given
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        //when


        //then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemIsArmed_setAllSensorsToInactive(ArmingStatus armingStatus) {
        //given
        Set<Sensor> sensors = Set.of(
                new Sensor(sensorId, SensorType.DOOR),
                new Sensor(sensorId, SensorType.WINDOW),
                new Sensor(sensorId, SensorType.MOTION)
        );

        //when
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors())
                .thenReturn(sensors);

        sensors.forEach(sensor -> sensor.setActive(true));
        securityService.setArmingStatus(armingStatus);

        //then
        securityService.getSensors()
                .forEach(sensor -> Assertions.assertFalse(sensor.getActive()));
    }

    @Test
    void ifSystemIsArmedHomeWhileTheImageIsACat_setAlarmStatusToAlarm() {
        //given
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED);

        //when
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        //then
        verify(securityRepository, times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }
























}