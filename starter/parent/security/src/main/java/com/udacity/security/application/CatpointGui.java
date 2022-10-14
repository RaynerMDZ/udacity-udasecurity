package com.udacity.security.application;

import com.udacity.image.service.FakeImageServiceImpl;
import com.udacity.image.service.ImageService;
import com.udacity.security.data.SecurityService;
import com.udacity.security.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.security.data.SecurityRepository;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * This is the primary JFrame for the application that contains all the top-level JPanels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as necessary.
 */
public class CatpointGui extends JFrame {
    private static final String CONSTRAINT = "wrap";
    private static final String TITLE = "Catpoint Security System";
    private final SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
    private final ImageService imageService = new FakeImageServiceImpl();
    private final SecurityService securityService = new SecurityService(securityRepository, imageService);
    private DisplayPanel displayPanel = new DisplayPanel(securityService);
    private ControlPanel controlPanel = new ControlPanel(securityService);
    private SensorPanel sensorPanel = new SensorPanel(securityService);
    private ImagePanel imagePanel = new ImagePanel(securityService);

    public CatpointGui() {
        setLocation(100, 100);
        setSize(600, 850);
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());
        mainPanel.add(displayPanel, CONSTRAINT);
        mainPanel.add(imagePanel, CONSTRAINT);
        mainPanel.add(controlPanel, CONSTRAINT);
        mainPanel.add(sensorPanel);

        getContentPane().add(mainPanel);
    }
}
