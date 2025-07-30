package com.onlyoffice.slack.persistence.repository;

import com.onlyoffice.slack.persistence.entity.InstallerUser;
import com.onlyoffice.slack.persistence.entity.id.InstallerUserId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstallerUserRepository extends JpaRepository<InstallerUser, InstallerUserId> {
  Optional<InstallerUser> findFirstByTeamId(String teamId);

  Optional<InstallerUser> findByTeamIdAndInstallerUserId(String teamId, String installerUserId);

  @Query("SELECT u FROM InstallerUser u WHERE u.bot.botUserId = :botUserId")
  Optional<InstallerUser> findByBotUserId(String botUserId);

  @Query("SELECT u FROM InstallerUser u WHERE u.bot.botId = :botId")
  List<InstallerUser> findByBotId(String botId);

  Optional<InstallerUser> findByInstallerUserId(String installerUserId);

  int deleteAllByTeamId(String teamId);
}
