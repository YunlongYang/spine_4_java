package com.esotericsoftware.spine.superspineboy;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Launcher {
    public static void main (String[] args) throws Exception {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Super Spineboy";
        config.width = 800;
        config.height = 450;
        new LwjglApplication(new SuperSpineboy(), config);
    }
}
