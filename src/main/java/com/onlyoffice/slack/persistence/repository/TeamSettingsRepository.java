package com.onlyoffice.slack.persistence.repository;

import com.onlyoffice.slack.persistence.entity.TeamSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamSettingsRepository extends JpaRepository<TeamSettings, String> {

  @Modifying
  @Query(
      value =
          """
    INSERT INTO team_settings (team_id, address, header, secret, demo_enabled, demo_started_date)
    VALUES (:teamId, :address, :header, :secret, :demoEnabled,
            CASE WHEN :demoEnabled = true THEN CURRENT_TIMESTAMP ELSE NULL END)
    ON CONFLICT (team_id) DO UPDATE SET
      address = :address,
      header = :header,
      secret = :secret,
      demo_enabled = :demoEnabled,
      demo_started_date = CASE
        WHEN :demoEnabled = true AND team_settings.demo_started_date IS NULL
        THEN CURRENT_TIMESTAMP
        ELSE team_settings.demo_started_date
      END
    """,
      nativeQuery = true)
  void upsertSettings(
      @Param("teamId") String teamId,
      @Param("address") String address,
      @Param("header") String header,
      @Param("secret") String secret,
      @Param("demoEnabled") Boolean demoEnabled);
}
