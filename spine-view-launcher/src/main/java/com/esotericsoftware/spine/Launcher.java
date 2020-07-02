package com.esotericsoftware.spine;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import java.awt.*;

public class Launcher {

    public static void main(String[] args) {
        SkeletonControlBoard.args = args;
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
        String os = System.getProperty("os.name");
        float dpiScale = 1;
        if (os.contains("Windows")) dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96f;
        if (os.contains("OS X")) {
            Object object = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
            if (object instanceof Float && ((Float)object).intValue() >= 2) dpiScale = 2;
        }
        if (dpiScale >= 2.0f) SkeletonControlBoard.uiScale = 2;

        LwjglApplicationConfiguration.disableAudio = true;
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = (int)(800 * SkeletonControlBoard.uiScale);
        config.height = (int)(600 * SkeletonControlBoard.uiScale);
        config.title = "Skeleton Viewer";
        config.allowSoftwareMode = true;
        config.samples = 2;
        new LwjglApplication(new SkeletonControlBoard(), config);
    }
}
