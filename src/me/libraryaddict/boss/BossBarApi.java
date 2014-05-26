package me.libraryaddict.boss;

import java.lang.reflect.Field;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public class BossBarApi {
    private static int enderdragonId;
    private static Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];
    private static HashMap<String, BukkitRunnable> toHide = new HashMap<String, BukkitRunnable>();
    static {
        try {
            Field field = Class.forName(
                    "net.minecraft.server." + Bukkit.getServer().getClass().getName().split("\\.")[3] + ".Entity")
                    .getDeclaredField("entityCount");
            field.setAccessible(true);
            enderdragonId = field.getInt(null);
            field.set(null, enderdragonId + 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void removeBar(Player player) {
        removeBar(player, 2);
    }

    public static void removeBar(final Player player, int afterTicks) {
        if (player.hasMetadata("SeesEnderdragon") && !toHide.containsKey(player.getName())) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("SeesEnderdragon", plugin);
                    sendRemovePacket(player);
                    toHide.remove(player.getName());
                }
            };
            runnable.runTaskLater(plugin, afterTicks);
            toHide.put(player.getName(), runnable);
        }
    }

    private static void sendRemovePacket(Player player) {
        try {
            PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            spawnPacket.getIntegerArrays().write(0, new int[] { enderdragonId });
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawnPacket, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void sendSpawnPacket(Player player, String message, float health) throws Exception {
        PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
        StructureModifier<Object> spawnPacketModifier = spawnPacket.getModifier();
        spawnPacketModifier.write(0, enderdragonId);
        spawnPacketModifier.write(1, (byte) 63); // EntityID of wither
        spawnPacketModifier.write(2, player.getLocation().getBlockX() * 32);
        spawnPacketModifier.write(3, -378 * 32);
        spawnPacketModifier.write(4, player.getLocation().getBlockZ() * 32);
        // Make the datawatcher that turns it invisible
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(6, health, true); // Set health
        watcher.setObject(10, message);
        spawnPacket.getDataWatcherModifier().write(0, watcher);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, spawnPacket, false);
    }

    public static void setName(Player player, String message, float health) {
        try {
            if (!player.hasMetadata("SeesEnderdragon")) {
                player.setMetadata("SeesEnderdragon", new FixedMetadataValue(plugin, true));
            }
            if (toHide.containsKey(player.getName())) {
                toHide.remove(player.getName()).cancel();
            }
            sendSpawnPacket(player, message, health);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
