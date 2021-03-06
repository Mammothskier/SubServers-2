package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketInExLogMessage;
import net.md_5.bungee.api.ProxyServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * External Process Logger Class
 */
public class ExternalSubLogger extends SubLogger {
    private Object handle;
    protected UUID id = null;
    protected String name;
    protected Container<Boolean> log;
    private List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
    private List<LogMessage> messages = new LinkedList<LogMessage>();
    protected File file;
    private PrintWriter writer = null;
    private boolean started = false;

    /**
     * Creates a new External Logger
     *
     * @param user Object using this logger (or null)
     * @param name Prefix
     * @param log Console Logging Status
     * @param file File to log to (or null for disabled)
     */
    protected ExternalSubLogger(Object user, String name, Container<Boolean> log, File file) {
        this.handle = user;
        this.name = name;
        this.log = log;
        this.file = file;
    }

    @Override
    public void start() {
        id = PacketInExLogMessage.register(this);
        started = true;
        if (file != null && writer == null) {
            try {
                this.writer = new PrintWriter(file, "UTF-8");
                this.writer.println("---------- LOG START \u2014 " + name + " ----------");
                this.writer.flush();
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
        filters.addAll(this.filters);
        for (SubLogFilter filter : filters) try {
            filter.start();
        } catch (Throwable e) {
            new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
        }
    }

    /**
     * Get the External Logger Address
     *
     * @return External Address
     */
    public UUID getExternalAddress() {
        return id;
    }

    /**
     * Log a Message
     *
     * @param line Message
     */
    public void log(String line) {
        if (started) {
            String msg = line;
            Level level;

            // REGEX Formatting
            String type = "";
            Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARNING|WARN|ERROR|ERR|SEVERE)\\]?:?(?:\\s*>)?\\s*)").matcher(msg);
            while (matcher.find()) {
                type = matcher.group(3).toUpperCase();
            }

            msg = msg.replaceAll("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARNING|WARN|ERROR|ERR|SEVERE)\\]?:?(?:\\s*>)?\\s*)", "");

            // Determine LOG LEVEL
            switch (type) {
                case "WARNING":
                case "WARN":
                    level = Level.WARNING;
                    break;
                case "SEVERE":
                case "ERROR":
                case "ERR":
                    level = Level.SEVERE;
                    break;
                default:
                    level = Level.INFO;
            }

            // Filter Message
            boolean allow = true;
            List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
            filters.addAll(this.filters);
            for (SubLogFilter filter : filters)
                try {
                    if (allow) allow = filter.log(level, msg);
                } catch (Throwable e) {
                    new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
                }

            // Log to CONSOLE
            if (log.get() && allow) ProxyServer.getInstance().getLogger().log(level, name + " > " + msg);

            // Log to MEMORY
            messages.add(new LogMessage(level, msg));

            // Log to FILE
            if (writer != null) {
                writer.println(line);
                writer.flush();
            }
        }
    }

    @Override
    public void registerFilter(SubLogFilter filter) {
        if (Util.isNull(filter)) throw new NullPointerException();
        filters.add(filter);
    }

    @Override
    public void unregisterFilter(SubLogFilter filter) {
        if (Util.isNull(filter)) throw new NullPointerException();
        filters.remove(filter);
    }

    @Override
    public void stop() {
        if (started) {
            PacketInExLogMessage.unregister(id);
            id = null;
            started = false;
            List<SubLogFilter> filters = new ArrayList<SubLogFilter>();
            filters.addAll(this.filters);
            for (SubLogFilter filter : filters) try {
                filter.stop();
            } catch (Throwable e) {
                new InvocationTargetException(e, "Exception while running SubLogger Event").printStackTrace();
            }
            messages.clear();
            if (writer != null) {
                PrintWriter writer = this.writer;
                this.writer = null;
                int l = (int) Math.floor((("---------- LOG START \u2014 " + name + " ----------").length() - 9) / 2);
                String s = "";
                while (s.length() < l) s += '-';
                writer.println(s + " LOG END " + s);
                writer.close();
            }
        }
    }

    @Override
    public Object getHandler() {
        return handle;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public List<LogMessage> getMessageHistory() {
        return new LinkedList<LogMessage>(messages);
    }
}
