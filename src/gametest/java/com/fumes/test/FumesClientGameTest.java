package com.fumes.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/** Builds rows of every gas at concentration 1..8 (left to right) with ticks frozen, and screenshots them. */
public class FumesClientGameTest implements FabricClientGameTest {

    private static final String[] GASES = {"methane", "hydrogen_sulfide", "carbon_dioxide", "carbon_monoxide", "steam"};

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            TestServerContext server = sp.getServer();
            context.waitTicks(40);
            server.runCommand("gamerule doDaylightCycle false");
            server.runCommand("time set noon");
            server.runCommand("tick freeze");
            int x0 = 0, y = -58, z0 = 10;
            server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone_bricks", x0 - 1, y - 1, z0 - 1, x0 + 16, y - 1, z0 + 10));
            for (int g = 0; g < GASES.length; g++)
                for (int a = 1; a <= 8; a++) {
                    int x = x0 + (a - 1) * 2, z = z0 + g * 2;
                    server.runCommand(String.format("fill %d %d %d %d %d %d fumes:%s[amount=%d]", x, y, z, x + 1, y + 1, z, GASES[g], a));
                }
            server.runCommand("gamemode spectator @p");
            server.runCommand(String.format("tp @p %d %d %d 0 30", x0 + 8, y + 9, z0 - 9));
            context.waitTicks(220);
            context.takeScreenshot(TestScreenshotOptions.of("fumes_day").withSize(1280, 720));
            server.runCommand(String.format("tp @p %d %d %d 90 15", x0 + 22, y + 4, z0 + 5));
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("fumes_side").withSize(1280, 720));
            server.runCommand("time set midnight");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("fumes_night").withSize(1280, 720));
        }
    }
}
