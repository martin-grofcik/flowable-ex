package org.flowable.ex.shell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.ParameterMissingResolutionException;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class DeployIT {
    @Autowired
    Shell shell;

    @Test
    void deploy() {
        assertThat((String) shell.evaluate(() -> "deploy src/test/resources/app.bar")).
                contains("\"name\" : \"app\"");
    }

    @Test
    void deployWithAppName() {
        assertThat((String) shell.evaluate(() -> "deploy src/test/resources/app.bar --file-name testFileName.bar")).
                contains("\"name\" : \"testFileName\"");
    }

    @Test
    void deployWithTenant() {
        assertThat((String) shell.evaluate(() -> "deploy src/test/resources/app.bar --file-name app.bar --tenant-id testTenant")).
                contains("\"tenantId\" : \"testTenant\"");
    }

    @Test
    void deployWithoutFileName() {
        assertThat((ParameterMissingResolutionException) shell.evaluate(() -> "deploy")).
                extracting(Throwable::getMessage).isEqualTo("Parameter '--path-to-application string' should be specified");
    }

}
