package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.GDXRoot;
import com.mygdx.game.brains.BeeGenotype;

public class DesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "BeeGame";
        config.width  = 1600;
        config.height = 900;



        new LwjglApplication(new GDXRoot(), config);
    }
}
