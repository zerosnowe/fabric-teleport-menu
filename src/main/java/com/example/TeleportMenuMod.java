package com.example;

import com.example.server.ServerLog;
import com.example.server.command.CommandRegistrar;
import com.example.server.event.ServerEventRegistrar;
import com.example.server.network.ServerNetworkRegistrar;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

/**
 * Server entrypoint. All behaviour lives in {@code com.example.server.*} services.
 */
public class TeleportMenuMod implements ModInitializer {
	public static final String MOD_ID = "teleportmenu";
	public static final Logger LOGGER = ServerLog.LOGGER;

	@Override
	public void onInitialize() {
		ServerNetworkRegistrar.register();
		CommandRegistrar.register();
		ServerEventRegistrar.register();
		LOGGER.info("Teleport Menu 2.3 initialized");
	}
}
