package me.vennlmao.ariscore.order.listeners;

import me.vennlmao.ariscore.order.utils.ServerScheduler;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SignInputListener implements Listener {
   private final JavaPlugin plugin;
   private final String marker;
   private final Map<UUID, SignInputListener.Session> sessions = new ConcurrentHashMap<>();
   private final boolean protocolAvailable;
   private PacketAdapter signPacketListener;

   public SignInputListener(JavaPlugin plugin, String marker) {
      this.plugin = plugin;
      this.marker = marker;
      this.protocolAvailable = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
      this.registerProtocolListener();
   }

   public void open(Player player, Consumer<String> onSubmit, Runnable onCancel) {
      if (player != null) {
         ServerScheduler.runPlayerTask(
            this.plugin,
            player,
            () -> {
               SignInputListener.Session existing = this.sessions.remove(player.getUniqueId());
               if (existing != null) {
                  this.cleanupDirect(player, existing);
               }

               Location base = player.getLocation().clone();
               Location signLoc = new Location(base.getWorld(), base.getBlockX(), base.getBlockY() - 2, base.getBlockZ());
               Location supportLoc = new Location(base.getWorld(), base.getBlockX(), base.getBlockY() - 3, base.getBlockZ());
               Block signBlock = signLoc.getBlock();
               Block supportBlock = supportLoc.getBlock();
               BlockState prevSign = signBlock.getState();
               BlockData prevSignData = signBlock.getBlockData().clone();
               BlockState prevSupport = supportBlock.getState();
               BlockData prevSupportData = supportBlock.getBlockData().clone();
               String arrows = this.marker;
               SignInputListener.Session session = new SignInputListener.Session(
                  signLoc, prevSign, prevSignData, supportLoc, prevSupport, prevSupportData, onSubmit, onCancel
               );
               this.sessions.put(player.getUniqueId(), session);
               if (this.protocolAvailable) {
                  player.sendBlockChange(signLoc, Bukkit.createBlockData(Material.OAK_SIGN));
                  List<Component> lines = List.of(
                     Component.empty(),
                     Component.text(arrows, NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false),
                     Component.empty(),
                     Component.empty()
                  );
                  player.sendSignChange(signLoc, lines, DyeColor.BLACK, false);
                  this.openSignEditorPacket(player, signLoc);
               } else {
                  ServerScheduler.runRegionTask(this.plugin, signLoc, () -> {
                     if (!player.isOnline()) {
                        this.sessions.remove(player.getUniqueId());
                     } else {
                        Block regionSignBlock = signLoc.getBlock();
                        Block regionSupportBlock = supportLoc.getBlock();
                        if (!regionSupportBlock.getType().isSolid()) {
                           regionSupportBlock.setType(Material.STONE, false);
                        }

                        regionSignBlock.setType(Material.OAK_SIGN, false);
                        if (regionSignBlock.getState() instanceof Sign sign) {
                           sign.getSide(Side.FRONT).line(0, this.empty());
                           sign.getSide(Side.FRONT).line(1, Component.text(arrows, NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false));
                           sign.getSide(Side.FRONT).line(2, this.empty());
                           sign.getSide(Side.FRONT).line(3, this.empty());
                           sign.update(true, false);
                           if (player.isOnline()) {
                              player.openSign(sign, Side.FRONT);
                           }
                        } else {
                           prevSign.update(true, false);
                           this.sessions.remove(player.getUniqueId());
                        }
                     }
                  });
               }
            }
         );
      }
   }

   public void cancel(Player player) {
      if (player != null) {
         SignInputListener.Session session = this.sessions.remove(player.getUniqueId());
         if (session != null) {
            this.cleanup(player, session);
            if (session.onCancel != null) {
               session.onCancel.run();
            }
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      SignInputListener.Session session = this.sessions.remove(player.getUniqueId());
      if (session != null) {
         this.cleanup(player, session);
      }
   }

   @EventHandler
   public void onSignChange(SignChangeEvent event) {
      Player player = event.getPlayer();
      SignInputListener.Session session = this.sessions.remove(player.getUniqueId());
      if (session != null) {
         if (this.sameBlock(event.getBlock().getLocation(), session.location)) {
            String input = this.firstNonBlankLine(event);
            this.cleanup(player, session);
            if (session.onSubmit != null) {
               session.onSubmit.accept(input);
            }
         }
      }
   }

   private void registerProtocolListener() {
      if (this.protocolAvailable) {
         this.signPacketListener = new PacketAdapter(this.plugin, ListenerPriority.NORMAL, Client.UPDATE_SIGN) {
            public void onPacketReceiving(PacketEvent event) {
               Player player = event.getPlayer();
               SignInputListener.Session session = SignInputListener.this.sessions.get(player.getUniqueId());
               if (session != null) {
                  BlockPosition pos = (BlockPosition)event.getPacket().getBlockPositionModifier().readSafely(0);
                  if (pos != null) {
                     Location packetLoc = new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
                     if (SignInputListener.this.sameBlock(packetLoc, session.location)) {
                        String input = "";
                        String[] lines = (String[])event.getPacket().getStringArrays().readSafely(0);
                        if (lines != null) {
                           String arrows = SignInputListener.this.marker;

                           for (String line : lines) {
                              if (line != null) {
                                 String trimmed = line.trim();
                                 if (!trimmed.isEmpty() && !trimmed.equals(arrows)) {
                                    input = trimmed;
                                    break;
                                 }
                              }
                           }
                        }

                        SignInputListener.Session consumed = SignInputListener.this.sessions.remove(player.getUniqueId());
                        if (consumed != null) {
                           String finalInput = input;
                           ServerScheduler.runPlayerTask(this.plugin, player, () -> {
                              SignInputListener.this.cleanup(player, consumed);
                              if (consumed.onSubmit != null) {
                                 consumed.onSubmit.accept(finalInput);
                              }
                           });
                        }
                     }
                  }
               }
            }
         };
         ProtocolLibrary.getProtocolManager().addPacketListener(this.signPacketListener);
      }
   }

   private void openSignEditorPacket(Player player, Location signLoc) {
      try {
         ProtocolManager manager = ProtocolLibrary.getProtocolManager();
         PacketContainer packet = manager.createPacket(Server.OPEN_SIGN_EDITOR);
         packet.getBlockPositionModifier().writeSafely(0, new BlockPosition(signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ()));
         packet.getBooleans().writeSafely(0, true);
         manager.sendServerPacket(player, packet);
      } catch (Exception var5) {
      }
   }

   private Component empty() {
      return Component.text("", NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false);
   }

   private String firstNonBlankLine(SignChangeEvent event) {
      String arrows = this.marker;

      for (int i = 0; i < 4; i++) {
         Component line = event.line(i);
         String s = line == null ? "" : PlainTextComponentSerializer.plainText().serialize(line);
         if (s != null) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty() && !trimmed.equals(arrows)) {
               return trimmed;
            }
         }
      }

      return "";
   }

   private void cleanup(Player player, SignInputListener.Session session) {
      ServerScheduler.runPlayerTask(this.plugin, player, () -> this.cleanupDirect(player, session));
   }

   private void cleanupDirect(Player player, SignInputListener.Session session) {
      if (this.protocolAvailable) {
         player.sendBlockChange(session.location, session.previousBlockData);
         player.sendBlockChange(session.supportLocation, session.previousSupportBlockData);
      } else {
         Location signLoc = session.location;
         ServerScheduler.runRegionTask(this.plugin, signLoc, () -> {
            Block signBlock = session.location.getBlock();
            signBlock.setType(Material.AIR, false);
            session.previous.update(true, false);
            session.previousSupport.update(true, false);
            if (player.isOnline()) {
               ServerScheduler.runPlayerTask(this.plugin, player, () -> {
                  player.sendBlockChange(session.location, session.previousBlockData);
                  player.sendBlockChange(session.supportLocation, session.previousSupportBlockData);
               });
            }
         });
      }
   }

   private boolean sameBlock(Location a, Location b) {
      if (a == null || b == null) {
         return false;
      } else {
         return a.getWorld() != null && b.getWorld() != null
            ? a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ()
            : false;
      }
   }

   private static final class Session {
      private final Location location;
      private final BlockState previous;
      private final BlockData previousBlockData;
      private final Location supportLocation;
      private final BlockState previousSupport;
      private final BlockData previousSupportBlockData;
      private final Consumer<String> onSubmit;
      private final Runnable onCancel;

      private Session(
         Location location,
         BlockState previous,
         BlockData previousBlockData,
         Location supportLocation,
         BlockState previousSupport,
         BlockData previousSupportBlockData,
         Consumer<String> onSubmit,
         Runnable onCancel
      ) {
         this.location = location;
         this.previous = previous;
         this.previousBlockData = previousBlockData;
         this.supportLocation = supportLocation;
         this.previousSupport = previousSupport;
         this.previousSupportBlockData = previousSupportBlockData;
         this.onSubmit = onSubmit;
         this.onCancel = onCancel;
      }
   }
}
