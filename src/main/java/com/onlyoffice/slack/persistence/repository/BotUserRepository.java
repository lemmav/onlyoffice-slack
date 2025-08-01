package com.onlyoffice.slack.persistence.repository;

import com.onlyoffice.slack.persistence.entity.BotUser;
import com.onlyoffice.slack.persistence.entity.id.BotUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotUserRepository extends JpaRepository<BotUser, BotUserId> {
  void deleteByTeamId(String teamId);
}
