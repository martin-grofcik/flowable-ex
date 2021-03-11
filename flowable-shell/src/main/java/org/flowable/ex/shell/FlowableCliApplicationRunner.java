package org.flowable.ex.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Input;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.util.ArrayList;
import java.util.List;

@Order(-1)
public class FlowableCliApplicationRunner implements CommandLineRunner {
    private static final String COMMAND_SEPARATOR = ";";
    private static final String WORD_SEPARATOR = " ";

    @Autowired
    private Shell shell;
    @Autowired
    private ConfigurableEnvironment environment;

    @Override
    public void run(String... args) {
        List<Input> commands = createCommands(args);
        if (!commands.isEmpty()) {
            InteractiveShellApplicationRunner.disable(environment);
            commands.forEach(command -> shell.evaluate(command));
        }
    }

    private List<Input> createCommands(String[] args) {
        ArrayList<Input> inputs = new ArrayList<>();
        boolean insideCommand = false;
        StringBuilder command = new StringBuilder();
        for (String arg : args) {
            if (isReserved(arg)) {
                if (insideCommand) {
                    insideCommand = appendCommand(inputs, command, arg);
                }
            } else {
                insideCommand = appendCommand(inputs, command, arg);
            }
        }
        if (command.toString().length() > 0) {
            inputs.add(new FlowableArgsInputProvider(command.toString()));
        }
        return inputs;
    }

    private boolean appendCommand(ArrayList<Input> commandConstructor, StringBuilder command, String arg) {
        append(getWord(arg), command);
        boolean insideCommand = isNotCommandEnd(arg);
        appendToCommandList(commandConstructor, insideCommand, command);
        return insideCommand;
    }

    private boolean isNotCommandEnd(String arg) {
        return !arg.endsWith(COMMAND_SEPARATOR);
    }

    private void appendToCommandList(ArrayList<Input> commandConstructor, boolean insideCommand, StringBuilder command) {
        if (!insideCommand) {
            commandConstructor.add(new FlowableArgsInputProvider(command.toString()));
            command.delete(0, command.length());
        }
    }

    private void append(String word, StringBuilder command) {
        if (command.length() == 0) {
            command.append(word);
        } else {
            command.append(WORD_SEPARATOR).append(word);
        }
    }

    private String getWord(String arg) {
        return arg.endsWith(COMMAND_SEPARATOR) ? arg.substring(0, arg.length() - COMMAND_SEPARATOR.length()) : arg;
    }

    private boolean isReserved(String arg) {
        return arg.startsWith("-D") || arg.startsWith("-P");
    }

    private static class FlowableArgsInputProvider implements Input {
        private final String command;

        public FlowableArgsInputProvider(String command) {
            this.command = command;
        }

        @Override
        public String rawText() {
            return command;
        }

    }

}
