package cz.xtf.core.openshift;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import cz.xtf.core.config.OpenShiftConfig;
import cz.xtf.core.config.XTFConfig;
import cz.xtf.core.http.Https;

public class HelmClients {

    private static final String HELM_CLIENTS_URL = "https://mirror.openshift.com/pub/openshift-v4/clients/helm";
    private static final String HELM_FILE_NAME = "helm.tar.gz";

    private static HelmBinary adminHelmBinary;
    private static HelmBinary masterHelmBinary;

    public static HelmBinary admin() {
        if (adminHelmBinary == null) {
            adminHelmBinary = getBinary(OpenShiftConfig.adminToken(),
                    OpenShiftConfig.adminUsername(),
                    OpenShiftConfig.adminPassword(),
                    OpenShiftConfig.adminKubeconfig(),
                    OpenShiftConfig.namespace());
        }
        return adminHelmBinary;
    }

    public static HelmBinary master() {
        if (masterHelmBinary == null) {
            masterHelmBinary = getBinary(OpenShiftConfig.masterToken(),
                    OpenShiftConfig.masterUsername(),
                    OpenShiftConfig.masterPassword(),
                    OpenShiftConfig.masterKubeconfig(),
                    OpenShiftConfig.namespace());
        }
        return masterHelmBinary;
    }

    private static HelmBinary getBinary(String token, String username, String password, String kubeconfig,
            String namespace) {
        OpenShiftBinary openShiftBinary = OpenShifts.getBinary(token, username, password, kubeconfig, namespace);
        return new HelmBinary(downloadHelmBinary(), openShiftBinary.getOcConfigPath());

    }

    private static String downloadHelmBinary() {
        String systemType = "";
        if (SystemUtils.IS_OS_MAC) {
            systemType = "darwin";
        } else {
            systemType = "linux";
        }
        String helmClientVersion = XTFConfig.get("helm.client.version", "latest");
        String helmClientUrl = String.format("%s/%s/helm-%s-amd64.tar.gz", HELM_CLIENTS_URL, helmClientVersion, systemType);
        return downloadHelmBinary(helmClientUrl, helmClientVersion, systemType, true);
    }

    private static String downloadHelmBinary(final String helmClientUrl, String helmClientVersion, String systemType,
            final boolean trustAll) {
        int code = Https.httpsGetCode(helmClientUrl);

        if (code != 200) {
            throw new IllegalStateException(
                    "Helm client binary of version: " + helmClientVersion + " isn't available at " + helmClientUrl);
        }

        File workdir = helmBinaryFolder();

        // Download and extract client
        File helmTarFile = new File(workdir, HELM_FILE_NAME);
        File helmFile = new File(workdir, "helm");

        try {
            URL requestUrl = new URL(helmClientUrl);

            if (trustAll) {
                Https.copyHttpsURLToFile(requestUrl, helmTarFile, 20_000, 300_000);
            } else {
                FileUtils.copyURLToFile(requestUrl, helmTarFile, 20_000, 300_000);
            }

            CLIUtils.executeCommand("tar", "-xf", helmTarFile.getPath(), "-C", workdir.getPath());
            FileUtils.deleteQuietly(helmTarFile);
            Paths.get(workdir.getPath()).resolve(String.format("helm-%s-amd64", systemType)).toFile()
                    .renameTo(helmFile);
            if (!helmFile.canExecute()) {
                helmFile.setExecutable(true);
            }
            return helmFile.getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download and extract helm binary from " + helmClientUrl, e);
        }
    }

    private static File helmBinaryFolder() {
        File workdir = new File(Paths.get("tmp/helm").toAbsolutePath().toString());
        if (workdir.exists()) {
            return workdir;
        }
        if (!workdir.mkdirs()) {
            throw new IllegalStateException("Cannot mkdirs " + workdir);
        }
        return workdir;
    }
}
