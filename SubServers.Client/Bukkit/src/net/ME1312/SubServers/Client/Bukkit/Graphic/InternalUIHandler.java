package net.ME1312.SubServers.Client.Bukkit.Graphic;

import net.ME1312.SubServers.Client.Bukkit.Library.Container;
import net.ME1312.SubServers.Client.Bukkit.Library.JSONCallback;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Internal GUI Listener
 */
public class InternalUIHandler implements UIHandler, Listener {
    private HashMap<UUID, JSONCallback> input = new HashMap<UUID, JSONCallback>();
    private HashMap<UUID, InternalUIRenderer> gui = new HashMap<UUID, InternalUIRenderer>();
    private boolean enabled = true;
    private SubPlugin plugin;

    /**
     * Creates a new Internal GUI Listener
     *
     * @param plugin Event
     */
    public InternalUIHandler(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public InternalUIRenderer getRenderer(Player player) {
        if (!gui.keySet().contains(player.getUniqueId())) gui.put(player.getUniqueId(), new InternalUIRenderer(plugin, player.getUniqueId()));
        return gui.get(player.getUniqueId());
    }

    public void disable() {
        enabled = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.isCancelled() && enabled && gui.keySet().contains(player.getUniqueId())) {
            InternalUIRenderer gui = this.gui.get(player.getUniqueId());
            if (gui.open && event.getClickedInventory() != null && event.getClickedInventory().getTitle() != null) {
                if (plugin.subdata == null) {
                    new IllegalStateException("SubData is not connected").printStackTrace();
                } else if (plugin.lang == null) {
                    new IllegalStateException("There are no lang options available at this time").printStackTrace();
                } else if (event.getClickedInventory().getTitle().equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.Title", '&'))) { // Host Menu
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage - 1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(gui.lastPage + 1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.SubServer-Menu", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(1, null);
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Menu.No-Hosts", '&'))) {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            gui.hostAdmin(obj);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$")[0]) && // Host Creator
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Undo", '&'))) {
                            player.closeInventory();
                            gui.lastUsedOptions.undo();
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Submit", '&'))) {
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + gui.lastVistedObject.toLowerCase())) {
                                player.closeInventory();
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                plugin.subdata.sendPacket(new PacketCreateServer(player.getUniqueId(), (UIRenderer.CreatorOptions) gui.lastUsedOptions, json -> {
                                    gui.back();
                                }));
                            } else {
                                gui.back();
                            }
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (m.getString("message").contains(" ")) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                    plugin.subdata.sendPacket(new PacketDownloadServerList(null, json -> {
                                        boolean match = false;
                                        for (String tmphost : json.getJSONObject("hosts").keySet()) {
                                            for (String tmpsubserver : json.getJSONObject("hosts").getJSONObject(tmphost).getJSONObject("servers").keySet()) {
                                                if (tmpsubserver.equalsIgnoreCase(m.getString("message"))) match = true;
                                            }
                                        }
                                        if (match) {
                                            gui.setDownloading(null);
                                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Exists-Title", '&'), 4 * 20))
                                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Name.Exists", '&'));
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                        } else {
                                            ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setName(m.getString("message"));
                                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                        }
                                    }));
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Template", '&')))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(1, (UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (new Version("1.8").compareTo(new Version(m.getString("message"))) > 0) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Unavailable-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Version.Unavailable", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setVersion(new Version(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        } else if (ChatColor.stripColor(item).equals(ChatColor.stripColor(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port", '&')))) {
                            player.closeInventory();
                            if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Title", '&'), 4 * 20))
                                player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Message", '&'));
                            input.put(player.getUniqueId(), m -> {
                                if (Util.isException(() -> Integer.parseInt(m.getString("message"))) || Integer.parseInt(m.getString("message")) <= 0 || Integer.parseInt(m.getString("message")) > 65535) {
                                    if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Invalid-Title", '&'), 4 * 20))
                                        player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Port.Invalid", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions), 4 * 20);
                                } else {
                                    ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setPort(Integer.valueOf(m.getString("message")));
                                    gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                                }
                            });
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Template.Title", '&').split("\\$str\\$")[0]) && // Host Creator Templates
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Template.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Creator.Edit-Template.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(gui.lastPage - 1, (UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostCreatorTemplates(gui.lastPage + 1, (UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        } else {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            ((UIRenderer.CreatorOptions) gui.lastUsedOptions).setTemplate(obj);
                            gui.hostCreator((UIRenderer.CreatorOptions) gui.lastUsedOptions);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').split("\\$str\\$")[0]) && // Host Plugin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage - 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.hostPlugin(gui.lastPage + 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else {
                            player.closeInventory();
                            final Container<Renderer> plugin = new Container<Renderer>(null);
                            for (Renderer renderer : InternalUIRenderer.hostPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.set(renderer);
                            }
                            if (plugin.get() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.lang.getSection("Lang").getColoredString("Interface.Host-Plugin.Title", '&').replace("$str$", gui.lastVistedObject)));
                                this.plugin.subdata.sendPacket(new PacketDownloadHostInfo(gui.lastVistedObject, (json) -> {
                                    if (json.getBoolean("valid")) {
                                        gui.setDownloading(null);
                                        plugin.get().open(player, json.getJSONObject("host"));
                                    } else {
                                        gui.back();
                                    }
                                }));
                            }
                        }
                    }
                } else if (event.getClickedInventory().getTitle().equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Title", '&')) || // SubServer Menu
                        event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$")[0]) &&
                                (plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$").length == 1 ||
                                        event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-SubServer.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(gui.lastPage - 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(gui.lastPage + 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.Host-Menu", '&'))) {
                            player.closeInventory();
                            gui.hostMenu(1);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (!item.equals(ChatColor.RESET.toString()) && !item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Menu.No-SubServers", '&'))) {
                            player.closeInventory();
                            String obj;
                            if (event.getCurrentItem().getItemMeta().getLore() != null && event.getCurrentItem().getItemMeta().getLore().size() > 0 && event.getCurrentItem().getItemMeta().getLore().get(0).startsWith(ChatColor.GRAY.toString())) {
                                obj = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getLore().get(0));
                            } else {
                                obj = ChatColor.stripColor(item);
                            }
                            gui.subserverAdmin(obj);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$")[0]) && // Host Admin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Creator", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.host.create.*") || player.hasPermission("subservers.host.create." + gui.lastVistedObject.toLowerCase())) {
                                gui.hostCreator(new UIRenderer.CreatorOptions(gui.lastVistedObject));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.SubServers", '&'))) {
                            player.closeInventory();
                            gui.subserverMenu(1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Host-Admin.Plugins", '&'))) {
                            player.closeInventory();
                            gui.hostPlugin(1, gui.lastVistedObject);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$")[0]) && // SubServer Admin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();

                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.start.*") || player.hasPermission("subservers.subserver.start." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                plugin.subdata.sendPacket(new PacketStartServer(player.getUniqueId(), gui.lastVistedObject, json -> {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Start.Title", '&'));
                                    Bukkit.getScheduler().runTaskLater(plugin, gui::reopen, 30);
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.stop.*") || player.hasPermission("subservers.subserver.stop." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                PacketInRunEvent.callback("SubStoppedEvent", new JSONCallback() {
                                    @Override
                                    public void run(JSONObject json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase(gui.lastVistedObject)) {
                                                PacketInRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), gui.lastVistedObject, false, json -> {
                                    if (json.getInt("r") != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Stop.Title", '&').replace("$str$", gui.lastVistedObject));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.terminate.*") || player.hasPermission("subservers.subserver.terminate." + gui.lastVistedObject.toLowerCase())) {
                                gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                final Container<Boolean> listening = new Container<Boolean>(true);
                                PacketInRunEvent.callback("SubStoppedEvent", new JSONCallback() {
                                    @Override
                                    public void run(JSONObject json) {
                                        try {
                                            if (listening.get()) if (!json.getString("server").equalsIgnoreCase(gui.lastVistedObject)) {
                                                PacketInRunEvent.callback("SubStoppedEvent", this);
                                            } else {
                                                gui.reopen();
                                            }
                                        } catch (Exception e) {}
                                    }
                                });
                                plugin.subdata.sendPacket(new PacketStopServer(player.getUniqueId(), gui.lastVistedObject, false, json -> {
                                    if (json.getInt("r") != 0) {
                                        gui.reopen();
                                        listening.set(false);
                                    } else gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Terminate.Title", '&').replace("$str$", gui.lastVistedObject));
                                }));
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command", '&'))) {
                            player.closeInventory();
                            if (player.hasPermission("subservers.subserver.command.*") || player.hasPermission("subservers.subserver.command." + gui.lastVistedObject.toLowerCase())) {
                                if (!gui.sendTitle(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command.Title", '&'), 4 * 20))
                                    player.sendMessage(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Command.Message", '&'));
                                input.put(player.getUniqueId(), m -> {
                                    gui.setDownloading(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Downloading.Response", '&'));
                                    plugin.subdata.sendPacket(new PacketCommandServer(player.getUniqueId(), gui.lastVistedObject, (m.getString("message").startsWith("/"))?m.getString("message").substring(1):m.getString("message"), json -> {
                                        gui.reopen();
                                    }));
                                });
                            } else gui.reopen();
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Admin.Plugins", '&'))) {
                            player.closeInventory();
                            gui.subserverPlugin(1, gui.lastVistedObject);
                        }
                    }
                } else if (event.getClickedInventory().getTitle().startsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').split("\\$str\\$")[0]) && // SubServer Plugin
                        (plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').split("\\$str\\$").length == 1 ||
                                event.getClickedInventory().getTitle().endsWith(plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').split("\\$str\\$")[1]))) {
                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().hasItemMeta()) {
                        String item = event.getCurrentItem().getItemMeta().getDisplayName();
                        if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverPlugin(gui.lastPage - 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Next-Arrow", '&'))) {
                            player.closeInventory();
                            gui.subserverPlugin(gui.lastPage + 1, gui.lastVistedObject);
                        } else if (item.equals(plugin.lang.getSection("Lang").getColoredString("Interface.Generic.Back", '&'))) {
                            player.closeInventory();
                            gui.back();
                        } else {
                            player.closeInventory();
                            Container<Renderer> plugin = new Container<Renderer>(null);
                            for (Renderer renderer : InternalUIRenderer.subserverPlugins.values()) {
                                if (item.equals(renderer.getIcon().getItemMeta().getDisplayName())) plugin.set(renderer);
                            }
                            if (plugin.get() == null) {
                                gui.reopen();
                            } else {
                                gui.setDownloading(ChatColor.stripColor(this.plugin.lang.getSection("Lang").getColoredString("Interface.SubServer-Plugin.Title", '&').replace("$str$", gui.lastVistedObject)));
                                this.plugin.subdata.sendPacket(new PacketDownloadServerInfo(gui.lastVistedObject, json -> {
                                    if (json.getString("type").equals("subserver")) {
                                        gui.setDownloading(null);
                                        plugin.get().open(player, json.getJSONObject("server"));
                                    } else {
                                        gui.back();
                                    }
                                }));
                            }
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Input Listener
     *
     * @param event Event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void input(org.bukkit.event.player.PlayerChatEvent event) {
        if (!event.isCancelled() && enabled && input.keySet().contains(event.getPlayer().getUniqueId())) {
            JSONObject json = new JSONObject();
            json.put("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(json);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    /**
     * Input Listener
     *
     * @param event Event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void input(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled() && enabled && input.keySet().contains(event.getPlayer().getUniqueId())) {
            JSONObject json = new JSONObject();
            json.put("message", event.getMessage());
            input.get(event.getPlayer().getUniqueId()).run(json);
            input.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    /**
     * GUI Close Listener
     *
     * @param event Event
     */
    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) gui.get(event.getPlayer().getUniqueId()).open = false;
    }

    /**
     * Clean Renderers
     *
     * @param event Event
     */
    @EventHandler
    public void clean(PlayerQuitEvent event) {
        if (gui.keySet().contains(event.getPlayer().getUniqueId())) {
            gui.get(event.getPlayer().getUniqueId()).setDownloading(null);
            gui.remove(event.getPlayer().getUniqueId());
            input.remove(event.getPlayer().getUniqueId());
        }
    }
}
