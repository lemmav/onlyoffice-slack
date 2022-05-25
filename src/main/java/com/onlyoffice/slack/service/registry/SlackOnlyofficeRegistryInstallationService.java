package com.onlyoffice.slack.service.registry;

import com.onlyoffice.slack.configuration.cache.bot.HazelcastBotUsersCacheClient;
import com.onlyoffice.slack.configuration.cache.user.HazelcastUsersCacheClient;
import com.onlyoffice.slack.configuration.cache.workspaces.HazelcastWorkspacesCacheClient;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.onlyoffice.slack.model.registry.GenericResponse;
import com.onlyoffice.slack.model.registry.License;
import com.onlyoffice.slack.model.registry.User;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.util.EncryptorAesGcm;
import com.onlyoffice.slack.util.SlackInstallationMapper;
import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import com.slack.api.bolt.service.InstallationService;
import com.slack.api.model.block.LayoutBlock;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackOnlyofficeRegistryInstallationService implements InstallationService {
    private final OnlyofficeWorkspaceRegistryService workspaceRegistryService;
    private final OnlyofficeUserRegistryService userRegistryService;
    private final OnlyofficeLicenseRegistryService licenseRegistryService;

    private final HazelcastWorkspacesCacheClient workspacesCacheClient;
    private final HazelcastUsersCacheClient usersCacheClient;
    private final HazelcastBotUsersCacheClient botUsersCacheClient;

    private final IntegrationConfiguration integrationConfiguration;
    private final EncryptorAesGcm encryptor;
    private final SlackInstallationMapper slackInstallationMapper;

    @Setter
    private boolean historyEnabled = true;
    private final Predicate<String> isValidString = (s) -> s != null && !s.isBlank();

    @RateLimiter(name = "queryRateLimiter", fallbackMethod = "findWorkspaceRateFallback")
    public Workspace findWorkspace(String wid) {
        Workspace cached = workspacesCacheClient.getWorkspace(wid);

        if (cached != null) {
            log.debug("Found {} instance in the cache", wid);
            return cached;
        }

        Workspace workspace = workspaceRegistryService.getWorkspace(wid);
        if (workspace == null) return null;

        if (isValidString.test(workspace.getServerSecret())) {
            try {
                workspace.setServerSecret(encryptor.decrypt(workspace.getServerSecret()));
            } catch (Exception e) {
                log.error("An error has occurred while decrypting workspace secret: {}", e.getMessage());
                return null;
            }
        }

        workspacesCacheClient.addWorkspace(wid, workspace);

        log.debug("Found workspace with id {}", wid);

        return workspace;
    }

    public Workspace findWorkspaceRateFallback(
            String wid,
            RequestNotPermitted e
    ) {
        log.warn("find workspace {} - {}", wid, e.getMessage());
        return null;
    }

    @RateLimiter(name = "commandUpdateRateLimiter", fallbackMethod = "saveLicenseRateFallback")
    public boolean saveLicense(String wid, License license) {
        try {
            log.debug("Saving license for {}", wid);

            String plainSecret = license.getServerSecret();
            license.setServerSecret(encryptor.encrypt(plainSecret));

            GenericResponse response = licenseRegistryService.saveLicense(wid, license);

            if (response.getSuccess()) {
                workspacesCacheClient.updateWorkspace(wid, Workspace
                        .builder()
                        .id(wid)
                        .serverHeader(license.getServerHeader())
                        .serverUrl(license.getServerUrl())
                        .serverSecret(plainSecret)
                        .build()
                );
                return true;
            }

            log.debug("Successfully save license for {}", wid);

            return false;
        } catch (Exception e) {
            log.error("An error has occurred while saving license: {}", e.getMessage());
            return false;
        }
    }

    public boolean saveLicenseRateFallback(
            String wid, License license,
            RequestNotPermitted e
    ) {
        log.warn("save license {} has been terminated by RateLimiter: {}", wid, e.getMessage());
        return false;
    }

    @RateLimiter(name = "commandInstallRateLimiter", fallbackMethod = "saveInstallerAndBotRateFallback")
    public void saveInstallerAndBot(Installer installation) throws UnableToPerformSlackOperationException {
        try {
            if (!(installation instanceof DefaultInstaller))
                throw new UnableToPerformSlackOperationException("Expected to get installation of type DefaultInstaller. Got unknown type");
            DefaultInstaller installer = (DefaultInstaller) installation;

            if (!isValidString.test(installer.getTeamId()))
                throw new UnableToPerformSlackOperationException("Could not save a new installation. Invalid arguments (team id is empty)");

            if (!isValidString.test(installer.getInstallerUserId()))
                throw new UnableToPerformSlackOperationException("Could not save a new installation. Invalid arguments (user id is empty)");

            log.debug("New installation request with workspace id = {} and user id = {}",
                    installation.getTeamId(), installation.getInstallerUserId());

            Workspace workspace = Workspace
                    .builder()
                    .id(installer.getTeamId())
                    .build();

            String userToken = encryptor.encrypt(slackInstallationMapper.toBase64(installer));
            User user = User
                    .builder()
                    .id(installer.getInstallerUserId())
                    .username(installer.getInstallerUserId())
                    .token(userToken)
                    .build();

            String botToken = encryptor.encrypt(slackInstallationMapper.toBase64(slackInstallationMapper.toBot(installer)));
            User bot = User
                    .builder()
                    .id(installer.getTeamId())
                    .username(installer.getTeamId())
                    .token(botToken)
                    .build();

            workspaceRegistryService.saveWorkspaceAndUser(workspace, user, bot);

            log.debug("Successfully executed installation request with workspace id = {} and user id = {}",
                    workspace.getId(), user.getId());
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException(e.getMessage());
        }
    }

    public void saveInstallerAndBotRateFallback(
            Installer installation,
            RequestNotPermitted e
    ) throws UnableToPerformSlackOperationException {
        throw new UnableToPerformSlackOperationException(e.getMessage());
    }

    @RateLimiter(name = "queryRateLimiter", fallbackMethod = "findInstallerRateFallback")
    public Installer findInstaller(String s, String wid, String uid) {
        if (!isValidString.test(wid) || !isValidString.test(uid)) return null;
        log.debug("New request to find installation with workspace id = {} and user id = {}",
                wid, uid);

        try {
            User cached = usersCacheClient.getUser(wid, uid);
            if (cached != null)
                return slackInstallationMapper.toInstaller(cached.getToken());

            User user = userRegistryService.getUser(wid, uid);
            if (user == null || !isValidString.test(user.getToken())) return null;

            user.setToken(encryptor.decrypt(user.getToken()));
            Installer installer = slackInstallationMapper.toInstaller(user.getToken());

            usersCacheClient.addUser(wid, uid, user);

            log.debug("Successfully found installation with workspace id = {} and user id = {}",
                    wid, uid);

            return installer;
        } catch (Exception e) {
            return null;
        }
    }

    public Installer findInstallerRateFallback(
            String s, String wid, String uid,
            RequestNotPermitted e
    ) {
        log.warn("find installer ({}):{} - {}", wid, uid, e.getMessage());
        return null;
    }

    public void deleteInstaller(Installer installer) throws UnableToPerformSlackOperationException {
        if (!isValidString.test(installer.getTeamId()) || !isValidString.test(installer.getInstallerUserId()))
            throw new UnableToPerformSlackOperationException("Could not delete an installer instance. Invalid wid/uid arguments");

        log.debug("New request to delete installation with workspace id = {} and user id = {}",
                installer.getTeamId(), installer.getInstallerUserId());

        userRegistryService.deleteUser(installer.getTeamId(), installer.getInstallerUserId());
        usersCacheClient.deleteUser(installer.getTeamId(), installer.getInstallerUserId());

        log.debug("Successfully deleted installation with workspace id = {} and user id = {}",
                installer.getTeamId(), installer.getInstallerUserId());
    }

    public void deleteAll(String enterpriseId, String wid) throws UnableToPerformSlackOperationException {
        if (!isValidString.test(wid))
            throw new UnableToPerformSlackOperationException("Could remove all workspace records. Invalid arguments (wid)");

        log.debug("New request to delete workspace with id = {}", wid);

        workspaceRegistryService.deleteWorkspace(wid);

        workspacesCacheClient.deleteWorkspace(wid);
        botUsersCacheClient.deleteBot(wid);
        usersCacheClient.deleteAll();

        log.debug("Successfully deleted workspace with id = {}", wid);
    }

    public boolean isHistoricalDataEnabled() {
        return historyEnabled;
    }

    public void setHistoricalDataEnabled(boolean flag) {
        this.historyEnabled = flag;
    }

    @RateLimiter(name = "queryRateLimiter", fallbackMethod = "findBotRateFallback")
    public Bot findBot(String s, String wid) {
        if (!isValidString.test(wid)) return null;

        log.debug("New request to find workspace bot = {}", wid);

        try {
            User cached = botUsersCacheClient.getBot(wid);
            if (cached != null) {
                log.debug("Found a bot instance in the cache");
                return slackInstallationMapper.toBot(cached.getToken());
            }

            User user = userRegistryService.getUser(wid, wid);
            user.setToken(encryptor.decrypt(user.getToken()));

            Bot bot = slackInstallationMapper.toBot(user.getToken());

            botUsersCacheClient.addBot(wid, user);

            log.debug("Successfully found workspace bot = {}", wid);

            return bot;
        } catch (Exception e) {
            return null;
        }
    }

    public Bot findBotRateFallback(
            String s,
            String wid,
            RequestNotPermitted e
    ) throws UnableToPerformSlackOperationException {
        log.warn("find bot {}: {}", wid, e.getMessage());
        return null;
    }

    public void deleteBot(Bot bot) {}

    public void saveBot(Bot bot) {}

    public List<LayoutBlock> getInstallationGuideBlocks(String enterpriseId, String teamId, String userId) {
        return asBlocks(
                header(h -> h.text(plainText("Seems you did not install ONLYOFFICE App"))),
                section(s -> s.text(
                        markdownText("Please go to *<"+integrationConfiguration.getInstallUrl()+"|ONLYOFFICE Installation>* page to install/reinstall the App")
                ))
        );
    }
}
