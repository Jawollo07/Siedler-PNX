package de.jawollo07.siedler.core;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.command.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Handles registration of all Siedler commands.
 *
 * <p>This class provides a central place for command registration
 * and prevents the main plugin class from having to interact with
 * the PowerNukkitX CommandMap directly.</p>
 */
public final class CommandManager {

    private static final String COMMAND_FALLBACK_PREFIX = "siedler";

    private final SiedlerPlugin plugin;
    private final List<Command> registeredCommands = new ArrayList<>();

    /**
     * Creates a new command manager.
     *
     * @param plugin Siedler plugin instance
     */
    public CommandManager(SiedlerPlugin plugin) {
        this.plugin = Objects.requireNonNull(
                plugin,
                "plugin cannot be null"
        );
    }

    /**
     * Registers a single command.
     *
     * @param command command to register
     */
    public void register(Command command) {
        Objects.requireNonNull(
                command,
                "command cannot be null"
        );

        plugin.getServer()
                .getCommandMap()
                .register(COMMAND_FALLBACK_PREFIX, command);

        registeredCommands.add(command);
    }

    /**
     * Registers multiple commands.
     *
     * @param commands commands to register
     */
    public void registerAll(Collection<? extends Command> commands) {
        Objects.requireNonNull(
                commands,
                "commands cannot be null"
        );

        for (Command command : commands) {
            register(command);
        }
    }

    /**
     * Returns all commands registered through this manager.
     *
     * @return immutable list of registered commands
     */
    public List<Command> getCommands() {
        return List.copyOf(registeredCommands);
    }

    /**
     * Returns the number of registered commands.
     *
     * @return number of registered commands
     */
    public int size() {
        return registeredCommands.size();
    }

    /**
     * Checks whether a command was registered through this manager.
     *
     * @param command command to check
     * @return true if the command was registered
     */
    public boolean isRegistered(Command command) {
        return registeredCommands.contains(command);
    }
}
