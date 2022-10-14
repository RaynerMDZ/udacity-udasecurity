package com.udacity.image.service;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageServiceImpl implements ImageService {
    private final Random random = new Random();

    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        return random.nextBoolean();
    }
}
