package org.flowable.ex.shell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.ParameterMissingResolutionException;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class DeploymentIT {
    @Autowired
    Shell shell;

    @Test
    void deploy() {
        assertThat(shell.evaluate(() -> "deploy src/test/resources/app.bar").toString()).
                contains("\"name\":\"app\"");
        assertThat(shell.evaluate(() -> "lsd app").toString()).
                contains("\"size\":1");

        deleteDeployment("app");
    }

    @Test
    void deployWithAppName() {
        assertThat(shell.evaluate(() -> "deploy src/test/resources/app.bar --deployment-name testFileName.bar").toString()).
                contains("\"name\":\"testFileName\"");

        deleteDeployment("testFileName");
    }

    @Test
    void deployWithTenant() {
        assertThat(shell.evaluate(() -> "deploy src/test/resources/app.bar --deployment-name app.bar --tenant-id testTenant").toString()).
                contains("\"tenantId\":\"testTenant\"");

        deleteDeployment("app");
    }

    @Test
    void deployWithoutFileName() {
        assertThat((ParameterMissingResolutionException) shell.evaluate(() -> "deploy")).
                extracting(Throwable::getMessage).isEqualTo("Parameter '--path-to-application string' should be specified");
    }

    private void deleteDeployment(String deploymentName) {
        shell.evaluate(() -> "delete-deployments " + deploymentName);
        assertThat(shell.evaluate(() -> "lsd "+ deploymentName).toString()).
                contains("\"size\":0");
    }
}
