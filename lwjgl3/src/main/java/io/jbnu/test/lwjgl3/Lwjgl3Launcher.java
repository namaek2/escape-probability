package io.jbnu.test.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.jbnu.test.Main;
import io.jbnu.test.Terrain;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("escape probability");
        configuration.useVsync(true);
        // Headless displays can report 0 Hz. A fixed 60 FPS keeps normal gameplay responsive.
        configuration.setForegroundFPS(60);
        configuration.setPauseWhenLostFocus(true);
        configuration.setPauseWhenMinimized(true);
        com.badlogic.gdx.Graphics.DisplayMode display=Lwjgl3ApplicationConfiguration.getDisplayMode();
        int[] fit=Terrain.windowSize(display.width,display.height);
        System.setProperty("roulette.displayWidth",Integer.toString(display.width));
        System.setProperty("roulette.displayHeight",Integer.toString(display.height));
        int width = Math.min(Integer.getInteger("roulette.qa.width", fit[0]),display.width-80);
        int height = Math.min(Integer.getInteger("roulette.qa.height", fit[1]),display.height-100);
        configuration.setWindowedMode(width, height);
        Integer x = Integer.getInteger("roulette.qa.windowX");
        Integer y = Integer.getInteger("roulette.qa.windowY");
        if (x != null && y != null) configuration.setWindowPosition(x, y);
        System.out.println("escape probability foregroundFPS=60 window=" + width + "x" + height);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
