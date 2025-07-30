package com.onlyoffice.slack.persistence.repository;

import com.onlyoffice.slack.persistence.entity.ActiveFileSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveFileSessionRepository extends JpaRepository<ActiveFileSession, String> {}
