package io.jbnu.test;

import java.lang.reflect.Field;
import org.junit.Test;
import static org.junit.Assert.*;

/** Lifecycle regression without opening a window; native focus is checked separately. */
public class V14PolishTest {
    private static Main view(GameRules game) throws Exception {
        Main view = new Main();
        Field field = Main.class.getDeclaredField("game");
        field.setAccessible(true);
        field.set(view, game);
        return view;
    }

    @Test public void losingFocusPausesUntilExplicitResume() throws Exception {
        GameRules game = new GameRules(4);
        game.startOrNext();
        game.move(1);
        game.update(.1f);
        Main view = view(game);
        float x = game.x(), time = game.runSeconds();
        view.pause();
        assertTrue("Focus loss must pause an active run", game.paused());
        view.pause(); // Repeated lifecycle notifications must not toggle back on.
        view.resume(); // Regaining focus must not silently restart a dangerous fall.
        game.update(1);
        assertTrue(game.paused());
        assertEquals(x, game.x(), 0);
        assertEquals(time, game.runSeconds(), 0);
        assertTrue(game.togglePause());
        game.update(.1f);
        assertTrue(game.runSeconds() > time);
    }

    @Test public void shopShowsAffordabilityAndOwnedState() throws Exception {
        GameRules game = new GameRules(4);
        Main view = view(game);
        java.lang.reflect.Method status;
        try { status = Main.class.getDeclaredMethod("shopStatus", boolean.class); }
        catch (NoSuchMethodException e) { throw new AssertionError("Shop must explain price, shortage and owned items", e); }
        status.setAccessible(true);
        assertEquals("NEED 600 COINS", status.invoke(view, true));
        assertEquals("NEED 200 COINS", status.invoke(view, false));
        Field coins = GameRules.class.getDeclaredField("coins");coins.setAccessible(true);coins.set(game, 800);
        Field phase = GameRules.class.getDeclaredField("phase");phase.setAccessible(true);phase.set(game, GameRules.Phase.SHOP);
        assertEquals("BUY 600", status.invoke(view, true));
        assertTrue(game.buyCushion());
        assertEquals("OWNED", status.invoke(view, true));
        assertEquals("BUY 200", status.invoke(view, false));
        assertTrue(game.buyCoffee());
        assertEquals("OWNED", status.invoke(view, false));
        assertEquals(0, game.coins());
    }

    @Test public void titleRemainsStartableAfterFocusChange() throws Exception {
        GameRules game = new GameRules(4);
        Main view = view(game);
        view.pause();
        view.resume();
        assertFalse(game.paused());
        assertEquals(GameRules.Phase.INTRO, game.phase());
        assertTrue(game.startOrNext());
    }
}
