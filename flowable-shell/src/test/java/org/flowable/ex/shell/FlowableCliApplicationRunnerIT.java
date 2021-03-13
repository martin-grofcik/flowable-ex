package org.flowable.ex.shell;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
class FlowableCliApplicationRunnerIT {

    @Autowired
    private CommandLineRunner commandLineRunner;

    @Autowired
    private Shell shell;

    @Test
    void deployByCli() throws Exception {
        commandLineRunner.run("deploy","src/test/resources/app.bar");

        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":1");
    }

    @Test
    void passVariablesAndProfiles() throws Exception {
        commandLineRunner.run("-PCLI", "-Dvariable=value", "deploy","src/test/resources/app.bar");

        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":1");
    }

    @Test
    void passVariablesAndProfilesWithoutCommand() throws Exception {
        commandLineRunner.run("-PCLI", "-Dvariable=value");

        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":0");
    }

    @Test
    void multiCommand() throws Exception {
        commandLineRunner.run("deploy","src/test/resources/app.bar;",
                "delete-deployments", "app;",
                "deploy","src/test/resources/app.bar;");

        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":1");
    }

    @AfterEach
    void deleteDeployment() {
        shell.evaluate(() -> "delete-deployments app");
        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":0");
    }
}