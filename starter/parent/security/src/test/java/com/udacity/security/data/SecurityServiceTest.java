package com.udacity.security.data;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.udacity.image.service.FakeImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private static final int PORT = 9090;
    private static final String URL = "http://localhost:" + PORT;
    private SecurityService securityService;
    @Mock
    private FakeImageService fakeImageService;
    @Mock
    private SecurityRepository securityRepository;
    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(PORT));

    @BeforeEach
    void setUpSecurityService() {
        wireMockServer.resetAll();
        securityService = new SecurityService(securityRepository, fakeImageService);
    }

    @BeforeAll
    static void setUp() {
        wireMockServer.start();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }


    @Test
    void setArmingStatus() {
    }

    @Test
    void addStatusListener() {
    }

    @Test
    void removeStatusListener() {
    }

    @Test
    void setAlarmStatus() {
    }

    @Test
    void changeSensorActivationStatus() {
    }

    @Test
    void processImage() {
    }

    @Test
    void getAlarmStatus() {
    }

    @Test
    void getSensors() {
    }

    @Test
    void addSensor() {
    }

    @Test
    void removeSensor() {
    }

    @Test
    void getArmingStatus() {
    }
}