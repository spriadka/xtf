package cz.xtf.core.openshift;

import org.apache.commons.lang3.ArrayUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenShiftBinary {
    private final String path;

    @Getter
    private String ocConfigPath;

    public OpenShiftBinary(String path) {
        this.path = path;
    }

    public OpenShiftBinary(String path, String ocConfigPath) {
        this(path);
        this.ocConfigPath = ocConfigPath;
    }

    public void login(String url, String token) {
        this.execute("login", url, "--insecure-skip-tls-verify=true", "--token=" + token);
    }

    public void login(String url, String username, String password) {
        this.execute("login", url, "--insecure-skip-tls-verify=true", "-u", username, "-p", password);
    }

    public void project(String projectName) {
        this.execute("project", projectName);
    }

    public void startBuild(String buildConfig, String sourcePath) {
        this.execute("start-build", buildConfig, "--from-dir=" + sourcePath);
    }

    // Common method for any oc command call
    public String execute(String... args) {
        if (ocConfigPath == null) {
            return CLIUtils.executeCommand(ArrayUtils.addAll(new String[] { path }, args));
        } else {
            return CLIUtils.executeCommand(ArrayUtils.addAll(new String[] { path, "--kubeconfig=" + ocConfigPath }, args));
        }
    }
}
