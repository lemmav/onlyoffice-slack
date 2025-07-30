package com.onlyoffice.slack.persistence.repository;

import com.onlyoffice.slack.persistence.entity.BotUser;
import com.onlyoffice.slack.persistence.entity.id.BotUserId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotUserRepository extends JpaRepository<BotUser, BotUserId> {
  List<BotUser> findByTeamId(String teamId);

  Optional<BotUser> findByTeamIdAndBotId(String teamId, String botId);

  Optional<BotUser> findByBotUserId(String botUserId);

  void deleteByTeamId(String teamId);
}
