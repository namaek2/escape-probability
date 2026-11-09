package io.jbnu.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/** Model contracts only; rendering/title art require the parent's visual verification. */
public class V13ContractTest {
    private static final class CountingRandom extends Random {
        int calls;
        double value;
        @Override public double nextDouble() { calls++; return value; }
    }

    private static Object api(Class<?> owner, String name, Class<?> argument, Object value) {
        Method method;
        try {
            method = owner.getMethod(name, argument);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Missing V13 API: " + owner.getSimpleName() + "." + name, e);
        }
        assertTrue(name + " must be static", Modifier.isStatic(method.getModifiers()));
        try {
            return method.invoke(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("V13 API failed: " + name, e);
        }
    }

    private static int floors(int stage) {
        Object value = api(Terrain.class, "floorCount", int.class, stage);
        assertTrue("floorCount returns int", value instanceof Integer);
        return (Integer) value;
    }

    private static float[][] platforms(int stage) {
        Object value = api(Terrain.class, "platforms", int.class, stage);
        assertTrue("platforms returns float[][]", value instanceof float[][]);
        float[][] rows = (float[][]) value;
        for (float[] row : rows) {
            assertNotNull("platform row", row);
            assertEquals("[left,right,topY,localDepth]", 4, row.length);
            for (float v : row) assertTrue("finite geometry", Float.isFinite(v));
            assertTrue(row[0] >= 0 && row[0] < row[1] && row[1] <= Terrain.WIDTH);
            assertEquals("integer local depth", Math.rint(row[3]), row[3], 0);
            assertTrue(row[3] >= 0 && row[3] <= floors(stage));
            assertEquals(-160f * row[3], row[2], 0);
        }
        return rows;
    }

    private static List<Integer> at(float[][] rows, int depth) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) if (rows[i][3] == depth) ids.add(i);
        ids.sort(Comparator.comparingDouble(i -> rows[i][0]));
        return ids;
    }

    private static void set(GameRules game, String name, Object value) {
        try {
            Field field = GameRules.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(game, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Missing unit-fixture field: " + name, e);
        }
    }

    private static GameRules ready(CountingRandom random, int stage) {
        try {
            Constructor<GameRules> ctor = GameRules.class.getDeclaredConstructor(long.class, Random.class);
            int access = ctor.getModifiers();
            assertEquals("RNG injection remains package-private", 0,
                    access & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE));
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Missing package-private RNG test constructor", e);
        }
        GameRules game = new GameRules(4, random);
        assertTrue(game.startOrNext());
        for (int s = 0; s < stage; s++) {
            set(game, "phase", GameRules.Phase.SHOP);
            assertTrue(game.leaveShop());
        }
        return game;
    }

    private static void airborne(GameRules game, float x, float y) {
        set(game, "x", x);
        set(game, "y", y);
        set(game, "vy", 0f);
        set(game, "grounded", false);
        set(game, "support", -1);
        game.move(0);
    }

    private static void until(GameRules game, GameRules.Phase phase) {
        for (int i = 0; i < 400 && game.phase() != phase; i++) game.update(.01f);
        assertEquals("event must reach " + phase, phase, game.phase());
    }

    private static float middle(float[] row) { return (row[0] + row[1]) / 2; }

    @Test public void introIsInertAndSavedRevealDoesNotConsumeEarly() {
        CountingRandom random = new CountingRandom();random.value=.99;
        GameRules game = new GameRules(4, random);
        assertFalse(game.togglePause());assertFalse(game.paused());assertFalse(game.jump());
        game.move(1);game.update(5);assertEquals(300,game.x(),0);assertEquals(0,game.runSeconds(),0);
        assertTrue(game.startOrNext());set(game,"cushion",true);
        float[] row=platforms(0)[3];airborne(game,middle(row),row[2]+1);
        until(game,GameRules.Phase.SPINNING);
        assertEquals("DEAD",game.rawResult());assertEquals("SAVED",game.revealResult());
        assertEquals(1,random.calls);float world=game.worldSeconds(),elapsed=game.runSeconds();
        game.update(.2f);assertEquals(GameRules.Phase.SPINNING,game.phase());
        assertEquals(world,game.worldSeconds(),0);assertTrue(game.runSeconds()>elapsed);
        until(game,GameRules.Phase.REVEAL);
        assertEquals(GameRules.wheelRotation(game.sample()),game.wheelRotation(),0);
        assertTrue(game.cushion());assertEquals("SAVED",game.revealResult());assertEquals(0,game.coins());
        game.update(.1f);assertEquals(GameRules.Phase.REVEAL,game.phase());
        until(game,GameRules.Phase.READY);assertFalse(game.cushion());assertEquals("SAVED",game.appliedResult());
        assertEquals(0,game.coins());assertEquals(1,random.calls);
    }

    @Test public void nextBuildingStartsAtLocalRoofAndBanksPurchases() {
        CountingRandom random = new CountingRandom();
        GameRules game = ready(random, 0);
        set(game, "phase", GameRules.Phase.SHOP);
        set(game, "coins", 500);
        assertTrue(game.leaveShop());
        assertEquals(1, game.stage());
        assertEquals("next roof is not V12 global anchor 8", 0, game.anchor());
        assertEquals(0, game.depth());
        assertEquals(0, game.y(), 0);
        assertEquals(500, game.stageBank());
        assertEquals(10, floors(0));
        assertEquals(20, floors(1));
        assertEquals(30, floors(2));

        // Actual downward ground contact, not a synthetic shop transition.
        for (int stage = 1; stage <= 2; stage++) {
            float[][] rows = platforms(stage);
            List<Integer> ground = at(rows, floors(stage));
            assertFalse("ground exists", ground.isEmpty());
            float[] row = rows[ground.get(0)];
            airborne(game, middle(row), row[2] + 1);
            until(game, GameRules.Phase.SPINNING);
            until(game, stage == 2 ? GameRules.Phase.ALL_CLEAR : GameRules.Phase.SHOP);
            assertEquals(floors(stage), game.anchor());
            if (stage == 1) {
                assertTrue(game.buyCoffee());
                int bank = game.coins();
                assertTrue(game.leaveShop());
                assertEquals(bank, game.stageBank());
                random.value = .99;
                airborne(game, game.x(), -160f * floors(2) - 241);
                until(game, GameRules.Phase.DEAD);
                assertEquals(bank, game.coins());
                until(game, GameRules.Phase.READY);
                assertEquals(2, game.stage());
                assertEquals(0, game.anchor());
                assertEquals(0, game.y(), 0);
                random.value = 0;
            }
        }
        game.restart();
        assertEquals(0, game.stage());
        assertEquals(0, game.anchor());
        assertEquals(0, game.stageBank());
        assertEquals(0, game.coins());
    }

    @Test public void separatedLedgesHaveRealGapsAndSameFloorCannotFarm() {
        for (int stage = 0; stage < 3; stage++) {
            float[][] rows = platforms(stage);
            assertFalse("roof exists", at(rows, 0).isEmpty());
            assertFalse("ground exists", at(rows, floors(stage)).isEmpty());
            for (int depth = 1; depth < floors(stage); depth++) {
                List<Integer> ids = at(rows, depth);
                assertTrue("at least three ledges on each ordinary floor", ids.size() >= 3);
                float right = -1;
                for (int id : ids) {
                    float[] row = rows[id];
                    assertTrue("short ledge", row[1] - row[0] < 180);
                    assertTrue("strictly positive gap", row[0] > right);
                    right = row[1];
                }
            }
        }
        float[][] rows = platforms(0);
        List<Integer> ids = at(rows, 1);
        CountingRandom random = new CountingRandom();
        GameRules game = ready(random, 0);
        float[] first = rows[ids.get(0)], second = rows[ids.get(1)];
        airborne(game, middle(first), first[2] + 1);
        until(game, GameRules.Phase.SPINNING);
        until(game, GameRules.Phase.READY);
        assertEquals(ids.get(0).intValue(), game.support());
        int coins = game.coins(), streak = game.streak(), calls = random.calls;
        assertTrue(game.jump());
        // Fixture isolates same-floor contact; it does not prescribe a movement controller.
        airborne(game, middle(second), second[2] + 1);
        game.update(.1f);
        assertTrue(game.grounded());
        assertEquals(ids.get(1).intValue(), game.support());
        assertEquals(calls, random.calls);
        assertEquals(coins, game.coins());
        assertEquals(streak, game.streak());
        set(game, "x", (first[1] + second[0]) / 2);
        game.update(.1f);
        assertFalse("no floor-wide support across a gap", game.grounded());
        assertEquals(-1, game.support());
        assertTrue(game.y() < first[2]);
    }

    @Test public void collapseBelongsOnlyToContactedSupportAndFreezes() {
        CountingRandom random = new CountingRandom();
        GameRules game = ready(random, 2);
        float[][] rows = platforms(2);
        List<Integer> ids = at(rows, 1);
        assertTrue(ids.size() >= 3);
        int id = ids.get(0);
        airborne(game, middle(rows[id]), rows[id][2] + 1);
        until(game, GameRules.Phase.SPINNING);
        assertEquals(id, game.support());
        assertTrue(game.collapse(id) >= 0);
        float timer = game.collapse(id);
        game.update(.2f);
        assertEquals(timer, game.collapse(id), 0);
        until(game, GameRules.Phase.READY);
        assertEquals(1, random.calls);
        assertEquals(100, game.coins());
        for (int other : ids) if (other != id) assertEquals(-1, game.collapse(other), 0);
        game.togglePause();
        timer = game.collapse(id);
        game.update(10);
        assertEquals(timer, game.collapse(id), 0);
        game.togglePause();
        for (int i = 0; i < 400 && !game.broken(id); i++) game.update(.01f);
        assertTrue(game.broken(id));
        assertFalse(game.grounded());
        assertEquals(-1, game.support());
        for (int other : ids) if (other != id) assertEquals(-1, game.collapse(other), 0);
        assertEquals("animation never draws cosmetic randomness", 1, random.calls);
    }

    @Test public void survivalAndDeathTextRemainTruthfulThroughThirtyFloors() {
        assertEquals(.9, GameRules.survival(1), 1e-15);
        assertEquals(.729, GameRules.survival(2), 1e-15);
        assertEquals(.531441, GameRules.survival(3), 1e-15);
        double previous = 1;
        for (int n = 1; n <= 30; n++) {
            double q = GameRules.survival(n);
            assertTrue(q > 0 && q < previous);
            assertEquals(Math.pow(.9, n * (n + 1.0) / 2), q, q * 1e-14);
            previous = q;
            Object text = api(GameRules.class, "deathPercent", double.class, q);
            assertTrue("deathPercent returns text", text instanceof String);
            String label = ((String) text).trim();
            assertFalse("death text must not be empty", label.isEmpty());
            assertFalse("positive survival must not display certain death: " + label,
                    label.matches("100(?:\\.0+)?\\s*%"));
            if (n == 1) assertEquals("10.00%", label);
            if (1 - q == 1) assertTrue("tiny q needs a truthful bound or survival qualification",
                    label.contains(">99.99") || label.contains("<100") || label.toLowerCase(java.util.Locale.ROOT).contains("surviv"));
        }
    }

    @Test public void stoppedWheelProjectsTheActualSampleWithoutFixedSlots() {
        final double epsilon = 1e-12;
        for (double q : new double[]{.9, .729, GameRules.survival(30)}) {
            for (double sample : new double[]{0, q / 2, Math.nextDown(q), q,
                    Math.nextUp(q), .123456789, .5, Math.nextDown(1.0)}) {
                Object rotationValue = api(GameRules.class, "wheelRotation", double.class, sample);
                assertTrue("double rotation", rotationValue instanceof Double);
                double rotation = (Double) rotationValue;
                assertTrue(Double.isFinite(rotation));
                Object fractionValue = api(GameRules.class, "wheelFractionAtPointer", double.class, rotation);
                assertTrue("double pointer fraction", fractionValue instanceof Double);
                double fraction = (Double) fractionValue;
                assertTrue(fraction >= 0 && fraction < 1);
                double error = Math.abs(sample - fraction);
                assertTrue("continuous circular projection, not fixed sectors", Math.min(error, 1 - error) <= epsilon);
                // Subpixel wedges are not enlarged; classification is checked outside projection roundoff.
                if (Math.abs(sample - q) > epsilon && sample > epsilon && sample < 1 - epsilon)
                    assertEquals("pointer agrees with unquantized sample < q", sample < q, fraction < q);
            }
        }
    }
}
