package com.onlyoffice.slack.service.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.persistence.repository.BotUserRepository;
import com.onlyoffice.slack.persistence.repository.InstallerUserRepository;
import com.slack.api.bolt.model.Installer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class InstallationServiceTests {
  @Mock private IMap userCache;
  @Mock private SlackConfigurationProperties slackProperties;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private InstallerUserRepository userRepository;
  @Mock private IMap botCache;
  @Mock private BotUserRepository botRepository;
  @InjectMocks private InstallationService service;

  @Test
  void whenIsHistoricalDataEnabled_thenReturnCurrentValue() {
    assertTrue(service.isHistoricalDataEnabled());

    service.setHistoricalDataEnabled(false);

    assertFalse(service.isHistoricalDataEnabled());
  }

  @Test
  void whenSetHistoricalDataEnabled_thenValueChanges() {
    service.setHistoricalDataEnabled(false);

    assertFalse(service.isHistoricalDataEnabled());

    service.setHistoricalDataEnabled(true);

    assertTrue(service.isHistoricalDataEnabled());
  }

  @Test
  void whenSaveInstallerAndBot_thenNoException() {
    var installer = mock(Installer.class);
    assertDoesNotThrow(() -> service.saveInstallerAndBot(installer));
  }
}
