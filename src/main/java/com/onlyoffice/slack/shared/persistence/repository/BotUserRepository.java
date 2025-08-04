package com.onlyoffice.slack.shared.persistence.repository;

import com.onlyoffice.slack.shared.persistence.entity.BotUser;
import com.onlyoffice.slack.shared.persistence.entity.id.BotUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotUserRepository extends JpaRepository<BotUser, BotUserId> {
  void deleteByTeamId(String teamId);
}
