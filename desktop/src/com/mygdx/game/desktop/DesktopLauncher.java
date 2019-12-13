package com.mygdx.game.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.BeeApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.GDXRoot;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "BeeGame";
		config.width  = 1600;
		config.height = 900;

		BeeApplication app = new BeeApplication(new GDXRoot(), config);
		while(app.running) {

		}
		System.out.println("\n" + app.getScore());

//		new LwjglApplication(new GDXRoot(), config);
	}
}
