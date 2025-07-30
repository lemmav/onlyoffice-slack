package com.onlyoffice.slack.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.exception.SettingsConfigurationException;
import com.onlyoffice.slack.persistence.entity.TeamSettings;
import com.onlyoffice.slack.persistence.repository.TeamSettingsRepository;
import com.onlyoffice.slack.service.cryptography.AesEncryptionService;
import com.onlyoffice.slack.service.data.SettingsService;
import com.onlyoffice.slack.transfer.request.SubmitSettingsRequest;
import com.slack.api.bolt.context.Context;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("SettingsService Integration Tests")
class SettingsServiceIntegrationTests extends BaseIntegrationTest {
  @Autowired private EntityManager entityManager;
  @Autowired private SettingsService settingsService;
  @Autowired private TeamSettingsRepository teamSettingsRepository;

  @MockitoBean private AesEncryptionService encryptionService;
  @MockitoBean private ServerConfigurationProperties serverConfigurationProperties;

  private Context mockContext;

  @BeforeEach
  void setUp() {
    teamSettingsRepository.deleteAll();

    mockContext = mock(Context.class);

    when(mockContext.getTeamId()).thenReturn("test-team-id");

    var mockDemoProperties = new ServerConfigurationProperties.DemoProperties();
    mockDemoProperties.setDurationDays(7);

    when(serverConfigurationProperties.getDemo()).thenReturn(mockDemoProperties);
    when(encryptionService.encrypt("test-secret")).thenReturn("encrypted-test-secret");
    when(encryptionService.decrypt("encrypted-test-secret")).thenReturn("test-secret");
  }

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperationsTests {
    @Test
    @DisplayName("When creating new demo settings then save successfully")
    void whenCreatingNewDemoSettings_thenSaveSuccessfully() {
      var request = SubmitSettingsRequest.builder().demoEnabled(true).build();

      settingsService.saveSettings(mockContext, request);
      var savedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(savedSettings).isPresent();
      assertThat(savedSettings.get().getDemoEnabled()).isTrue();
      assertThat(savedSettings.get().getDemoStartedDate()).isNotNull();
      assertThat(savedSettings.get().getTeamId()).isEqualTo("test-team-id");
    }

    @Test
    @DisplayName("When creating new settings with complete credentials then save successfully")
    void whenCreatingNewSettingsWithCompleteCredentials_thenSaveSuccessfully() {
      var request =
          SubmitSettingsRequest.builder()
              .address("https://example.com")
              .header("Authorization")
              .secret("test-secret")
              .demoEnabled(false)
              .build();

      settingsService.saveSettings(mockContext, request);
      var savedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(savedSettings).isPresent();
      assertThat(savedSettings.get().getAddress()).isEqualTo("https://example.com");
      assertThat(savedSettings.get().getHeader()).isEqualTo("Authorization");
      assertThat(savedSettings.get().getSecret()).isEqualTo("encrypted-test-secret");
      assertThat(savedSettings.get().getDemoEnabled()).isFalse();
      assertThat(savedSettings.get().getDemoStartedDate()).isNull();
    }

    @Test
    @DisplayName("When creating settings with invalid configuration then throw exception")
    void whenCreatingSettingsWithInvalidConfiguration_thenThrowException() {
      var request =
          SubmitSettingsRequest.builder()
              .address("https://example.com")
              .header("Authorization")
              .demoEnabled(false)
              .build();

      assertThatThrownBy(() -> settingsService.saveSettings(mockContext, request))
          .isInstanceOf(SettingsConfigurationException.class)
          .hasMessageContaining("Could not validate settings");
    }

    @Test
    @DisplayName("When creating settings with null secret then do not encrypt")
    void whenCreatingSettingsWithNullSecret_thenDoNotEncrypt() {
      var request = SubmitSettingsRequest.builder().demoEnabled(true).secret(null).build();

      settingsService.saveSettings(mockContext, request);
      var savedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(savedSettings).isPresent();
      assertThat(savedSettings.get().getSecret()).isNull();
    }

    @Test
    @DisplayName("When creating settings with blank secret then do not encrypt")
    void whenCreatingSettingsWithBlankSecret_thenDoNotEncrypt() {
      var request = SubmitSettingsRequest.builder().demoEnabled(true).secret("   ").build();

      settingsService.saveSettings(mockContext, request);
      var savedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(savedSettings).isPresent();
      assertThat(savedSettings.get().getSecret()).isEqualTo("   ");
    }

    @Test
    @DisplayName("When creation fails due to encryption error then rollback transaction")
    void whenCreationFailsDueToEncryptionError_thenRollbackTransaction() {
      var request =
          SubmitSettingsRequest.builder()
              .address("https://example.com")
              .header("Authorization")
              .secret("test-secret")
              .demoEnabled(false)
              .build();

      when(encryptionService.encrypt("test-secret"))
          .thenThrow(new RuntimeException("Encryption failed"));
      assertThatThrownBy(() -> settingsService.saveSettings(mockContext, request))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Encryption failed");
      assertThat(teamSettingsRepository.findById("test-team-id")).isEmpty();
    }

    @Test
    @DisplayName("When creating multiple team settings concurrently then handle correctly")
    void whenCreatingMultipleTeamSettingsConcurrently_thenHandleCorrectly() {
      var teamOneRequest =
          SubmitSettingsRequest.builder()
              .address("https://team1.com")
              .header("Team1-Header")
              .secret("team1-secret")
              .demoEnabled(false)
              .build();

      var teamTwoRequest =
          SubmitSettingsRequest.builder()
              .address("https://team2.com")
              .header("Team2-Header")
              .secret("team2-secret")
              .demoEnabled(false)
              .build();

      var teamOneContext = mock(Context.class);
      var teamTwoContext = mock(Context.class);

      when(teamOneContext.getTeamId()).thenReturn("team-1");
      when(teamTwoContext.getTeamId()).thenReturn("team-2");
      when(encryptionService.encrypt("team1-secret")).thenReturn("encrypted-team1-secret");
      when(encryptionService.encrypt("team2-secret")).thenReturn("encrypted-team2-secret");

      settingsService.saveSettings(teamOneContext, teamOneRequest);
      settingsService.saveSettings(teamTwoContext, teamTwoRequest);

      var teamOneSettings = teamSettingsRepository.findById("team-1");
      var teamTwoSettings = teamSettingsRepository.findById("team-2");

      assertThat(teamOneSettings).isPresent();
      assertThat(teamOneSettings.get().getAddress()).isEqualTo("https://team1.com");
      assertThat(teamOneSettings.get().getSecret()).isEqualTo("encrypted-team1-secret");
      assertThat(teamTwoSettings).isPresent();
      assertThat(teamTwoSettings.get().getAddress()).isEqualTo("https://team2.com");
      assertThat(teamTwoSettings.get().getSecret()).isEqualTo("encrypted-team2-secret");
      assertThat(teamSettingsRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("When creating demo settings then demo started date is set")
    void whenCreatingDemoSettings_thenDemoStartedDateIsSet() {
      var request = SubmitSettingsRequest.builder().demoEnabled(true).build();

      settingsService.saveSettings(mockContext, request);

      var savedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(savedSettings).isPresent();
      assertThat(savedSettings.get().getDemoStartedDate())
          .isNotNull()
          .isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)) // Reasonable past date
          .isBefore(LocalDateTime.of(2030, 1, 1, 0, 0)); // Reasonable future date
    }
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperationsTests {
    @Test
    @DisplayName("When reading non-existent settings then return empty response")
    void whenReadingNonExistentSettings_thenReturnEmptyResponse() {
      var response = settingsService.findSettings("non-existent-team");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isNull();
      assertThat(response.getHeader()).isNull();
      assertThat(response.getSecret()).isNull();
      assertThat(response.isDemoEnabled()).isFalse();
    }

    @Test
    @DisplayName("When reading valid demo settings then return decrypted settings")
    void whenReadingValidDemoSettings_thenReturnDecryptedSettings() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://demo.com")
              .header("Demo-Header")
              .secret("encrypted-demo-secret")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(3))
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      when(encryptionService.decrypt("encrypted-demo-secret")).thenReturn("demo-secret");

      var response = settingsService.findSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://demo.com");
      assertThat(response.getHeader()).isEqualTo("Demo-Header");
      assertThat(response.getSecret()).isEqualTo("demo-secret");
      assertThat(response.isDemoEnabled()).isTrue();
      assertThat(response.getDemoStartedDate()).isNotNull();
    }

    @Test
    @DisplayName("When reading expired demo settings then throw exception")
    void whenReadingExpiredDemoSettings_thenThrowException() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(10))
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      assertThatThrownBy(() -> settingsService.findSettings("test-team-id"))
          .isInstanceOf(SettingsConfigurationException.class)
          .hasMessageContaining("ONLYOFFICE demo has expired");
    }

    @Test
    @DisplayName("When reading incomplete non-demo settings then throw exception")
    void whenReadingIncompleteNonDemoSettings_thenThrowException() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      assertThatThrownBy(() -> settingsService.findSettings("test-team-id"))
          .isInstanceOf(SettingsConfigurationException.class)
          .hasMessageContaining("ONLYOFFICE settings are incomplete");
    }

    @Test
    @DisplayName("When reading complete non-demo settings then return decrypted settings")
    void whenReadingCompleteNonDemoSettings_thenReturnDecryptedSettings() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .header("Authorization")
              .secret("encrypted-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      when(encryptionService.decrypt("encrypted-secret")).thenReturn("decrypted-secret");

      var response = settingsService.findSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://example.com");
      assertThat(response.getHeader()).isEqualTo("Authorization");
      assertThat(response.getSecret()).isEqualTo("decrypted-secret");
      assertThat(response.isDemoEnabled()).isFalse();
    }

    @Test
    @DisplayName("When reading settings with null secret then do not decrypt")
    void whenReadingSettingsWithNullSecret_thenDoNotDecrypt() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now())
              .secret(null)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      var response = settingsService.findSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getSecret()).isNull();
    }

    @Test
    @DisplayName("When reading settings with decryption failure then propagate exception")
    void whenReadingSettingsWithDecryptionFailure_thenPropagateException() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .header("Authorization")
              .secret("corrupted-encrypted-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      when(encryptionService.decrypt("corrupted-encrypted-secret"))
          .thenThrow(new RuntimeException("Decryption failed"));
      assertThatThrownBy(() -> settingsService.findSettings("test-team-id"))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Decryption failed");
    }

    @Test
    @DisplayName("When reading demo settings at expiration boundary then throw exception")
    void whenReadingDemoSettingsAtExpirationBoundary_thenThrowException() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(7).minusMinutes(1))
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      assertThatThrownBy(() -> settingsService.findSettings("test-team-id"))
          .isInstanceOf(SettingsConfigurationException.class)
          .hasMessageContaining("ONLYOFFICE demo has expired");
    }

    @Test
    @DisplayName("When reading demo settings within limit then return settings")
    void whenReadingDemoSettingsWithinLimit_thenReturnSettings() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://demo.com")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(6).minusHours(23))
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      var response = settingsService.findSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://demo.com");
      assertThat(response.isDemoEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperationsAlwaysTests {
    @Test
    @DisplayName("When reading non-existent settings always then return empty response")
    void whenReadingNonExistentSettingsAlways_thenReturnEmptyResponse() {
      var response = settingsService.alwaysFindSettings("non-existent-team");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isNull();
      assertThat(response.getHeader()).isNull();
      assertThat(response.getSecret()).isNull();
      assertThat(response.isDemoEnabled()).isFalse();
    }

    @Test
    @DisplayName("When reading expired demo settings always then return without validation")
    void whenReadingExpiredDemoSettingsAlways_thenReturnWithoutValidation() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://demo.com")
              .header("Demo-Header")
              .secret("encrypted-secret")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(10))
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      when(encryptionService.decrypt("encrypted-secret")).thenReturn("decrypted-secret");

      var response = settingsService.alwaysFindSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://demo.com");
      assertThat(response.getHeader()).isEqualTo("Demo-Header");
      assertThat(response.getSecret()).isEqualTo("decrypted-secret");
      assertThat(response.isDemoEnabled()).isTrue();
    }

    @Test
    @DisplayName("When reading incomplete settings always then return without validation")
    void whenReadingIncompleteSettingsAlways_thenReturnWithoutValidation() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      var response = settingsService.alwaysFindSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://example.com");
      assertThat(response.getSecret()).isNull();
      assertThat(response.isDemoEnabled()).isFalse();
    }

    @Test
    @DisplayName("When reading complete settings always then return decrypted settings")
    void whenReadingCompleteSettingsAlways_thenReturnDecryptedSettings() {
      var settings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .header("Authorization")
              .secret("encrypted-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(settings);
      teamSettingsRepository.flush();

      when(encryptionService.decrypt("encrypted-secret")).thenReturn("decrypted-secret");

      var response = settingsService.alwaysFindSettings("test-team-id");

      assertThat(response).isNotNull();
      assertThat(response.getAddress()).isEqualTo("https://example.com");
      assertThat(response.getHeader()).isEqualTo("Authorization");
      assertThat(response.getSecret()).isEqualTo("decrypted-secret");
      assertThat(response.isDemoEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("UPDATE Operations")
  class UpdateOperationsTests {
    @Test
    @DisplayName("When updating existing settings then upsert correctly")
    void whenUpdatingExistingSettings_thenUpsertCorrectly() {
      var existingSettings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://old.com")
              .header("Old-Header")
              .secret("old-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(existingSettings);
      teamSettingsRepository.flush();

      var request =
          SubmitSettingsRequest.builder()
              .address("https://new.com")
              .header("New-Header")
              .secret("new-secret")
              .demoEnabled(false)
              .build();

      when(encryptionService.encrypt("new-secret")).thenReturn("encrypted-new-secret");

      settingsService.saveSettings(mockContext, request);

      entityManager.flush();
      entityManager.clear();
      var updatedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(updatedSettings).isPresent();
      assertThat(updatedSettings.get().getAddress()).isEqualTo("https://new.com");
      assertThat(updatedSettings.get().getHeader()).isEqualTo("New-Header");
      assertThat(updatedSettings.get().getSecret()).isEqualTo("encrypted-new-secret");
      assertThat(teamSettingsRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("When updating from non-demo to demo then preserve existing settings")
    void whenUpdatingFromNonDemoToDemo_thenPreserveExistingSettings() {
      var existingSettings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://production.com")
              .header("Production-Header")
              .secret("production-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(existingSettings);
      teamSettingsRepository.flush();

      var request = SubmitSettingsRequest.builder().demoEnabled(true).build();

      settingsService.saveSettings(mockContext, request);

      entityManager.flush();
      entityManager.clear();
      var updatedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(updatedSettings).isPresent();
      assertThat(updatedSettings.get().getDemoEnabled()).isTrue();
      assertThat(updatedSettings.get().getDemoStartedDate()).isNotNull();
      assertThat(teamSettingsRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("When updating from demo to non-demo then update correctly")
    void whenUpdatingFromDemoToNonDemo_thenUpdateCorrectly() {
      var existingSettings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .demoEnabled(true)
              .demoStartedDate(LocalDateTime.now().minusDays(2))
              .build();
      teamSettingsRepository.save(existingSettings);
      teamSettingsRepository.flush();

      var request =
          SubmitSettingsRequest.builder()
              .address("https://production.com")
              .header("Production-Header")
              .secret("production-secret")
              .demoEnabled(false)
              .build();

      when(encryptionService.encrypt("production-secret"))
          .thenReturn("encrypted-production-secret");

      settingsService.saveSettings(mockContext, request);

      entityManager.flush();
      entityManager.clear();
      var updatedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(updatedSettings).isPresent();
      assertThat(updatedSettings.get().getAddress()).isEqualTo("https://production.com");
      assertThat(updatedSettings.get().getHeader()).isEqualTo("Production-Header");
      assertThat(updatedSettings.get().getSecret()).isEqualTo("encrypted-production-secret");
      assertThat(updatedSettings.get().getDemoEnabled()).isFalse();
    }

    @Test
    @DisplayName("When updating with invalid configuration then reject update")
    void whenUpdatingWithInvalidConfiguration_thenRejectUpdate() {
      var existingSettings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://existing.com")
              .header("Existing-Header")
              .secret("existing-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(existingSettings);
      teamSettingsRepository.flush();

      var request =
          SubmitSettingsRequest.builder()
              .address("https://invalid.com")
              .header("Invalid-Header")
              .demoEnabled(false)
              .build();

      assertThatThrownBy(() -> settingsService.saveSettings(mockContext, request))
          .isInstanceOf(SettingsConfigurationException.class)
          .hasMessageContaining("Could not validate settings");

      var unchangedSettings = teamSettingsRepository.findById("test-team-id");

      assertThat(unchangedSettings).isPresent();
      assertThat(unchangedSettings.get().getAddress()).isEqualTo("https://existing.com");
    }
  }

  @Nested
  @DisplayName("DELETE Operations")
  class DeleteOperationsTests {
    @Test
    @DisplayName("When deleting existing settings then remove from database")
    void whenDeletingExistingSettings_thenRemoveFromDatabase() {
      var existingSettings =
          TeamSettings.builder()
              .teamId("test-team-id")
              .address("https://example.com")
              .header("Authorization")
              .secret("encrypted-secret")
              .demoEnabled(false)
              .build();
      teamSettingsRepository.save(existingSettings);
      teamSettingsRepository.flush();

      assertThat(teamSettingsRepository.findById("test-team-id")).isPresent();

      teamSettingsRepository.deleteById("test-team-id");

      assertThat(teamSettingsRepository.findById("test-team-id")).isEmpty();
      assertThat(teamSettingsRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When deleting non-existent settings then handle gracefully")
    void whenDeletingNonExistentSettings_thenHandleGracefully() {
      assertThat(teamSettingsRepository.findById("non-existent-team")).isEmpty();

      teamSettingsRepository.deleteById("non-existent-team");

      assertThat(teamSettingsRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When deleting all settings then clear database")
    void whenDeletingAllSettings_thenClearDatabase() {
      var settingsOne = TeamSettings.builder().teamId("team-1").demoEnabled(true).build();
      var settingsTwo = TeamSettings.builder().teamId("team-2").demoEnabled(true).build();
      var settingsThree = TeamSettings.builder().teamId("team-3").demoEnabled(false).build();

      teamSettingsRepository.saveAll(java.util.List.of(settingsOne, settingsTwo, settingsThree));
      teamSettingsRepository.flush();

      assertThat(teamSettingsRepository.count()).isEqualTo(3);

      teamSettingsRepository.deleteAll();

      assertThat(teamSettingsRepository.count()).isEqualTo(0);
      assertThat(teamSettingsRepository.findAll()).isEmpty();
    }
  }
}
