package org.flowable.ex.shell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.ParameterMissingResolutionException;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class DeployIT {
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

    @Test
    void importExportApp() {
        File outFile = new File("target/outputFile.zip");
        try {
            shell.evaluate(() -> "import --input-file-name src/test/resources/app.zip");

            assertThat(shell.evaluate(() -> "list app").toString()).contains("\"name\":\"app\"");

            shell.evaluate(() -> "export --name app --output-file-name target/outputFile.zip");

            assertThat(outFile).exists();
        } finally {
            if (outFile.exists()) {
                if (!outFile.delete()) {
                    System.err.println("Unable to delete file");
                }
            }
            shell.evaluate(() -> "rm app");
            assertThat(shell.evaluate(() -> "ls app").toString()).
                    contains("\"size\":0");

        }
    }

    private void deleteDeployment(String deploymentName) {
        shell.evaluate(() -> "delete-deployments " + deploymentName);
        assertThat(shell.evaluate(() -> "lsd "+ deploymentName).toString()).
                contains("\"size\":0");
    }
}
