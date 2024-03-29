/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter. 
 * There must be some undocumented OpenGL code in setScreen.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
 package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.assets.loaders.*;
import com.badlogic.gdx.assets.loaders.resolvers.*;

import com.mygdx.game.bees.BeeController;
import com.mygdx.game.brains.BeeBrain;
import com.mygdx.game.util.ScreenListener;

import javax.swing.*;

/**
 * Root class for a LibGDX.  
 * 
 * This class is technically not the ROOT CLASS. Each platform has another class above
 * this (e.g. PC games use DesktopLauncher) which serves as the true root.  However, 
 * those classes are unique to each platform, while this class is the same across all 
 * plaforms. In addition, this functions as the root class all intents and purposes, 
 * and you would draw it as a root class in an architecture specification.  
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	private AssetManager manager;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas; 
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private int current;
	/** List of all WorldControllers */
	private WorldController controller;

	private boolean isRunning;
	private boolean isLoading;

	private BeeBrain[] brains;
	
	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot(BeeBrain[] brains) {
		// Start loading with the asset manager
		manager = new AssetManager();
		this.brains = brains;
		isRunning = true;
		isLoading = true;
		// Add font support to the asset manager
	}

	public GDXRoot() {
		// Start loading with the asset manager
		manager = new AssetManager();
		isRunning = true;
		isLoading = true;
		resetting = false;
		// Add font support to the asset manager
	}

	public void set(BeeBrain[] brains) {
		manager = new AssetManager();
		this.brains = brains;
		isRunning = true;
		isLoading = true;
		resetting = true;

		if(controller != null) {
			((BeeController) controller).giveBrains(brains);
		} else {
			controller =  new BeeController();
			((BeeController)controller).giveBrains(brains);
		}

		controller.resume();
		controller.setResetting(false);
		resetting = false;
	}

	public void reset(BeeBrain[] brains) {
		resetting = true;
		manager = new AssetManager();
		this.brains = brains;
		isRunning = true;
		isLoading = true;

		if(controller != null) {
			controller.pause();
			controller.setResetting(true);
			controller.reset();
			((BeeController) controller).giveBrains(brains);
		} else {
			controller =  new BeeController();
			((BeeController)controller).giveBrains(brains);
		}
        controller.setResetting(false);
        controller.resume();
		resetting = false;
	}

	private boolean resetting;
	public boolean resetting() {return resetting;}

	public boolean isRunning() { return ((BeeController)controller).isRunning(); }
	public boolean isLoading() { return isLoading; }
	public double[] getScores() {
		brains = ((BeeController)controller).getBrains();

		double[] scores = new double[brains.length];
		int i = 0;
		for(BeeBrain brain : brains) {
			scores[i] = brain.getScore();
			i++;
		}
		return scores;
	}

	public double secondsElapsed() { return controller.time; }

	public void reset() {
		controller.pause();
		controller.setResetting(true);
		controller.reset();
		controller = null;
		brains = null;
		manager = new AssetManager();
		//controller.setResetting(false);
		//controller.resume();
	}

	/** 
	 * Called when the Application is first created.
	 * 
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas  = new GameCanvas();
		loading = new LoadingMode(canvas,manager,1);
		isRunning = true;
		// Initialize the three game worlds
		controller =  new BeeController();
		((BeeController)controller).giveBrains(brains);
		controller.preLoadContent(manager);
		current = 0;
		loading.setScreenListener(this);
		//todo:get rid of loading screen
		setScreen(loading);
	}


	public void pause() {
		// Call dispose on our children
		setScreen(null);
		controller.pause();
		super.pause();
	}

	/** 
	 * Called when the Application is destroyed. 
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		setScreen(null);
		controller.dispose();

		canvas.dispose();
		canvas = null;
	
		// Unload all of the resources
		manager.clear();
		manager.dispose();
		super.dispose();
		isRunning = false;
	}
	
	/**
	 * Called when the Application is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize();
		super.resize(width,height);
	}
	
	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (screen == loading) {
			controller.loadContent(manager);
			controller.setScreenListener(this);
			controller.setCanvas(canvas);
			controller.reset();
			setScreen(controller);
			
			loading.dispose();
			loading = null;
			isLoading = false;
		} else if (exitCode == WorldController.EXIT_QUIT) {
			// We quit the main application
			Gdx.app.exit();
		}
	}

}
